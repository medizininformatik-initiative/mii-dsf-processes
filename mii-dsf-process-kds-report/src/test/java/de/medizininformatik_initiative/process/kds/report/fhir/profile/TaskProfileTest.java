package de.medizininformatik_initiative.process.kds.report.fhir.profile;

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
import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;
import de.medizininformatik_initiative.process.kds.report.KdsReportProcessPluginDefinition;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class TaskProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(TaskProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			KdsReportProcessPluginDefinition.VERSION, KdsReportProcessPluginDefinition.RELEASE_DATE,
			Arrays.asList("highmed-task-base-0.5.0.xml", "extension-mii-kds-report-status-error.xml",
					"mii-kds-report-search-bundle.xml", "mii-kds-report-search-bundle-response.xml",
					"mii-kds-report-task-autostart-start.xml", "mii-kds-report-task-autostart-stop.xml",
					"mii-kds-report-task-receive.xml", "mii-kds-report-task-send.xml",
					"mii-kds-report-task-send-start.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml", "mii-kds-report.xml",
					"mii-kds-report-status.xml"),
			Arrays.asList("highmed-read-access-tag-0.5.0.xml", "highmed-bpmn-message-0.5.0.xml", "mii-kds-report.xml",
					"mii-kds-report-status-receive.xml", "mii-kds-report-status-send.xml"));

	private ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testTaskAutostartStartProcessProfileValid() throws Exception
	{
		Task task = createValidTaskAutostartStartProcess();

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskAutostartStartProcessProfileValidTimerInterval() throws Exception
	{
		Task task = createValidTaskAutostartStartProcess();
		task.addInput().setValue(new StringType("P30D")).getType().addCoding()
				.setSystem(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT)
				.setCode(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_TIMER_INTERVAL);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskStartAutostartProcessProfileNotValidTimerInterval() throws Exception
	{
		Task task = createValidTaskAutostartStartProcess();
		task.addInput().setValue(new StringType("P10X")).getType().addCoding()
				.setSystem(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT)
				.setCode(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_TIMER_INTERVAL);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(1, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskAutostartStartProcess()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START);
		task.setInstantiatesUri(
				ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");

		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput()
				.setValue(new StringType(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		return task;
	}

	@Test
	public void testTaskAutostartStopProcessProfileValid() throws Exception
	{
		Task task = createValidTaskAutostartStopProcess();

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskAutostartStopProcess()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_STOP);
		task.setInstantiatesUri(
				ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");

		task.addInput()
				.setValue(new StringType(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_STOP_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}

	@Test
	public void testTaskSendStartProcessProfileValid() throws Exception
	{
		Task task = createValidTaskSendStartProcess();

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskSendStartProcessProfileValidWithBuisnessKeyOutput() throws Exception
	{
		Task task = createValidTaskSendStartProcess();
		task.addOutput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskSendStartProcessProfileValidWithBusinessKeyAndKdsReportStatusOutput() throws Exception
	{
		Task task = createValidTaskSendStartProcess();
		task.addOutput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addOutput(new KdsReportStatusGenerator()
				.createKdsReportStatusOutput(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_OK));

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskSendStartProcessProfileValidWithBusinessKeyAndKdsReportStatusErrorOutput() throws Exception
	{
		Task task = createValidTaskSendStartProcess();
		task.addOutput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addOutput(new KdsReportStatusGenerator().createKdsReportStatusOutput(
				ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_REACHABLE, "some error message"));

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskSendStartProcess()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_SEND_START);
		task.setInstantiatesUri(
				ConstantsKdsReport.PROFILE_KDS_MII_REPORT_TASK_SEND_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");

		task.addInput().setValue(new StringType(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_SEND_START_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		return task;
	}

	@Test
	public void testTaskSendProcessProfileValid() throws Exception
	{
		Task task = createValidTaskSendProcess();

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskSendProcessProfileValidWithKdsReportStatusOutput() throws Exception
	{
		Task task = createValidTaskSendProcess();
		task.addOutput(new KdsReportStatusGenerator()
				.createKdsReportStatusOutput(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_OK));

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskSendProcessProfileValidWithKdsReportStatusErrorOutput() throws Exception
	{
		Task task = createValidTaskSendProcess();
		task.addOutput(new KdsReportStatusGenerator().createKdsReportStatusOutput(
				ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR, "some error message"));

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskSendProcess()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_SEND);
		task.setInstantiatesUri(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("HRP");

		task.addInput().setValue(new StringType(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_SEND_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		task.addInput()
				.setValue(new Reference("http://foo.bar/fhir/Bundle/" + UUID.randomUUID())
						.setType(ResourceType.Bundle.name()))
				.getType().addCoding().setSystem(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT)
				.setCode(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE);

		return task;
	}

	@Test
	public void testTaskReceiveProcessProfileValidWithResponseInput() throws Exception
	{
		Task task = createValidTaskReceiveProcess();
		task.addInput(new KdsReportStatusGenerator()
				.createKdsReportStatusInput(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_OK));

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testTaskReceiveProcessProfileValidWithResponseInputError() throws Exception
	{
		Task task = createValidTaskReceiveProcess();
		task.addInput(new KdsReportStatusGenerator().createKdsReportStatusInput(
				ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_ERROR, "some error message"));

		ValidationResult result = resourceValidator.validate(task);
		ValidationSupportRule.logValidationMessages(logger, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	private Task createValidTaskReceiveProcess()
	{
		Task task = new Task();
		task.getMeta().addProfile(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_RECEIVE);
		task.setInstantiatesUri(
				ConstantsKdsReport.PROFILE_KDS_MII_REPORT_TASK_SEND_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("HRP");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("DIC");

		task.addInput().setValue(new StringType(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_RECEIVE_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType(UUID.randomUUID().toString())).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}
}
