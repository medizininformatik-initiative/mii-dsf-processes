package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class LogMissingDataSetsCoordinate extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(LogMissingDataSetsCoordinate.class);

	public LogMissingDataSetsCoordinate(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String taskId = getLeadingTaskFromExecutionVariables(execution).getId();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Targets targets = (Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS);

		logMissingDataSets(targets, taskId, projectIdentifier);
		outputMissingDataSets(execution, targets);
	}

	private void logMissingDataSets(Targets targets, String taskId, String projectIdentifier)
	{
		targets.getEntries().forEach(target -> log(target, taskId, projectIdentifier));
	}

	private void log(Target target, String taskId, String projectIdentifier)
	{
		logger.warn("Missing data-set from organization='{}' in project='{}' and task-id='{}'",
				target.getOrganizationIdentifierValue(), taskId, projectIdentifier);
	}

	private void outputMissingDataSets(DelegateExecution execution, Targets targets)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		targets.getEntries().forEach(target -> output(task, target));
		updateLeadingTaskInExecutionVariables(execution, task);
	}

	private void output(Task task, Target target)
	{
		task.addOutput()
				.setValue(new Reference()
						.setIdentifier(
								new Identifier().setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
										.setValue(target.getOrganizationIdentifierValue()))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_MISSING);
	}
}
