package de.medizininformatik_initiative.process.projectathon.data_transfer.message;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;

public class StartReceiveProcess extends AbstractTaskMessageSend
{
	public StartReceiveProcess(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String binaryId = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE);

		ParameterComponent binaryComponent = new ParameterComponent();
		binaryComponent.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_DATA_SET_REFERENCE);
		binaryComponent.setValue(new Reference().setType(ResourceType.Binary.name()).setReference(binaryId));

		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		Task.ParameterComponent projectIdentifierComponent = new Task.ParameterComponent();
		projectIdentifierComponent.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER);
		projectIdentifierComponent.setValue(new Identifier()
				.setSystem(ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER).setValue(projectIdentifier));

		return Stream.of(binaryComponent, projectIdentifierComponent);
	}
}
