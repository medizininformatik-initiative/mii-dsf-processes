package de.medizininformatik_initiative.processes.projectathon.data_transfer.message;

import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_DATA_SET_REFERENCE;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import ca.uhn.fhir.context.FhirContext;

public class StartReceiveProcess extends AbstractTaskMessageSend
{
	public StartReceiveProcess(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	/*
	 * TODO we should send the http://medizininformatik-initiative.de/sid/project-identifier as an input parameter (Task
	 * profile would require modification). On the receiving end, the identifier should be validated against current
	 * active projects. Maybe check if an active ResearchStudy resource with the same project identifier exists. The
	 * ResearchStudy could also reference sending organizations, to not except data from every one. The ResearchStudy
	 * active and sending organization expected status should be checked as part of a validate Task before downloading
	 * the Binary step.
	 */
	@Override
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String binaryId = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE);

		ParameterComponent parameterComponent = new ParameterComponent();
		parameterComponent.getType().addCoding().setSystem(CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(CODESYSTEM_MII_DATA_TRANSFER_VALUE_DATA_SET_REFERENCE);
		parameterComponent.setValue(new Reference().setType(ResourceType.Binary.name()).setReference(binaryId));

		return Stream.of(parameterComponent);
	}
}
