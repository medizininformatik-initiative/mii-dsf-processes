package de.medizininformatik_initiative.process.kds.report.bpe.start;

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

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;

public class KdsReportAutostartStartExampleStarter
{
	private static final String DIC_URL = "https://dic1/fhir";
	private static final String DIC_IDENTIFIER = "Test_DIC1";

	public static void main(String[] args) throws Exception
	{
		ExampleStarter starter = ExampleStarter.forServer(args, DIC_URL);

		Task task = createTask();
		starter.startWith(task);
	}

	private static Task createTask()
	{
		Task task = new Task();
		task.setIdElement(new IdType("urn:uuid:" + UUID.randomUUID().toString()));

		task.getMeta().addProfile(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START);
		task.setInstantiatesUri(
				ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(Task.TaskStatus.REQUESTED);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(DIC_IDENTIFIER);
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(DIC_IDENTIFIER);

		task.addInput().setValue(new StringType("9ad28295-eccc-41c2-b0f0-c9db0b229f26")).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY);
		task.addInput()
				.setValue(new StringType(ConstantsKdsReport.PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		task.addInput().setValue(new StringType("PT5M")).getType().addCoding()
				.setSystem(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT)
				.setCode(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_TIMER_INTERVAL);

		return task;
	}
}
