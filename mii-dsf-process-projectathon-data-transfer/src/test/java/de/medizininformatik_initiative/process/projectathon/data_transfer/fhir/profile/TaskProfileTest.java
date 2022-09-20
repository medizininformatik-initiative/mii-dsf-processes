package de.medizininformatik_initiative.process.projectathon.data_transfer.fhir.profile;

import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.highmed.dsf.fhir.validation.ResourceValidator;
import org.highmed.dsf.fhir.validation.ResourceValidatorImpl;
import org.highmed.dsf.fhir.validation.ValidationSupportRule;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.DataTransferProcessPluginDefinition;

public class TaskProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			DataTransferProcessPluginDefinition.VERSION, DataTransferProcessPluginDefinition.RELEASE_DATE,
			Arrays.asList("highmed-task-base-0.5.0.xml", "mii-projectathon-task-start-data-receive.xml",
					"mii-projectathon-task-start-data-send.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml",
					"mii-data-transfer.xml", "mii-cryptography.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml",
					"mii-data-transfer.xml", "mii-cryptography.xml"));

	private ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	private void logTask(Task task)
	{
		logger.debug("task: {}",
				validationRule.getFhirContext().newXmlParser().setPrettyPrint(true).encodeResourceToString(task));
	}

	@Test
	public void testTaskStartDataSendValid() throws Exception
	{
		Task task = createValidTaskStartDataSend();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartDataSendValidWithBusinessKey() throws Exception
	{
		Task task = createValidTaskStartDataSend();
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataSend()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataTransfer.PROFILE_MII_TASK_START_DATA_SEND);
		task.setInstantiatesUri(ConstantsDataTransfer.PROFILE_MII_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.addInput().setValue(new StringType(ConstantsDataTransfer.PROFILE_MII_TASK_START_DATA_SEND_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		task.addInput()
				.setValue(new Reference().setIdentifier(
						new Identifier().setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_COS_IDENTIFIER);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER)
						.setValue("Test_PROJECT"))
				.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER);

		return task;
	}

	@Test
	public void testTaskStartDataReceiveValid()
	{
		Task task = createValidTaskStartDataReceive();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskStartDataReceive()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataTransfer.PROFILE_MII_TASK_START_DATA_RECEIVE);
		task.setInstantiatesUri(ConstantsDataTransfer.PROFILE_MII_TASK_DATA_RECEIVE_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS");
		task.addInput().setValue(new StringType(ConstantsDataTransfer.PROFILE_MII_TASK_START_DATA_RECEIVE_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput()
				.setValue(new Reference().setReference("https://dsf-dic.de/fhir/Binary/" + UUID.randomUUID().toString())
						.setType(ResourceType.Binary.name()))
				.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_DATA_SET_REFERENCE);
		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER)
						.setValue("Test_PROJECT"))
				.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER);
		return task;
	}
}
