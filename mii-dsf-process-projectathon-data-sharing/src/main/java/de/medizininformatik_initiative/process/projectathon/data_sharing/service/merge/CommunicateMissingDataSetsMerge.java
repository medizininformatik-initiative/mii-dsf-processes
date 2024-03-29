package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
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
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class CommunicateMissingDataSetsMerge extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(CommunicateMissingDataSetsMerge.class);

	private final MailService mailService;

	public CommunicateMissingDataSetsMerge(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String taskId = getLeadingTaskFromExecutionVariables(execution).getId();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Targets targets = ((Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS));

		logMissingDataSets(targets, taskId, projectIdentifier);
		sendMail(targets, projectIdentifier);
		outputMissingDataSets(execution, targets);
	}

	private void logMissingDataSets(Targets targets, String taskId, String projectIdentifier)
	{
		targets.getEntries().forEach(target -> log(target, taskId, projectIdentifier));
	}

	private void log(Target target, String taskId, String projectIdentifier)
	{
		logger.warn("Missing data-set from organization '{}' in data-sharing project '{}' and Task with id '{}'",
				target.getOrganizationIdentifierValue(), projectIdentifier, taskId);
	}

	private void sendMail(Targets targets, String projectIdentifier)
	{
		String subject = "Missing data-sets in process '" + ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING
				+ "'";
		StringBuilder message = new StringBuilder("Data-sets are missing for data-sharing project '")
				.append(projectIdentifier).append("' in process '")
				.append(ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING)
				.append("' from the following organizations:\n");

		for (Target target : targets.getEntries())
			message.append("- ").append(target.getOrganizationIdentifierValue()).append("\n");

		mailService.send(subject, message.toString());
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
