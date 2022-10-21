package de.medizininformatik_initiative.process.kds.report.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.highmed.fhir.client.BasicFhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class DownloadKdsReport extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadKdsReport.class);

	private final KdsReportStatusGenerator kdsReportStatusGenerator;

	public DownloadKdsReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, KdsReportStatusGenerator kdsReportStatusGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.kdsReportStatusGenerator = kdsReportStatusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(kdsReportStatusGenerator, "kdsReportStatusGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		IdType reportReference = getReportReference(task);

		logger.info("Downloading KDS report with id '{}' referenced in Task with id '{}'", reportReference.getValue(),
				task.getId());

		try
		{
			Bundle reportBundle = downloadReportBundle(reportReference);
			execution.setVariable(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE,
					FhirResourceValues.create(reportBundle));
		}
		catch (Exception exception)
		{
			task.setStatus(Task.TaskStatus.FAILED);
			task.addOutput(kdsReportStatusGenerator.createKdsReportStatusOutput(
					ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR, exception.getMessage()));
			updateLeadingTaskInExecutionVariables(execution, task);

			logger.warn("Downloading report with id '{}' referenced in Task with id '{}' failed - {}",
					reportReference.getValue(), task.getId(), exception.getMessage());
			throw new BpmnError(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_RECEIVE_ERROR,
					exception.getMessage());
		}
	}

	private IdType getReportReference(Task task)
	{
		List<String> reportReferences = getTaskHelper()
				.getInputParameterReferenceValues(task, ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT,
						ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE)
				.filter(Reference::hasReference).map(Reference::getReference).collect(toList());

		if (reportReferences.size() < 1)
			throw new IllegalArgumentException(
					"No KDS report reference present in Task with id '" + task.getId() + "'");

		if (reportReferences.size() > 1)
			logger.warn("Found {} KDS report references in task with id '{}', using only the first",
					reportReferences.size(), task.getId());

		return new IdType(reportReferences.get(0));
	}

	private Bundle downloadReportBundle(IdType reportReference)
	{
		BasicFhirWebserviceClient client = getFhirWebserviceClientProvider()
				.getWebserviceClient(reportReference.getBaseUrl()).withRetry(
						ConstantsKdsReport.DSF_CLIENT_RETRY_6_TIMES, ConstantsKdsReport.DSF_CLIENT_RETRY_INTERVAL_5MIN);

		if (reportReference.hasVersionIdPart())
			return client.read(Bundle.class, reportReference.getIdPart(), reportReference.getVersionIdPart());
		else
			return client.read(Bundle.class, reportReference.getIdPart());

	}
}
