package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.util.Objects;
import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class CreateDataSetBundle extends AbstractServiceDelegate implements InitializingBean
{
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
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		DocumentReference documentReference = (DocumentReference) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE);
		Binary binary = (Binary) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_BINARY);
		Bundle bundle = createTransactionBundle(projectIdentifier, documentReference, binary);

		dataLogger.logResource("Created data-set Bundle", bundle);

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET, FhirResourceValues.create(bundle));
	}

	private Bundle createTransactionBundle(String projectIdentifier, DocumentReference documentReference, Binary binary)
	{
		Binary binaryToTransmit = new Binary().setContentType(binary.getContentType());
		binaryToTransmit.setContent(binary.getContent());
		binaryToTransmit.setId(UUID.randomUUID().toString());

		DocumentReference documentReferenceToTransmit = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReferenceToTransmit.getMasterIdentifier()
				.setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER).setValue(projectIdentifier);
		documentReferenceToTransmit.addAuthor().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
				.setValue(organizationProvider.getLocalIdentifierValue());
		documentReferenceToTransmit.setDate(documentReference.getDate());
		documentReferenceToTransmit.addContent().getAttachment().setContentType(binary.getContentType())
				.setUrl("urn:uuid:" + binaryToTransmit.getId());

		Bundle bundle = new Bundle().setType(TRANSACTION);
		bundle.addEntry().setResource(documentReferenceToTransmit)
				.setFullUrl("urn:uuid:" + documentReferenceToTransmit.getId()).getRequest()
				.setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.DocumentReference.name());
		bundle.addEntry().setResource(binaryToTransmit).setFullUrl("urn:uuid:" + binaryToTransmit.getId()).getRequest()
				.setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.Binary.name());


		return bundle;
	}
}
