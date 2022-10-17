package de.medizininformatik_initiative.process.kds.report.service;

import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_ALLOWED;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_REACHABLE;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class DownloadSearchBundle extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadSearchBundle.class);

	private final KdsReportStatusGenerator statusGenerator;
	private final DataLogger dataLogger;

	public DownloadSearchBundle(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, KdsReportStatusGenerator statusGenerator, DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.statusGenerator = statusGenerator;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(statusGenerator, "statusGenerator");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Target target = (Target) execution.getVariable(BPMN_EXECUTION_VARIABLE_TARGET);

		String searchBundleIdentifier = CODESYSTEM_MII_KDS_REPORT + "|" + CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE;

		logger.info("Downloading search bundle='{}' from hrp='{}' for task with id='{}'", searchBundleIdentifier,
				target.getOrganizationIdentifierValue(), getLeadingTaskFromExecutionVariables(execution).getId());

		Bundle bundle = searchSearchBundle(execution, target, searchBundleIdentifier);

		dataLogger.logResource("Search Response", bundle);

		Bundle searchBundle = extractSearchBundle(bundle, searchBundleIdentifier);
		dataLogger.logResource("Search Bundle", searchBundle);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE,
				FhirResourceValues.create(searchBundle));
	}

	private Bundle searchSearchBundle(DelegateExecution execution, Target target, String searchBundleIdentifier)
	{
		FhirWebserviceClient client = getFhirWebserviceClientProvider().getWebserviceClient(target.getEndpointUrl());

		try
		{
			return client.searchWithStrictHandling(Bundle.class,
					Map.of("identifier", Collections.singletonList(searchBundleIdentifier)));
		}
		catch (WebApplicationException exception)
		{
			String statusCode = CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_REACHABLE;

			if (exception.getResponse() != null
					&& exception.getResponse().getStatus() == Response.Status.FORBIDDEN.getStatusCode())
			{
				statusCode = CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_ALLOWED;
			}

			Task task = getLeadingTaskFromExecutionVariables(execution);
			task.addOutput(statusGenerator.createKdsReportStatusOutput(statusCode, createErrorMessage(exception)));
			updateLeadingTaskInExecutionVariables(execution, task);

			throw new RuntimeException("Error while reading search Bundle with identifier '" + searchBundleIdentifier
					+ "' from organization '" + task.getRequester().getReference() + "': " + exception.getMessage());
		}
	}

	private String createErrorMessage(Exception exception)
	{
		return exception.getClass().getSimpleName()
				+ ((exception.getMessage() != null && !exception.getMessage().isBlank())
						? (": " + exception.getMessage())
						: "");
	}

	private Bundle extractSearchBundle(Bundle bundle, String searchBundleIdentifier)
	{
		if (bundle.getTotal() != 1 && !(bundle.getEntryFirstRep().getResource() instanceof Bundle))
			throw new IllegalStateException("Expected a single search Bundle with identifier '" + searchBundleIdentifier
					+ "' but found " + bundle.getTotal());

		return (Bundle) bundle.getEntryFirstRep().getResource();
	}
}
