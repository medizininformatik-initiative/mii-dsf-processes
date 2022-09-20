package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.KdsClient;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class ReadDataSet extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ReadDataSet.class);

	private final KdsClientFactory kdsClientFactory;

	public ReadDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, KdsClientFactory kdsClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.kdsClientFactory = kdsClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(kdsClientFactory, "kdsClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);

		KdsClient kdsClient = kdsClientFactory.getKdsClient();

		logger.info(
				"Reading data-set on FHIR server with baseUrl='{}' for COS-identifier='{}' and project-identifier='{}' referenced in Task with id='{}'",
				kdsClient.getFhirBaseUrl(), cosIdentifier, projectIdentifier, task.getId());

		DocumentReference documentReference = readDocumentReference(kdsClient, projectIdentifier, task.getId());
		Resource resource = readAttachment(kdsClient, documentReference, task.getId());

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE,
				FhirResourceValues.create(documentReference));
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_RESOURCE,
				FhirResourceValues.create(resource));
	}

	private DocumentReference readDocumentReference(KdsClient kdsClient, String projectIdentifier, String taskId)
	{
		List<DocumentReference> documentReferences = kdsClient
				.searchDocumentReferences(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER, projectIdentifier)
				.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
				.filter(r -> r instanceof DocumentReference).map(r -> ((DocumentReference) r)).collect(toList());

		if (documentReferences.size() < 1)
			throw new IllegalArgumentException("Could not find any DocumentReference for project-identifier='"
					+ projectIdentifier + "' referenced in task with id='" + taskId + "'");

		if (documentReferences.size() > 1)
			logger.warn(
					"Found {} DocumentReferences for project-identifier='{}' referenced in task with id='{}', using first ({})",
					documentReferences.size(), projectIdentifier, taskId,
					documentReferences.get(0).getIdElement().getValue());

		return documentReferences.get(0);
	}

	private Resource readAttachment(KdsClient kdsClient, DocumentReference documentReference, String taskId)
	{
		String url = getAttachmentUrl(documentReference, taskId);
		IdType urlIdType = checkValidKdsFhirStoreUrlAndGetIdType(kdsClient, url, documentReference.getId(), taskId);

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
			throw new IllegalArgumentException("Could not find any attachment URLs in DocumentReference with id='"
					+ documentReference.getId() + "' belonging to task with id='" + taskId + "'");

		if (urls.size() > 1)
			logger.warn(
					"Found {} attachment URLs in DocumentReference with id='{}' belonging to task with id='{}', using first ({})",
					urls.size(), documentReference.getId(), taskId, urls.get(0));

		return urls.get(0);
	}

	private IdType checkValidKdsFhirStoreUrlAndGetIdType(KdsClient kdsClient, String url, String documentReferenceId,
			String taskId)
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
				throw new IllegalArgumentException("Attachment URL " + url + " in DocumentReference with id='"
						+ documentReferenceId + "' belonging to task with id='" + taskId
						+ "' is not a valid KDS FHIR store reference");
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
}
