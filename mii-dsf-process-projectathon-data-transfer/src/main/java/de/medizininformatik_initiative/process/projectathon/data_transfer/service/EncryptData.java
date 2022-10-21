package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static java.util.stream.Collectors.toList;

import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_transfer.crypto.RsaAesGcmUtil;

public class EncryptData extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(EncryptData.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public EncryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
			EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String coordinatingSiteIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER);
		Bundle toEncrypt = (Bundle) execution.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET);
		String localOrganizationIdentifier = organizationProvider.getLocalIdentifierValue();

		try
		{
			PublicKey publicKey = readPublicKey(coordinatingSiteIdentifier);
			byte[] encrypted = encrypt(execution, publicKey, toEncrypt, localOrganizationIdentifier,
					coordinatingSiteIdentifier);

			execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED,
					Variables.byteArrayValue(encrypted));
		}
		catch (Exception exception)
		{
			logger.warn(
					"Could not encrypt data-set for COS '{}' and data-transfer project '{}' referenced in Task with id '{}' - {}",
					coordinatingSiteIdentifier, projectIdentifier, task.getId(), exception.getMessage());
			throw new RuntimeException("Could not encrypt data-set for COS '" + coordinatingSiteIdentifier
					+ "' and data-transfer project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "'", exception);
		}
	}

	private PublicKey readPublicKey(String coordinatingSiteIdentifier)
	{
		String url = getEndpointUrl(coordinatingSiteIdentifier);

		Bundle searchBundle = getFhirWebserviceClientProvider().getWebserviceClient(url)
				.withRetry(ConstantsDataTransfer.DSF_CLIENT_RETRY_6_TIMES,
						ConstantsDataTransfer.DSF_CLIENT_RETRY_INTERVAL_5MIN)
				.search(Bundle.class,
						Map.of("identifier", Collections.singletonList(ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY
								+ "|" + ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY)));

		if (searchBundle.getTotal() != 1 && searchBundle.getEntryFirstRep().getResource() instanceof Bundle)
			throw new IllegalStateException("Could not find PublicKey Bundle of organization with identifier'"
					+ coordinatingSiteIdentifier + "'");

		Bundle publicKeyBundle = (Bundle) searchBundle.getEntryFirstRep().getResource();

		logger.debug("Downloaded PublicKey Bundle from organization with identifier '{}'", coordinatingSiteIdentifier);

		DocumentReference documentReference = getDocumentReference(publicKeyBundle);
		Binary binary = getBinary(publicKeyBundle);

		PublicKey publicKey = getPublicKey(binary, publicKeyBundle.getId());

		checkHash(documentReference, publicKey);

		return publicKey;
	}

	private String getEndpointUrl(String identifier)
	{
		return endpointProvider.getFirstConsortiumEndpointAdress(
				NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				CODESYSTEM_HIGHMED_ORGANIZATION_ROLE, CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS, identifier)
				.orElseThrow(() -> new RuntimeException(
						"Could not find Endpoint for organization with identifier '" + identifier + "'"));
	}

	private DocumentReference getDocumentReference(Bundle bundle)
	{
		List<DocumentReference> documentReferences = bundle.getEntry().stream()
				.map(Bundle.BundleEntryComponent::getResource).filter(r -> r instanceof DocumentReference)
				.map(r -> ((DocumentReference) r)).collect(toList());

		if (documentReferences.size() < 1)
			throw new IllegalArgumentException("Could not find any DocumentReference in PublicKey Bundle");

		if (documentReferences.size() > 1)
			logger.warn("Found {} DocumentReferences in PublicKey Bundle, using the first", documentReferences.size());

		return documentReferences.get(0);
	}

	private Binary getBinary(Bundle bundle)
	{
		List<Binary> binaries = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
				.filter(r -> r instanceof Binary).map(b -> ((Binary) b)).collect(toList());

		if (binaries.size() < 1)
			throw new IllegalArgumentException("Could not find any Binary in PublicKey Bundle");

		if (binaries.size() > 1)
			logger.warn("Found {} Binaries in PublicKey Bundle, using the first", binaries.size());

		return binaries.get(0);
	}

	private PublicKey getPublicKey(Binary binary, String publicKeyBundleId)
	{
		try
		{
			return KeyProvider.fromBytes(binary.getContent());
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Could not read PublicKey from Binary in PublicKey Bundle", exception);
		}
	}

	private void checkHash(DocumentReference documentReference, PublicKey publicKey)
	{
		long numberOfHashes = documentReference.getContent().stream()
				.filter(DocumentReference.DocumentReferenceContentComponent::hasAttachment)
				.map(DocumentReference.DocumentReferenceContentComponent::getAttachment).filter(Attachment::hasHash)
				.map(Attachment::getHash).count();

		if (numberOfHashes < 1)
			throw new RuntimeException("Could not find any sha256-hash in DocumentReference");

		if (numberOfHashes > 1)
			logger.warn("DocumentReference contains {} sha256-hashes, using the first", numberOfHashes);

		byte[] documentReferenceHash = documentReference.getContentFirstRep().getAttachment().getHash();
		byte[] publicKeyHash = DigestUtils.sha256(publicKey.getEncoded());

		logger.debug("DocumentReference PublicKey sha256-hash '{}'", Hex.encodeHexString(documentReferenceHash));
		logger.debug("PublicKey actual sha256-hash '{}'", Hex.encodeHexString(publicKeyHash));

		if (!Arrays.equals(documentReferenceHash, publicKeyHash))
			throw new RuntimeException(
					"Sha256-hash in DocumentReference does not match computed sha256-hash of Binary");
	}

	private byte[] encrypt(DelegateExecution execution, PublicKey publicKey, Bundle bundle,
			String sendingOrganizationIdentifier, String receivingOrganizationIdentifier)
	{
		try
		{
			byte[] toEncrypt = FhirContext.forR4().newXmlParser().encodeResourceToString(bundle)
					.getBytes(StandardCharsets.UTF_8);

			return RsaAesGcmUtil.encrypt(publicKey, toEncrypt, sendingOrganizationIdentifier,
					receivingOrganizationIdentifier);
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Could not encrypt data-set to transmit", exception);
		}
	}
}
