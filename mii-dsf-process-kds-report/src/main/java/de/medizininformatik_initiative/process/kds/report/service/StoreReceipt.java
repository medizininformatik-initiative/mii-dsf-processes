package de.medizininformatik_initiative.process.kds.report.service;

import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE_RESPONSE_REFERENCE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;

import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class StoreReceipt extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(StoreReceipt.class);

	private final KdsReportStatusGenerator statusGenerator;
	private final MailService mailService;

	public StoreReceipt(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, KdsReportStatusGenerator statusGenerator, MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.statusGenerator = statusGenerator;
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(statusGenerator, "statusGenerator");
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String reportLocation = (String) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE_RESPONSE_REFERENCE);

		Task leadingTask = getLeadingTaskFromExecutionVariables(execution);
		Task currentTask = getCurrentTaskFromExecutionVariables(execution);

		if (!currentTask.getId().equals(leadingTask.getId()))
			handleReceivedResponse(leadingTask, currentTask);
		else
			handleMissingResponse(leadingTask);

		addBusinessKeyOutput(execution, leadingTask);
		writeStatusLogAndSendMail(leadingTask, reportLocation);
		updateLeadingTaskInExecutionVariables(execution, leadingTask);
	}

	private void handleReceivedResponse(Task leadingTask, Task currentTask)
	{
		statusGenerator.transformInputToOutput(currentTask, leadingTask);

		if (leadingTask.getOutput().stream().filter(Task.TaskOutputComponent::hasExtension)
				.flatMap(o -> o.getExtension().stream())
				.anyMatch(e -> ConstantsKdsReport.EXTENSION_KDS_REPORT_STATUS_ERROR_URL.equals(e.getUrl())))
			leadingTask.setStatus(Task.TaskStatus.FAILED);

		// The currentTask finishes here but is not automatically set to completed
		// because it is an additional currentTask during the execution of the main process
		currentTask.setStatus(Task.TaskStatus.COMPLETED);
		getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
				.withRetry(ConstantsKdsReport.DSF_CLIENT_RETRY_6_TIMES,
						ConstantsKdsReport.DSF_CLIENT_RETRY_INTERVAL_5MIN)
				.update(currentTask);
	}

	private void handleMissingResponse(Task leadingTask)
	{
		leadingTask.setStatus(Task.TaskStatus.FAILED);
		leadingTask.addOutput(statusGenerator.createKdsReportStatusOutput(
				ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_MISSING));
	}

	private void addBusinessKeyOutput(DelegateExecution execution, Task leadingTask)
	{
		Optional<String> businessKey = getTaskHelper().getFirstInputParameterStringValue(leadingTask,
				CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		if (businessKey.isEmpty())
			leadingTask.addOutput(getTaskHelper().createOutput(CODESYSTEM_HIGHMED_BPMN,
					CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY, execution.getBusinessKey()));
	}

	private void writeStatusLogAndSendMail(Task leadingTask, String reportLocation)
	{
		leadingTask.getOutput().stream().filter(o -> o.getValue() instanceof Coding).map(o -> (Coding) o.getValue())
				.filter(c -> ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS.equals(c.getSystem()))
				.forEach(c -> doWriteStatusLogAndSendMail(c, leadingTask.getId(), reportLocation));
	}

	private void doWriteStatusLogAndSendMail(Coding status, String leadingTaskId, String reportLocation)
	{
		String code = status.getCode();
		String extension = status.hasExtension()
				? "and extension '" + status.getExtensionFirstRep().getUrl() + "|"
						+ status.getExtensionFirstRep().getValueAsPrimitive().getValueAsString() + "'"
				: "";

		if (ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_OK.equals(code))
		{
			logger.info("Task with id '{}' has KDS report-status code '{}'{}", leadingTaskId, code, extension);
			sendSuccessfulMail(reportLocation, code, extension);
		}
		else
		{
			logger.warn("Task with id '{}' has KDS report-status code '{}'{}", leadingTaskId, code, extension);
			sendErrorMail(leadingTaskId, reportLocation, code, extension);
		}
	}

	private void sendSuccessfulMail(String reportLocation, String code, String extension)
	{
		String subject = "New successful KDS report in process '" + ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND
				+ "'";
		String message = "A new KDS report has been successfully created and retrieved by the HRP with status code '"
				+ code + "'" + extension + " in process '" + ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND
				+ "' and can be accessed using the following link:\n" + "- " + reportLocation;

		mailService.send(subject, message);
	}

	private void sendErrorMail(String leadingTaskId, String reportLocation, String code, String extension)
	{
		String subject = "Error in KDS report process '" + ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND + "'";
		String message = "A new KDS report could not be created and retrieved by the HRP, status code is '" + code + "'"
				+ extension + " in process '" + ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND
				+ "' belonging to Task with id '" + leadingTaskId
				+ "' and can possibly be accessed using the following link:\n" + "- " + reportLocation;

		mailService.send(subject, message);
	}
}
