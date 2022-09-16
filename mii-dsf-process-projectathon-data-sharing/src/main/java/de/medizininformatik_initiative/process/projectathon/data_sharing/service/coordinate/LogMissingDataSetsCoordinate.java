package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import java.util.List;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
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
		String taskId = getLeadingTaskFromExecutionVariables().getId();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		List<Target> targets = ((Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS))
				.getEntries();

		targets.forEach(target -> logMissingDataSet(target, taskId, projectIdentifier));
	}

	private void logMissingDataSet(Target target, String taskId, String projectIdentifier)
	{
		logger.warn("Missing data-set from organization='{}' in project='{}' and task-id='{}'",
				target.getOrganizationIdentifierValue(), projectIdentifier, taskId);
	}
}
