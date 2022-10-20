package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.processes.kds.client.KdsClient;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class InsertData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(InsertData.class);

	private final KdsClientFactory kdsClientFactory;
	private final FhirContext fhirContext;

	private final MailService mailService;

	public InsertData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext, KdsClientFactory kdsClientFactory,
			MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fhirContext = fhirContext;
		this.kdsClientFactory = kdsClientFactory;
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(kdsClientFactory, "kdsClientFactory");
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String sendingOrganization = getLeadingTaskFromExecutionVariables(execution).getRequester().getIdentifier()
				.getValue();
		Bundle bundle = (Bundle) execution.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET);

		KdsClient kdsClient = kdsClientFactory.getKdsClient();

		logger.info(
				"Inserting data-set on FHIR server with baseUrl '{}' received from organization '{}' for data-transfer project '{}'",
				kdsClient.getFhirBaseUrl(), sendingOrganization, projectIdentifier);

		try
		{
			List<IdType> createdIds = storeData(execution, kdsClient, bundle, sendingOrganization, projectIdentifier);
			sendMail(createdIds, sendingOrganization, projectIdentifier);
		}
		catch (Exception exception)
		{
			logger.error(
					"Could not insert data received from organization '{}' for data-transfer project '{}', error-message='{}'",
					sendingOrganization, projectIdentifier, exception.getMessage());
		}
	}

	private List<IdType> storeData(DelegateExecution execution, KdsClient kdsClient, Bundle bundle,
			String sendingOrganization, String projectIdentifier)
	{
		Bundle stored = kdsClient.executeTransactionBundle(bundle);

		List<IdType> idsOfCreatedResources = stored.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResponse)
				.map(Bundle.BundleEntryComponent::getResponse).map(Bundle.BundleEntryResponseComponent::getLocation)
				.map(IdType::new).map(this::setIdBase).collect(toList());

		idsOfCreatedResources.stream().filter(i -> ResourceType.DocumentReference.name().equals(i.getResourceType()))
				.forEach(i -> addOutputToLeadingTask(execution, i));

		idsOfCreatedResources.forEach(id -> toLogMessage(id, sendingOrganization, projectIdentifier));

		return idsOfCreatedResources;
	}

	private void sendMail(List<IdType> createdIds, String sendingOrganization, String projectIdentifier)
	{
		String subject = "New data received in process '" + ConstantsDataTransfer.PROCESS_NAME_FULL_DATA_RECEIVE + "'";
		StringBuilder message = new StringBuilder("New data has been stored for data-transfer project '")
				.append(projectIdentifier).append("' in process '")
				.append(ConstantsDataTransfer.PROCESS_NAME_FULL_DATA_RECEIVE).append("' received from organization '")
				.append(sendingOrganization).append("' and can be accessed using the following links:\n");

		for (IdType id : createdIds)
			message.append("- ").append(id.getValue()).append("\n");

		mailService.send(subject, message.toString());
	}

	private IdType setIdBase(IdType idType)
	{
		String id = idType.getValue();
		String fhirBaseUrl = kdsClientFactory.getKdsClient().getFhirBaseUrl();
		String deliminator = fhirBaseUrl.endsWith("/") ? "" : "/";
		return new IdType(fhirBaseUrl + deliminator + id);
	}

	private void toLogMessage(IdType idType, String sendingOrganization, String projectIdentifier)
	{
		logger.info(
				"Stored {} with id '{}' on FHIR server with baseUrl '{}' received from organization '{}' for data-transfer project '{}'",
				idType.getResourceType(), idType.getIdPart(), idType.getBaseUrl(), sendingOrganization,
				projectIdentifier);
	}

	private void addOutputToLeadingTask(DelegateExecution execution, IdType id)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);

		task.addOutput().setValue(new Reference(id.getValue()).setType(id.getResourceType())).getType().addCoding()
				.setSystem(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_DOCUMENT_REFERENCE_LOCATION);

		updateLeadingTaskInExecutionVariables(execution, task);
	}
}
