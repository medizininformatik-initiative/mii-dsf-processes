package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class HandleErrorMergeReceive extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(HandleErrorMergeReceive.class);

	private final MailService mailService;

	public HandleErrorMergeReceive(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String error = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_RECEIVE_ERROR_MESSAGE);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Task task = getCurrentTaskFromExecutionVariables(execution);
		String sendingOrganization = getSendingOrganization(task);

		logger.warn(error);

		task.setStatus(Task.TaskStatus.FAILED);
		task.addOutput(getTaskHelper().createOutput(ConstantsBase.CODESYSTEM_HIGHMED_BPMN,
				ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR, error));
		updateCurrentTaskInExecutionVariables(execution, task);

		String subject = "Error processing received data-set in process '"
				+ ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING + "' for data-sharing project '"
				+ projectIdentifier + "'";
		String message = "The following error occurred whilst processing a received data-set in process '"
				+ ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING + "' for data-sharing project '"
				+ projectIdentifier + "' Manual communication with the sending organization '" + sendingOrganization
				+ "' is needed to repair the error.\n\n" + "Error:\n" + error;

		mailService.send(subject, message);
	}

	private String getSendingOrganization(Task task)
	{
		return task.getRequester().getIdentifier().getValue();
	}
}
