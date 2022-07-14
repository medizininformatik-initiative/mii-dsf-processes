package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_STATUS;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_MISSING;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_OK;
import static de.medizininformatik_initiative.process.report.ConstantsReport.EXTENSION_REPORT_STATUS_ERROR_URL;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;

import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;

public class StoreReceipt extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(StoreReceipt.class);

	private final ReportStatusGenerator statusGenerator;

	public StoreReceipt(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, ReportStatusGenerator statusGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.statusGenerator = statusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(statusGenerator, "statusGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task leadingTask = getLeadingTaskFromExecutionVariables();
		Task currentTask = getCurrentTaskFromExecutionVariables();

		if (!currentTask.getId().equals(leadingTask.getId()))
			handleReceivedResponse(leadingTask, currentTask);
		else
			handleMissingResponse(leadingTask);

		addBusinessKeyOutput(leadingTask, execution);
		writeStatusLog(leadingTask);
		updateLeadingTaskInExecutionVariables(leadingTask);
	}

	private void handleReceivedResponse(Task leadingTask, Task currentTask)
	{
		statusGenerator.transformInputToOutput(currentTask, leadingTask);

		if (leadingTask.getOutput().stream().filter(Task.TaskOutputComponent::hasExtension)
				.flatMap(o -> o.getExtension().stream())
				.anyMatch(e -> EXTENSION_REPORT_STATUS_ERROR_URL.equals(e.getUrl())))
			leadingTask.setStatus(Task.TaskStatus.FAILED);

		// The currentTask finishes here but is not automatically set to completed
		// because it is an additional currentTask during the execution of the main process
		currentTask.setStatus(Task.TaskStatus.COMPLETED);
		getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().update(currentTask);
	}

	private void handleMissingResponse(Task leadingTask)
	{
		leadingTask.setStatus(Task.TaskStatus.FAILED);
		leadingTask.addOutput(
				statusGenerator.createReportStatusOutput(CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_MISSING));
	}

	private void addBusinessKeyOutput(Task leadingTask, DelegateExecution execution)
	{
		Optional<String> businessKey = getTaskHelper().getFirstInputParameterStringValue(leadingTask,
				CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		if (businessKey.isEmpty())
			leadingTask.addOutput(getTaskHelper().createOutput(CODESYSTEM_HIGHMED_BPMN,
					CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY, execution.getBusinessKey()));
	}

	private void writeStatusLog(Task leadingTask)
	{
		leadingTask.getOutput().stream().filter(o -> o.getValue() instanceof Coding).map(o -> (Coding) o.getValue())
				.filter(c -> CODESYSTEM_MII_REPORT_STATUS.equals(c.getSystem()))
				.forEach(c -> doWriteStatusLog(c, leadingTask.getId()));
	}

	private void doWriteStatusLog(Coding status, String taskId)
	{
		String code = status.getCode();
		String extension = status.hasExtension()
				? " and extension '" + status.getExtensionFirstRep().getUrl() + "|"
						+ status.getExtensionFirstRep().getValueAsPrimitive().getValueAsString() + "'"
				: "";

		if (CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_OK.equals(code))
			logger.info("Task with id '{}' has report-status code '{}'{}", taskId, code, extension);
		else
			logger.warn("Task with id '{}' has report-status code '{}'{}", taskId, code, extension);
	}
}
