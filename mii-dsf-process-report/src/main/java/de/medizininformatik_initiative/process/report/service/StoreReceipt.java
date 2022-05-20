package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_MISSING;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;

public class StoreReceipt extends AbstractServiceDelegate implements InitializingBean
{
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
		{
			handleReceivedResponse(leadingTask, currentTask);
		}
		else
		{
			handleMissingResponse(leadingTask);
		}

		updateLeadingTaskInExecutionVariables(leadingTask);
	}

	private void handleReceivedResponse(Task leadingTask, Task currentTask)
	{
		statusGenerator.transformInputToOutput(currentTask, leadingTask);

		// The currentTask finishes here but is not automatically set to completed
		// because it is an additional currentTask during the execution of the main process
		currentTask.setStatus(Task.TaskStatus.COMPLETED);
		getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().update(currentTask);
	}

	private void handleMissingResponse(Task leadingTask)
	{
		leadingTask.addOutput(
				statusGenerator.createReportStatusOutput(CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_MISSING));
	}
}
