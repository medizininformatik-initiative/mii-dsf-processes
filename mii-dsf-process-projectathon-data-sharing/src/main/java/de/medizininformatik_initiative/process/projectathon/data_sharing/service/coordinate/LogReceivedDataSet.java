package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import java.util.List;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class LogReceivedDataSet extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogReceivedDataSet.class);

	public LogReceivedDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task currentTask = getCurrentTaskFromExecutionVariables();
		Task leadingTask = getLeadingTaskFromExecutionVariables();

		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String organizationIdentifier = getOrganizationIdentifier(currentTask);

		logger.info("COS received data-set from organization='{}' in project='{}' for task-id='{}'",
				organizationIdentifier, projectIdentifier, leadingTask.getId());

		List<Target> targets = ((Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS))
				.getEntries();
		List<Target> targetsWithoutReceivedIdentifier = targets.stream()
				.filter(t -> !organizationIdentifier.equals(t.getOrganizationIdentifierValue()))
				.collect(Collectors.toList());
		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS,
				TargetsValues.create(new Targets(targetsWithoutReceivedIdentifier)));
	}

	private String getOrganizationIdentifier(Task task)
	{
		return getTaskHelper()
				.getFirstInputParameterReferenceValue(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER)
				.map(Reference::getIdentifier).map(Identifier::getValue).orElse("unknown");
	}
}
