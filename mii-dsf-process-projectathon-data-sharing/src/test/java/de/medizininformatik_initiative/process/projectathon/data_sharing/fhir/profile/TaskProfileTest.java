package de.medizininformatik_initiative.process.projectathon.data_sharing.fhir.profile;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.highmed.dsf.bpe.ConstantsBase;
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
import org.hl7.fhir.r4.model.UrlType;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.DataSharingProcessPluginDefinition;

public class TaskProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			DataSharingProcessPluginDefinition.VERSION, DataSharingProcessPluginDefinition.RELEASE_DATE,
			Arrays.asList("highmed-task-base-0.5.0.xml", "mii-projectathon-extension-medic-identifier.xml",
					"mii-projectathon-task-coordinate-data-sharing.xml",
					"mii-projectathon-task-execute-data-sharing.xml", "mii-projectathon-task-merge-data-sharing.xml",
					"mii-projectathon-task-send-received-data-set.xml", "mii-projectathon-task-send-data-set.xml",
					"mii-projectathon-task-send-merged-data-set.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml",
					"mii-data-sharing.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml",
					"mii-data-sharing.xml"));

	private ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	private void logTask(Task task)
	{
		logger.debug("task: {}",
				validationRule.getFhirContext().newXmlParser().setPrettyPrint(true).encodeResourceToString(task));
	}

	@Test
	public void testValidTaskCoordinateDataSharing()
	{
		Task task = createValidTaskCoordinateDataSharing();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskCoordinateDataSharing()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataSharing.COORDINATE_DATA_SHARING_TASK_PROFILE);
		task.setInstantiatesUri(ConstantsDataSharing.COORDINATE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.addInput().setValue(new StringType(ConstantsDataSharing.COORDINATE_DATA_SHARING_MESSAGE_NAME)).getType()
				.addCoding().setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER)
						.setValue("Test_PROJECT"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER);

		task.addInput().setValue(new UrlType("http://forschen-fuer-gesundheit.de/contract/test_project")).getType()
				.addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_RESEARCHER_IDENTIFIER)
						.setValue("Test_Researcher1"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER);
		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_RESEARCHER_IDENTIFIER)
						.setValue("Test_Researcher2"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER);

		task.addInput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER);
		task.addInput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC2"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER);

		task.addInput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER);

		task.addOutput().setValue(new UrlType("http://example.foo")).getType().addCoding()
				.setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);

		task.addOutput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_MISSING);

		return task;
	}

	@Test
	public void testValidTaskExecuteDataSharing()
	{
		Task task = createValidTaskExecuteDataSharing();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskExecuteDataSharing()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataSharing.EXECUTE_DATA_SHARING_TASK_PROFILE);
		task.setInstantiatesUri(ConstantsDataSharing.EXECUTE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1");
		task.addInput().setValue(new StringType(ConstantsDataSharing.EXECUTE_DATA_SHARING_MESSAGE_NAME)).getType()
				.addCoding().setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER)
						.setValue("Test_PROJECT"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER);

		task.addInput().setValue(new UrlType("http://forschen-fuer-gesundheit.de/contract/test_project")).getType()
				.addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION);

		task.addInput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER);

		return task;
	}

	@Test
	public void testValidTaskMergeDataSharing()
	{
		Task task = createValidTaskMergeDataSharing();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskMergeDataSharing()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataSharing.MERGE_DATA_SHARING_TASK_PROFILE);
		task.setInstantiatesUri(ConstantsDataSharing.MERGE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS");
		task.addInput().setValue(new StringType(ConstantsDataSharing.MERGE_DATA_SHARING_MESSAGE_NAME)).getType()
				.addCoding().setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER)
						.setValue("Test_PROJECT"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER);

		task.addInput().setValue(new UrlType("http://forschen-fuer-gesundheit.de/contract/test_project")).getType()
				.addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_RESEARCHER_IDENTIFIER)
						.setValue("Test_Researcher1"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER);
		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_RESEARCHER_IDENTIFIER)
						.setValue("Test_Researcher2"))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER);

		Task.ParameterComponent dic1 = task.addInput().setValue(new StringType(UUID.randomUUID().toString()));
		dic1.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY);
		dic1.addExtension().setUrl(ConstantsDataSharing.EXTENSION_URL_MEDIC_IDENTIFIER)
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1"))
						.setType(ResourceType.Organization.name()));

		Task.ParameterComponent dic2 = task.addInput().setValue(new StringType(UUID.randomUUID().toString()));
		dic2.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY);
		dic2.addExtension().setUrl(ConstantsDataSharing.EXTENSION_URL_MEDIC_IDENTIFIER)
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC2"))
						.setType(ResourceType.Organization.name()));

		task.addOutput()
				.setValue(new Reference("http://example.foo/fhir/DocumentReference/1")
						.setType(ResourceType.DocumentReference.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DOCUMENT_REFERENCE_REFERENCE);

		task.addOutput().setValue(new UrlType("http://example.foo")).getType().addCoding()
				.setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);

		task.addOutput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_MISSING);

		return task;
	}

	@Test
	public void testValidTaskSendDataSet()
	{
		Task task = createValidTaskSendDataSet();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskSendDataSet()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataSharing.SEND_DATA_SET_TASK_PROFILE);
		task.setInstantiatesUri(ConstantsDataSharing.MERGE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS");
		task.addInput().setValue(new StringType(ConstantsDataSharing.SEND_DATA_SET_MESSAGE_NAME)).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY);

		task.addInput()
				.setValue(new Reference().setReference("https://dic1/fhir/Binary/" + UUID.randomUUID().toString())
						.setType(ResourceType.Binary.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_REFERENCE);

		return task;
	}

	@Test
	public void testValidTaskSendReceivedDataSet()
	{
		Task task = createValidTaskSendReceivedDataSet();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskSendReceivedDataSet()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataSharing.SEND_RECEIVED_DATA_SET_TASK_PROFILE);
		task.setInstantiatesUri(ConstantsDataSharing.COORDINATE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.addInput().setValue(new StringType(ConstantsDataSharing.SEND_RECEIVED_DATA_SET_MESSAGE_NAME)).getType()
				.addCoding().setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		task.addInput()
				.setValue(new Reference().setIdentifier(new Identifier()
						.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER);

		return task;
	}

	@Test
	public void testValidTaskSendMergedDataSet()
	{
		Task task = createValidTaskSendMergedDataSet();
		logTask(task);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskSendMergedDataSet()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsDataSharing.SEND_MERGED_DATA_SET_TASK_PROFILE);
		task.setInstantiatesUri(ConstantsDataSharing.COORDINATE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.addInput().setValue(new StringType(ConstantsDataSharing.SEND_MERGED_DATA_SET_MESSAGE_NAME)).getType()
				.addCoding().setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(ConstantsBase.CODESYSTEM_HIGHMED_BPMN)
				.setCode(ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput().setValue(new UrlType("http://test.foo")).getType().addCoding()
				.setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);

		return task;
	}
}
