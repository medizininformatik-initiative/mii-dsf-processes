package de.medizininformatik_initiative.process.report.bpe.start;

import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROFILE_MII_REPORT_TASK_SEND_START;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROFILE_MII_REPORT_TASK_SEND_START_MESSAGE_NAME;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROFILE_MII_REPORT_TASK_SEND_START_PROCESS_URI_AND_LATEST_VERSION;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.highmed.dsf.bpe.start.ExampleStarter;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;

public class ReportSendExampleStarter
{
	private static final String DIC_URL = "https://dic1/fhir";
	private static final String DIC_IDENTIFIER = "Test_DIC1";

	private static final String HRP_URL = "https://hrp/fhir";

	public static void main(String[] args) throws Exception
	{
		ExampleStarter starter = ExampleStarter.forServer(args, DIC_URL);

		String searchBundleReference = getSearchBundleReference(starter);
		Task task = createTask(searchBundleReference);

		starter.startWith(task);
	}

	private static String getSearchBundleReference(ExampleStarter starter) throws Exception
	{
		FhirWebserviceClient client = starter.createClient(HRP_URL);
		Bundle searchResult = client.searchWithStrictHandling(Bundle.class, Map.of("identifier",
				Collections.singletonList(CODESYSTEM_MII_REPORT + "|" + CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE)));

		if (searchResult.getTotal() != 1 && searchResult.getEntryFirstRep().getResource() instanceof Bundle)
			throw new IllegalStateException("Expected a single search Bundle");

		Bundle bundle = (Bundle) searchResult.getEntryFirstRep().getResource();
		IdType id = new IdType(HRP_URL, ResourceType.Bundle.name(), bundle.getIdElement().getIdPart(),
				bundle.getIdElement().getVersionIdPart());
		return id.getValue();
	}

	private static Task createTask(String searchBundleReference)
	{
		Task task = new Task();
		task.setIdElement(new IdType("urn:uuid:" + UUID.randomUUID().toString()));

		task.getMeta().addProfile(PROFILE_MII_REPORT_TASK_SEND_START);
		task.setInstantiatesUri(PROFILE_MII_REPORT_TASK_SEND_START_PROCESS_URI_AND_LATEST_VERSION);
		task.setStatus(Task.TaskStatus.REQUESTED);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.getRequester().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(DIC_IDENTIFIER);
		task.getRestriction().addRecipient().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(DIC_IDENTIFIER);

		task.addInput().setValue(new StringType(PROFILE_MII_REPORT_TASK_SEND_START_MESSAGE_NAME)).getType().addCoding()
				.setSystem(CODESYSTEM_HIGHMED_BPMN).setCode(CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME);

		task.addInput().setValue(new Reference(searchBundleReference).setType(ResourceType.Bundle.name())).getType()
				.addCoding().setSystem(CODESYSTEM_MII_REPORT)
				.setCode(CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE);

		return task;
	}
}
