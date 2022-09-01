package de.medizininformatik_initiative.process.projectathon.data_sharing.bpe.start;

import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Date;
import java.util.UUID;

import org.highmed.dsf.bpe.start.ExampleStarter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class CoordinateDataSharingExampleStarter
{
	public static void main(String[] args) throws Exception
	{
		Task task = createTask();
		ExampleStarter.forServer(args, "https://hrp/fhir").startWith(task);
	}

	private static Task createTask()
	{
		Task task = new Task();
		task.setIdElement(new IdType("urn:uuid:" + UUID.randomUUID().toString()));

		task.getMeta().addProfile(ConstantsDataSharing.COORDINATE_DATA_SHARING_TASK_PROFILE_WITH_LATEST_VERSION);
		task.setInstantiatesUri(ConstantsDataSharing.COORDINATE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_HRP");

		task.addInput().setValue(new StringType(ConstantsDataSharing.COORDINATE_DATA_SHARING_MESSAGE_NAME)).getType()
				.addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		return task;
	}
}
