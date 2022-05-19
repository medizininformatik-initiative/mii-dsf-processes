package de.medizininformatik_initiative.process.report.bpe.start;

import static de.medizininformatik_initiative.process.report.ConstantsReport.PROFILE_MII_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROFILE_MII_REPORT_TASK_AUTOSTART_STOP;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROFILE_MII_REPORT_TASK_AUTOSTART_STOP_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Date;
import java.util.UUID;

import org.highmed.dsf.bpe.start.ExampleStarter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;

public class ReportAutostartStopExampleStarter
{
	public static void main(String[] args) throws Exception
	{
		Task task = createTask();
		ExampleStarter.forServer(args, "https://dic/fhir").startWith(task);
	}

	private static Task createTask()
	{
		Task task = new Task();
		task.setIdElement(new IdType("urn:uuid:" + UUID.randomUUID().toString()));

		task.getMeta().addProfile(PROFILE_MII_REPORT_TASK_AUTOSTART_STOP);
		task.setInstantiatesUri(PROFILE_MII_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(Task.TaskStatus.REQUESTED);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC");

		task.addInput().setValue(new StringType(PROFILE_MII_REPORT_TASK_AUTOSTART_STOP_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);
		task.addInput().setValue(new StringType("9ad28295-eccc-41c2-b0f0-c9db0b229f26")).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);

		return task;
	}
}
