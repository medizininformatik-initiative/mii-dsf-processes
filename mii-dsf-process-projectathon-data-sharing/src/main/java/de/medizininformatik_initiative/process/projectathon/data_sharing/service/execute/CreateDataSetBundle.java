package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import static java.util.stream.Collectors.toList;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
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
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class CreateDataSetBundle extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(CreateDataSetBundle.class);

	private final OrganizationProvider organizationProvider;
	private final DataLogger dataLogger;

	public CreateDataSetBundle(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);

		logger.info(
				"Creating transferable data-set for COS '{}' and data-sharing project '{}' referenced in Task with id '{}'",
				cosIdentifier, projectIdentifier, task.getId());

		try
		{
			DocumentReference documentReference = (DocumentReference) execution
					.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE);
			Resource resource = (Resource) execution
					.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_RESOURCE);

			Bundle bundle = createTransactionBundle(projectIdentifier, documentReference, resource);
			dataLogger.logResource("Created data-set Bundle", bundle);

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET,
					FhirResourceValues.create(bundle));
		}
		catch (Exception exception)
		{
			String message = "Could not create transferable data-set for COS '" + cosIdentifier
					+ "' and data-sharing project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "' - " + exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR, message,
					exception);
		}
	}

	private Bundle createTransactionBundle(String projectIdentifier, DocumentReference documentReference,
			Resource resource)
	{
		Resource attachmentToTransmit = resource.setId(UUID.randomUUID().toString());

		DocumentReference documentReferenceToTransmit = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReferenceToTransmit.setId(UUID.randomUUID().toString());
		documentReferenceToTransmit.getMasterIdentifier()
				.setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER).setValue(projectIdentifier);
		documentReferenceToTransmit.addAuthor().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
				.setValue(organizationProvider.getLocalIdentifierValue());
		documentReferenceToTransmit.setDate(documentReference.getDate());

		String contentType = getFirstAttachmentContentType(documentReference, projectIdentifier);
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

	private String getFirstAttachmentContentType(DocumentReference documentReference, String projectIdentifier)
	{
		List<Attachment> attachments = Stream.of(documentReference).filter(DocumentReference::hasContent)
				.flatMap(dr -> dr.getContent().stream())
				.filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
				.map(DocumentReference.DocumentReferenceContentComponent::getAttachment).filter(Attachment::hasUrl)
				.collect(toList());

		if (attachments.size() < 1)
			throw new IllegalArgumentException(
					"Could not find any attachment in DocumentReference with masterIdentifier '" + projectIdentifier
							+ "' stored on KDS FHIR server");

		return attachments.get(0).getContentType();
	}
}
