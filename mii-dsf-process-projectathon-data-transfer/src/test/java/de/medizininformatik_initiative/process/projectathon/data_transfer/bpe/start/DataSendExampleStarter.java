package de.medizininformatik_initiative.process.projectathon.data_transfer.bpe.start;

import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Date;
import java.util.UUID;

import org.highmed.dsf.bpe.start.ExampleStarter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;

public class DataSendExampleStarter
{
	public static void main(String[] args) throws Exception
	{
		Task task = createTask();
		ExampleStarter.forServer(args, "https://dic1/fhir").startWith(task);
	}

	private static Task createTask()
	{
		Task task = new Task();
		task.setIdElement(new IdType("urn:uuid:" + UUID.randomUUID().toString()));

		task.getMeta().addProfile(ConstantsDataTransfer.PROFILE_MII_TASK_START_DATA_SEND_AND_LATEST_VERSION);
		task.setInstantiatesUri(ConstantsDataTransfer.PROFILE_MII_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1");
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_DIC1");

		task.addInput().setValue(new StringType(ConstantsDataTransfer.PROFILE_MII_TASK_START_DATA_SEND_MESSAGE_NAME))
				.getType().addCoding().setSystem(CODESYSTEM_HIGHMED_BPMN)
				.setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		task.addInput()
				.setValue(new Reference().setIdentifier(
						new Identifier().setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue("Test_COS"))
						.setType(ResourceType.Organization.name()))
				.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_COORDINATING_SITE_IDENTIFIER);

		task.addInput()
				.setValue(new Identifier().setSystem(ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER)
						.setValue("Test_PROJECT"))
				.getType().addCoding().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER);

		return task;
	}
}
