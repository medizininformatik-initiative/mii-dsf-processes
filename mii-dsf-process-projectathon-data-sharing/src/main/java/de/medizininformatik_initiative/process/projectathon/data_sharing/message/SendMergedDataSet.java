package de.medizininformatik_initiative.process.projectathon.data_sharing.message;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class SendMergedDataSet extends AbstractTaskMessageSend
{
	public SendMergedDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String dataSetLocation = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_LOCATION);

		Task.ParameterComponent dataSetLocationInput = new Task.ParameterComponent();
		dataSetLocationInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);
		dataSetLocationInput.setValue(new UrlType().setValue(dataSetLocation));

		return Stream.of(dataSetLocationInput);
	}

	@Override
	protected void doSend(FhirWebserviceClient client, Task task)
	{
		client.withMinimalReturn().withRetry(ConstantsDataSharing.DSF_CLIENT_RETRY_6_TIMES,
				ConstantsDataSharing.DSF_CLIENT_RETRY_INTERVAL_5MIN).create(task);
	}

	@Override
	protected void handleSendTaskError(DelegateExecution execution, Exception exception, String errorMessage)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String hrpIdentifier = ((Target) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET))
				.getOrganizationIdentifierValue();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		String message = "Could not send merged data-set to HRP '" + hrpIdentifier + "' for data-sharing project '"
				+ projectIdentifier + "' referenced in Task with id '" + task.getId() + "' - " + exception.getMessage()
				+ " - Please retry after HRP is online again";

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_RELEASE_ERROR_MESSAGE,
				Variables.stringValue(message));

		throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_RELEASE_ERROR, message);
	}
}
