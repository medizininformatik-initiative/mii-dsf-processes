package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static java.util.stream.Collectors.toList;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class CreateBundle extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(CreateBundle.class);

	private final OrganizationProvider organizationProvider;
	private final DataLogger dataLogger;

	public CreateBundle(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		DocumentReference documentReference = (DocumentReference) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE);
		Resource resource = (Resource) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_RESOURCE);
		Bundle bundle = createTransactionBundle(execution, projectIdentifier, documentReference, resource);

		dataLogger.logResource("Created Transfer Bundle", bundle);

		execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET,
				FhirResourceValues.create(bundle));
	}

	private Bundle createTransactionBundle(DelegateExecution execution, String projectIdentifier,
			DocumentReference documentReference, Resource resource)
	{
		Resource attachmentToTransmit = resource.setId(UUID.randomUUID().toString());

		DocumentReference documentReferenceToTransmit = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReferenceToTransmit.setId(UUID.randomUUID().toString());
		documentReferenceToTransmit.getMasterIdentifier()
				.setSystem(ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER).setValue(projectIdentifier);
		documentReferenceToTransmit.addAuthor().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
				.setValue(organizationProvider.getLocalIdentifierValue());
		documentReferenceToTransmit.setDate(documentReference.getDate());

		String contentType = getFirstAttachmentContentType(execution, documentReference);
		documentReferenceToTransmit.addContent().getAttachment().setContentType(contentType)
				.setUrl("urn:uuid:" + resource.getId());

		Bundle bundle = new Bundle().setType(TRANSACTION);
		bundle.addEntry().setResource(documentReferenceToTransmit)
				.setFullUrl("urn:uuid:" + documentReferenceToTransmit.getId()).getRequest()
				.setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.DocumentReference.name());
		bundle.addEntry().setResource(attachmentToTransmit).setFullUrl("urn:uuid:" + attachmentToTransmit.getId())
				.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl(attachmentToTransmit.getResourceType().name());

		return bundle;
	}

	private String getFirstAttachmentContentType(DelegateExecution execution, DocumentReference documentReference)
	{
		List<Attachment> attachments = Stream.of(documentReference).filter(DocumentReference::hasContent)
				.flatMap(dr -> dr.getContent().stream())
				.filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
				.map(DocumentReference.DocumentReferenceContentComponent::getAttachment).filter(Attachment::hasUrl)
				.collect(toList());

		if (attachments.size() < 1)
			throw new IllegalArgumentException("Could not find any attachment with url in DocumentReference with id '"
					+ documentReference.getId() + "' belonging to task with id '"
					+ getLeadingTaskFromExecutionVariables(execution).getId() + "'");

		if (attachments.size() > 1)
			logger.warn(
					"Found {} attachments in DocumentReference with id '{}' belonging to task with id '{}', using first ({})",
					attachments.size(), documentReference.getId(),
					getLeadingTaskFromExecutionVariables(execution).getId(), attachments.get(0));

		return attachments.get(0).getContentType();
	}
}
