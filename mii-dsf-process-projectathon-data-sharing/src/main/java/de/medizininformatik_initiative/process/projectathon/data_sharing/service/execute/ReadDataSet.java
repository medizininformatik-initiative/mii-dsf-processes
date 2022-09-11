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
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
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
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		Task task = getLeadingTaskFromExecutionVariables();
		DocumentReference documentReference = readDocumentReference(projectIdentifier, task.getId());
		Binary binary = readBinary(documentReference, task.getId());

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE,
				FhirResourceValues.create(documentReference));
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_BINARY, FhirResourceValues.create(binary));
	}

	private DocumentReference readDocumentReference(String projectIdentifier, String taskId)
	{
		List<DocumentReference> documentReferences = kdsClientFactory.getKdsClient()
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

	private Binary readBinary(DocumentReference documentReference, String taskId)
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

		if (!validBinaryUrl(urls.get(0)))
		{
			throw new IllegalArgumentException(
					"Attachment URL " + urls.get(0) + " in DocumentReference with id='" + documentReference.getId()
							+ "' belonging to task with id='" + taskId + "' not a valid Binary reference");
		}

		return readBinary(urls.get(0));
	}

	private boolean validBinaryUrl(String url)
	{
		IdType idType = new IdType(url);
		String fhirBaseUrl = kdsClientFactory.getKdsClient().getFhirBaseUrl();

		// expecting no Base URL or, Base URL equal to KDS client Base URL
		boolean hasValidBaseUrl = !idType.hasBaseUrl() || fhirBaseUrl.equals(idType.getBaseUrl());
		boolean isBinaryReference = ResourceType.Binary.name().equals(idType.getResourceType());

		return hasValidBaseUrl && isBinaryReference;
	}

	private Binary readBinary(String url)
	{
		return kdsClientFactory.getKdsClient().readBinary(url);
	}
}
