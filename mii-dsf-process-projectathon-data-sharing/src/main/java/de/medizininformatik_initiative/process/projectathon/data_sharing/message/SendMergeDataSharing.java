package de.medizininformatik_initiative.process.projectathon.data_sharing.message;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class SendMergeDataSharing extends AbstractTaskMessageSend
{
	public SendMergeDataSharing(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		Targets targets = (Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS);

		return targets.getEntries().stream().map(this::transformToInput);
	}

	private Task.ParameterComponent transformToInput(Target target)
	{
		return getTaskHelper().createInput(ConstantsDataSharing.CODESYSTEM_MII_DATA_SHARING,
				ConstantsDataSharing.CODESYSTEM_MII_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY,
				target.getCorrelationKey());
	}
}
