package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.util.MimeTypeHelper;

public class ValidateDataSetExecute extends AbstractServiceDelegate implements InitializingBean
{
	private final MimeTypeHelper mimeTypeHelper;

	private static final Logger logger = LoggerFactory.getLogger(ValidateDataSetExecute.class);

	public ValidateDataSetExecute(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, MimeTypeHelper mimeTypeHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.mimeTypeHelper = mimeTypeHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(mimeTypeHelper, "mimeTypeHelper");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);

		logger.info("Validating data-set for COS '{}' and data-sharing project '{}' referenced in Task with id '{}'",
				cosIdentifier, projectIdentifier, task.getId());

		try
		{
			Resource resource = (Resource) execution
					.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_RESOURCE);

			String mimeType = mimeTypeHelper.getMimeType(resource);
			byte[] data = mimeTypeHelper.getData(resource);

			mimeTypeHelper.validate(data, mimeType);
		}
		catch (Exception exception)
		{
			String message = "Could not validate data-set for COS '" + cosIdentifier + "' and  data-sharing project '"
					+ projectIdentifier + "' referenced in Task with id '" + task.getId() + "' - "
					+ exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR, message,
					exception);
		}
	}
}

