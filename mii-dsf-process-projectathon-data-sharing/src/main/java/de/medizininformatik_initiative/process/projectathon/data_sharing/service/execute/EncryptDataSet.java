package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.RsaAesGcmUtil;

public class EncryptDataSet extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(EncryptDataSet.class);

	private final OrganizationProvider organizationProvider;

	public EncryptDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.organizationProvider = organizationProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(organizationProvider, "organizationProvider");
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
				"Encrypting transferable data-set for COS '{}' and data-sharing project '{}' referenced in Task with id '{}'",
				cosIdentifier, projectIdentifier, task.getId());

		try
		{
			String cosUrl = ((Target) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET))
					.getEndpointUrl();
			String localIdentifier = organizationProvider.getLocalIdentifierValue();
			Bundle toEncrypt = (Bundle) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET);

			PublicKey publicKey = readPublicKey(cosIdentifier, cosUrl);
			byte[] encrypted = encrypt(execution, publicKey, toEncrypt, localIdentifier, cosIdentifier);

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED,
					Variables.byteArrayValue(encrypted));
		}
		catch (Exception exception)
		{
			String message = "Could not encrypt transferable data-set for COS '" + cosIdentifier
					+ "' and data-sharing project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "' - " + exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR, message,
					exception);
		}
	}

	private PublicKey readPublicKey(String cosIdentifier, String cosUrl)
	{
		Bundle searchBundle = getFhirWebserviceClientProvider().getWebserviceClient(cosUrl)
				.withRetry(ConstantsDataSharing.DSF_CLIENT_RETRY_TIMES,
						ConstantsDataSharing.DSF_CLIENT_RETRY_INTERVAL_5MIN)
				.search(Bundle.class,
						Map.of("identifier", Collections.singletonList(ConstantsDataSharing.CODESYSTEM_CRYPTOGRAPHY
								+ "|" + ConstantsDataSharing.CODESYSTEM_CRYPTOGRAPHY_VALUE_PUBLIC_KEY)));

		if (searchBundle.getTotal() != 1 && searchBundle.getEntryFirstRep().getResource() instanceof Bundle)
			throw new IllegalStateException(
					"Could not find PublicKey Bundle of organization with identifier '" + cosIdentifier + "'");

		Bundle publicKeyBundle = (Bundle) searchBundle.getEntryFirstRep().getResource();

		logger.debug("Downloaded PublicKey Bundle from organization with identifier '{}'", cosIdentifier);

		DocumentReference documentReference = getDocumentReference(publicKeyBundle);
		Binary binary = getBinary(publicKeyBundle);

		PublicKey publicKey = getPublicKey(binary, publicKeyBundle.getId());
		checkHash(documentReference, publicKey);

		return publicKey;
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
