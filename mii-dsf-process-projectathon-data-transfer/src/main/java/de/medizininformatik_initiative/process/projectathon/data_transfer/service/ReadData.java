package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.processes.kds.client.KdsClient;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class ReadData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ReadData.class);

	private final FhirContext fhirContext;
	private final KdsClientFactory kdsClientFactory;

	public ReadData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext, KdsClientFactory kdsClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fhirContext = fhirContext;
		this.kdsClientFactory = kdsClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(kdsClientFactory, "kdsClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String projectIdentifier = getProjectIdentifier(task);
		String cosIdentifier = getCoordinatingSiteIdentifier(task);

		KdsClient kdsClient = kdsClientFactory.getKdsClient();

		logger.info(
				"Reading data-set on FHIR server with baseUrl '{}' for COS '{}' and data-transfer project '{}' referenced in Task with id '{}'",
				kdsClient.getFhirBaseUrl(), cosIdentifier, projectIdentifier, task.getId());

		try
		{
			DocumentReference documentReference = readDocumentReference(kdsClient, projectIdentifier, task.getId());
			Resource resource = readAttachment(kdsClient, documentReference, task.getId());

			execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER,
					Variables.stringValue(projectIdentifier));
			execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER,
					Variables.stringValue(cosIdentifier));
			execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE,
					FhirResourceValues.create(documentReference));
			execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_RESOURCE,
					FhirResourceValues.create(resource));
		}
		catch (Exception exception)
		{
			logger.warn(
					"Could not read data-set on FHIR server with baseUrl '{}' for COS '{}' and data-transfer project '{}' referenced in Task with id '{}' - {}",
					kdsClient.getFhirBaseUrl(), cosIdentifier, projectIdentifier, task.getId(), exception.getMessage());
			throw new RuntimeException("Could not read data-set on FHIR server with baseUrl '"
					+ kdsClient.getFhirBaseUrl() + "' for COS '" + cosIdentifier + "' and data-transfer project '"
					+ projectIdentifier + "' referenced in Task with id '" + task.getId() + "'", exception);
		}
	}

	private String getProjectIdentifier(Task task)
	{
		List<String> identifiers = task.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER.equals(c.getSystem())
								&& ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER
										.equals(c.getCode())))
				.filter(i -> i.getValue() instanceof Identifier).map(i -> (Identifier) i.getValue())
				.filter(i -> ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).collect(toList());

		if (identifiers.size() < 1)
			throw new IllegalArgumentException("No project-identifier present in Task with id '" + task.getId() + "'");

		if (identifiers.size() > 1)
			logger.warn("Found {} project-identifiers in Task with id '{}', using only the first", identifiers.size(),
					task.getId());

		return identifiers.get(0);
	}

	private String getCoordinatingSiteIdentifier(Task task)
	{
		return getTaskHelper()
				.getFirstInputParameterReferenceValue(task, ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER,
						ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_COS_IDENTIFIER)
				.orElseThrow(
						() -> new IllegalArgumentException("No coordinating site identifier present in Task with id '"
								+ task.getId() + "', this should have been caught by resource validation"))
				.getIdentifier().getValue();
	}

	private DocumentReference readDocumentReference(KdsClient kdsClient, String projectIdentifier, String taskId)
	{
		List<DocumentReference> documentReferences = kdsClient
				.searchDocumentReferences(ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER, projectIdentifier)
				.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
				.filter(r -> r instanceof DocumentReference).map(r -> ((DocumentReference) r)).collect(toList());

		if (documentReferences.size() < 1)
			throw new IllegalArgumentException("Could not find any DocumentReference for data-transfer project '"
					+ projectIdentifier + "' on FHIR store with baseUrl '" + kdsClient.getFhirBaseUrl()
					+ "' referenced in Task with id '" + taskId + "'");

		if (documentReferences.size() > 1)
			logger.warn(
					"Found {} DocumentReferences for data-transfer project '{}' referenced in Task with id '{}', using first ({})",
					documentReferences.size(), projectIdentifier, taskId,
					documentReferences.get(0).getIdElement().getValue());

		return documentReferences.get(0);
	}

	private Resource readAttachment(KdsClient kdsClient, DocumentReference documentReference, String taskId)
	{
		String url = getAttachmentUrl(documentReference, taskId);
		IdType urlIdType = checkValidKdsFhirStoreUrlAndGetIdType(kdsClient, url, documentReference, taskId);

		return readAttachment(kdsClient, urlIdType);
	}

	private String getAttachmentUrl(DocumentReference documentReference, String taskId)
	{
		List<String> urls = Stream.of(documentReference).filter(DocumentReference::hasContent)
				.flatMap(dr -> dr.getContent().stream())
				.filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
				.map(DocumentReference.DocumentReferenceContentComponent::getAttachment).filter(Attachment::hasUrl)
				.map(Attachment::getUrl).collect(toList());

		if (urls.size() < 1)
			throw new IllegalArgumentException("Could not find any attachment URLs in DocumentReference with id '"
					+ getKdsFhirStoreAbsoluteId(documentReference.getIdElement()) + "' belonging to task with id '"
					+ taskId + "'");

		if (urls.size() > 1)
			logger.warn(
					"Found {} attachment URLs in DocumentReference with id '{}' belonging to task with id '{}', using first ({})",
					urls.size(), getKdsFhirStoreAbsoluteId(documentReference.getIdElement()), taskId, urls.get(0));

		return urls.get(0);
	}

	private IdType checkValidKdsFhirStoreUrlAndGetIdType(KdsClient kdsClient, String url,
			DocumentReference documentReference, String taskId)
	{
		try
		{
			IdType idType = new IdType(url);
			String fhirBaseUrl = kdsClient.getFhirBaseUrl();

			// expecting no Base URL or, Base URL equal to KDS client Base URL
			boolean hasValidBaseUrl = !idType.hasBaseUrl() || fhirBaseUrl.equals(idType.getBaseUrl());
			boolean isResourceReference = idType.hasResourceType() && idType.hasIdPart();

			if (hasValidBaseUrl && isResourceReference)
				return idType;
			else
				throw new IllegalArgumentException("Attachment URL " + url + " in DocumentReference with id '"
						+ getKdsFhirStoreAbsoluteId(documentReference.getIdElement()) + "' belonging to task with id '"
						+ taskId + "' is not a valid KDS FHIR store reference (baseUrl if not empty must match '"
						+ kdsClient.getFhirBaseUrl() + "', resource type must be set, id must be set)");
		}
		catch (Exception exception)
		{
			logger.error("Could not check if attachment url is a valid KDS FHIR store url: {}", exception.getMessage());
			throw exception;
		}
	}

	private Resource readAttachment(KdsClient kdsClient, IdType idType)
	{
		return kdsClient.readByIdType(idType);
	}

	private String getKdsFhirStoreAbsoluteId(IdType idType)
	{
		return new IdType(kdsClientFactory.getKdsClient().getFhirBaseUrl(), idType.getResourceType(),
				idType.getIdPart(), idType.getVersionIdPart()).getValue();
	}
}
