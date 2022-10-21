package de.medizininformatik_initiative.process.projectathon.data_sharing.message;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class SendExecuteDataSharing extends AbstractTaskMessageSend
{
	public SendExecuteDataSharing(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		Task.ParameterComponent cosIdentifierInput = getCosIdentifierInput(cosIdentifier);

		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Task.ParameterComponent projectIdentifierInput = getProjectIdentifierInput(projectIdentifier);

		String contractLocation = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_CONTRACT_LOCATION);
		Task.ParameterComponent contractLocationInput = getContractLocationInput(contractLocation);

		return Stream.of(cosIdentifierInput, projectIdentifierInput, contractLocationInput);
	}

	@Override
	protected void doSend(FhirWebserviceClient client, Task task)
	{
		client.withMinimalReturn().withRetry(ConstantsDataSharing.DSF_CLIENT_RETRY_6_TIMES,
				ConstantsDataSharing.DSF_CLIENT_RETRY_INTERVAL_10SEC).create(task);
	}

	private Task.ParameterComponent getCosIdentifierInput(String cosIdentifier)
	{
		return getTaskHelper().createInput(ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
				ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER,
				new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(cosIdentifier))
						.setType(ResourceType.Organization.name()));
	}

	private Task.ParameterComponent getProjectIdentifierInput(String projectIdentifier)
	{
		Task.ParameterComponent projectIdentifierInput = new Task.ParameterComponent();
		projectIdentifierInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER);
		projectIdentifierInput.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER)
				.setValue(projectIdentifier));

		return projectIdentifierInput;
	}

	private Task.ParameterComponent getContractLocationInput(String contractLocation)
	{
		Task.ParameterComponent contractLocationInput = new Task.ParameterComponent();
		contractLocationInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION);
		contractLocationInput.setValue(new UrlType(contractLocation));

		return contractLocationInput;
	}
}
