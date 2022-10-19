package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class HandleErrorExecute extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(HandleErrorExecute.class);

	private final MailService mailService;

	public HandleErrorExecute(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR_MESSAGE);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		logger.warn(error + " - creating new user-task 'release-data-set'");

		String subject = "Error whilst executing process '"
				+ ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING + "' for data-sharing project '"
				+ projectIdentifier + "'";
		String message = "The following error occurred whilst executing process '"
				+ ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING + "' for data-sharing project '"
				+ projectIdentifier + "'. Please repair the error and answer the new user-task 'release-data-set'.\n\n"
				+ "Error:\n" + error;

		mailService.send(subject, message);
	}
}
