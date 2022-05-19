package de.medizininformatik_initiative.process.report.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class StoreReceipt extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreReceipt.class);

	public StoreReceipt(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task leadingTask = getLeadingTaskFromExecutionVariables();
		Task currentTask = getCurrentTaskFromExecutionVariables();

		if (!currentTask.getId().equals(leadingTask.getId()))
		{
			// The currentTask finishes here but is not automatically set to completed
			// because it is an additional currentTask during the execution of the main process
			currentTask.setStatus(Task.TaskStatus.COMPLETED);
			getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().update(currentTask);
		}
	}
}
