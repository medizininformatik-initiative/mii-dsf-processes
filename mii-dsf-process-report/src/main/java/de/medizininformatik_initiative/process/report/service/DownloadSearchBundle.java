package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE;

import java.util.Objects;

import javax.ws.rs.WebApplicationException;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
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
		String searchBundleReference = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE);
		IdType searchBundleId = new IdType(searchBundleReference);
		Bundle bundle = readSearchBundle(searchBundleId);

		dataLogger.logBundle("Search Bundle", bundle);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE, FhirResourceValues.create(bundle));
	}

	private Bundle readSearchBundle(IdType searchBundleId)
	{
		Task task = getLeadingTaskFromExecutionVariables();

		FhirWebserviceClient client = getFhirWebserviceClientProvider()
				.getWebserviceClient(searchBundleId.getBaseUrl());

		try
		{
			if (searchBundleId.hasVersionIdPart())
				return client.read(Bundle.class, searchBundleId.getIdPart(), searchBundleId.getVersionIdPart());
			else
				return client.read(Bundle.class, searchBundleId.getIdPart());
		}
		catch (WebApplicationException exception)
		{
			throw new RuntimeException("Error while reading search Bundle with id '" + searchBundleId.getValue()
					+ "' from organization '" + task.getRequester().getReference() + "': " + exception.getMessage());
		}
	}
}
