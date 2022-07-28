package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class DownloadSearchBundle extends AbstractServiceDelegate implements InitializingBean
{
	private final DataLogger dataLogger;

	public DownloadSearchBundle(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Target target = (Target) execution.getVariable(BPMN_EXECUTION_VARIABLE_TARGET);

		String searchBundleIdentifier = CODESYSTEM_MII_REPORT + "|" + CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE;
		Bundle bundle = searchSearchBundle(target, searchBundleIdentifier);
		dataLogger.logResource("Search Response", bundle);

		Bundle searchBundle = extractSearchBundle(bundle, searchBundleIdentifier);
		dataLogger.logResource("Search Bundle", searchBundle);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE, FhirResourceValues.create(searchBundle));
	}

	private Bundle searchSearchBundle(Target target, String searchBundleIdentifier)
	{
		FhirWebserviceClient client = getFhirWebserviceClientProvider().getWebserviceClient(target.getEndpointUrl());

		try
		{
			return client.searchWithStrictHandling(Bundle.class,
					Map.of("identifier", Collections.singletonList(searchBundleIdentifier)));
		}
		catch (WebApplicationException exception)
		{
			Task task = getLeadingTaskFromExecutionVariables();
			throw new RuntimeException("Error while reading search Bundle with identifier '" + searchBundleIdentifier
					+ "' from organization '" + task.getRequester().getReference() + "': " + exception.getMessage());
		}
	}

	private Bundle extractSearchBundle(Bundle bundle, String searchBundleIdentifier)
	{
		if (bundle.getTotal() != 1 && bundle.getEntryFirstRep().getResource() instanceof Bundle)
			throw new IllegalStateException("Expected a single search Bundle with identifier '" + searchBundleIdentifier
					+ "' but found " + bundle.getTotal());

		return (Bundle) bundle.getEntryFirstRep().getResource();
	}
}
