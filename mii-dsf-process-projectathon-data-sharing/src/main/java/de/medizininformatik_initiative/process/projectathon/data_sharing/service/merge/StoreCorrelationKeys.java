package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.List;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Task;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class StoreCorrelationKeys extends AbstractServiceDelegate
{
	public StoreCorrelationKeys(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getLeadingTaskFromExecutionVariables();

		List<Target> targets = getTaskHelper()
				.getInputParameterStringValues(task, ConstantsDataSharing.CODESYSTEM_MII_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_MII_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY)
				.map(correlationKey -> Target.createBiDirectionalTarget("", "", "", correlationKey))
				.collect(Collectors.toList());

		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS,
				TargetsValues.create(new Targets(targets)));
	}
}
