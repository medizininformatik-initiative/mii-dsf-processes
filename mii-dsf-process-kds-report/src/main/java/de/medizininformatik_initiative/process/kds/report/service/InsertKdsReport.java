package de.medizininformatik_initiative.process.kds.report.service;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.PreferReturnMinimal;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class InsertKdsReport extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(InsertKdsReport.class);

	private final KdsReportStatusGenerator kdsReportStatusGenerator;
	private final MailService mailService;

	public InsertKdsReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, KdsReportStatusGenerator kdsReportStatusGenerator,
			MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.kdsReportStatusGenerator = kdsReportStatusGenerator;
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(kdsReportStatusGenerator, "kdsReportStatusGenerator");
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String sendingOrganization = task.getRequester().getIdentifier().getValue();
		Identifier reportIdentifier = getReportIdentifier(task);

		Bundle report = (Bundle) execution
				.getVariable(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE);
		report.setId("").getMeta().setVersionId("").setTag(null);
		report.setIdentifier(reportIdentifier);

		getReadAccessHelper().addLocal(report);
		getReadAccessHelper().addOrganization(report, task.getRequester().getIdentifier().getValue());

		PreferReturnMinimal client = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
				.withRetry(ConstantsKdsReport.DSF_CLIENT_RETRY_6_TIMES,
						ConstantsKdsReport.DSF_CLIENT_RETRY_INTERVAL_5MIN);
		try
		{
			IdType reportId = client.updateConditionaly(report, Map.of("identifier",
					Collections.singletonList(reportIdentifier.getSystem() + "|" + reportIdentifier.getValue())));

			task.addOutput(kdsReportStatusGenerator
					.createKdsReportStatusOutput(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_OK));
			updateLeadingTaskInExecutionVariables(execution, task);

			String absoluteReportId = new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(),
					ResourceType.Bundle.name(), reportId.getIdPart(), reportId.getVersionIdPart()).getValue();

			logger.info("Stored KDS report with id '{}' from organization '{}' referenced in Task with id '{}'",
					absoluteReportId, sendingOrganization, task.getId());
			sendMail(sendingOrganization, absoluteReportId);
		}
		catch (Exception exception)
		{
			task.setStatus(Task.TaskStatus.FAILED);
			task.addOutput(kdsReportStatusGenerator.createKdsReportStatusOutput(
					ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR, exception.getMessage()));
			updateLeadingTaskInExecutionVariables(execution, task);

			logger.warn("Storing KDS report from organization '{}' referenced in Task with id '{}' failed - {}",
					sendingOrganization, task.getId(), exception.getMessage());
			throw new BpmnError(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_RECEIVE_ERROR,
					exception.getMessage());
		}
	}

	private Identifier getReportIdentifier(Task task)
	{
		return new Identifier().setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
				.setValue(task.getRequester().getIdentifier().getValue());
	}

	private void sendMail(String sendingOrganization, String reportLocation)
	{
		String subject = "New KDS report stored in process '" + ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_RECEIVE
				+ "'";
		String message = "A new KDS report has been stored in process '"
				+ ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_RECEIVE + "' from organization '"
				+ sendingOrganization + "' and can be accessed using the following link:\n" + "- " + reportLocation;

		mailService.send(subject, message);
	}
}
