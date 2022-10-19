package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
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
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class CommunicateReceivedDataSet extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(CommunicateReceivedDataSet.class);

	private final MailService mailService;

	public CommunicateReceivedDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		Task currentTask = getCurrentTaskFromExecutionVariables(execution);
		Task leadingTask = getLeadingTaskFromExecutionVariables(execution);

		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		String organizationIdentifier = getOrganizationIdentifier(currentTask);

		log(cosIdentifier, organizationIdentifier, projectIdentifier, leadingTask.getId());
		sendMail(cosIdentifier, organizationIdentifier, projectIdentifier);

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

	private void log(String cosIdentifier, String organizationIdentifier, String projectIdentifier, String taskId)
	{
		logger.info("COS '{}' received data-set from organization '{}' in data-sharing project '{}' for task-id '{}'",
				cosIdentifier, organizationIdentifier, projectIdentifier, taskId);
	}

	private void sendMail(String cosIdentifier, String organizationIdentifier, String projectIdentifier)
	{
		String subject = "New data received in process '" + ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING
				+ "'";
		String message = "New data has been stored at COS '" + cosIdentifier + "' for data-sharing project '"
				+ projectIdentifier + "' in process '" + ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING
				+ "' received from organization '" + organizationIdentifier + "':\n";

		mailService.send(subject, message);
	}
}
