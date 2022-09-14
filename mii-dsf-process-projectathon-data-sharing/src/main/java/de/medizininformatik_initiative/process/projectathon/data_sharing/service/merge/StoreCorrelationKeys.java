package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.List;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class StoreCorrelationKeys extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreCorrelationKeys.class);

	public StoreCorrelationKeys(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws BpmnError, Exception
	{
		Task task = getLeadingTaskFromExecutionVariables();

		String projectIdentifier = getProjectIdentifier(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER,
				Variables.stringValue(projectIdentifier));

		List<Target> targets = getTargets(task);
		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS,
				TargetsValues.create(new Targets(targets)));

		logger.info(
				"Starting data-set reception and merging of approved data sharing project [project-identifier: {}, medic-count: {}]",
				projectIdentifier, targets.size());
	}

	private String getProjectIdentifier(Task task)
	{
		return task.getInput().stream().filter(i -> i.getType().getCoding().stream()
				.anyMatch(c -> ConstantsDataSharing.CODESYSTEM_DATA_SHARING.equals(c.getSystem())
						&& ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER.equals(c.getCode())))
				.filter(i -> i.getValue() instanceof Identifier).map(i -> (Identifier) i.getValue())
				.filter(i -> ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst().orElseThrow(() -> new RuntimeException(
						"No project-identifier present in task with id='" + task.getId() + "'"));
	}

	private List<Target> getTargets(Task task)
	{
		return getTaskHelper()
				.getInputParameterStringValues(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY)
				.map(correlationKey -> Target.createBiDirectionalTarget("", "", "", correlationKey))
				.collect(Collectors.toList());
	}
}
