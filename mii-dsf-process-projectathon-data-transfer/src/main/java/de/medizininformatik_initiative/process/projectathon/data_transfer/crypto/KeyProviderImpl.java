package de.medizininformatik_initiative.process.projectathon.data_transfer.crypto;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.pkcs.PKCSException;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.util.LoggingHelper;
import de.rwh.utils.crypto.io.PemIo;

public class KeyProviderImpl implements KeyProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(KeyProviderImpl.class);

	// openssl genrsa -out keypair.pem 4096
	// openssl rsa -in keypair.pem -pubout -out publickey.crt
	// openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out pkcs8.key

	/**
	 * One or both parameters should be <code>null</code>
	 *
	 * @param privateKeyFile
	 *            not <code>null</code>
	 * @param publicKeyFile
	 *            not <code>null</code>
	 * @param clientProvider
	 *            not <code>null</code>
	 * @return KeyProvider
	 */
	public static KeyProviderImpl fromFiles(String privateKeyFile, String publicKeyFile,
			FhirWebserviceClientProvider clientProvider, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper)
	{
		logger.info("Configuring KeyProvider with private-key from {} and public-key from {}", privateKeyFile,
				publicKeyFile);

		PrivateKey privateKey = null;
		RSAPublicKey publicKey = null;

		try
		{
			if (privateKeyFile != null)
			{
				Path privateKeyPath = Paths.get(privateKeyFile);
				if (!Files.isReadable(privateKeyPath))
					throw new RuntimeException("PrivateKey at " + privateKeyFile + " not readable");

				privateKey = PemIo.readPrivateKeyFromPem(privateKeyPath);
			}
		}
		catch (IOException | PKCSException e)
		{
			throw new RuntimeException("Error while reading PrivateKey from " + privateKeyFile, e);
		}

		try
		{
			if (publicKeyFile != null)
			{
				Path publicKeyPath = Paths.get(publicKeyFile);
				if (!Files.isReadable(publicKeyPath))
					throw new RuntimeException("PublicKey at " + publicKeyFile + " not readable");

				publicKey = PemIo.readPublicKeyFromPem(publicKeyPath);
			}
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
		{
			throw new RuntimeException("Error while reading PublicKey from " + publicKeyFile, e);
		}

		return new KeyProviderImpl(privateKey, publicKey, clientProvider, organizationProvider, readAccessHelper);
	}

	private final PrivateKey privateKey;
	private final PublicKey publicKey;

	private final FhirWebserviceClientProvider clientProvider;
	private final OrganizationProvider organizationProvider;

	private final ReadAccessHelper readAccessHelper;

	public KeyProviderImpl(PrivateKey privateKey, PublicKey publicKey, FhirWebserviceClientProvider clientProvider,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper)
	{
		this.privateKey = privateKey;
		this.publicKey = publicKey;

		this.clientProvider = clientProvider;
		this.organizationProvider = organizationProvider;
		this.readAccessHelper = readAccessHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(clientProvider, "clientProvider");
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(readAccessHelper, "readAccessHelper");
	}

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event)
	{
		try
		{
			if (publicKey != null)
			{
				Bundle output = clientProvider.getLocalWebserviceClient().search(Bundle.class,
						Map.of("identifier", Collections.singletonList(ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY
								+ "|" + ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY)));

				Bundle bundleOnServer = null;

				if (output.getTotal() == 1)
				{
					logger.info("PublicKey Bundle already exists on DSF FHIR server");
					bundleOnServer = ((Bundle) output.getEntryFirstRep().getResource());
				}
				else if (output.getTotal() == 0)
				{
					logger.info("Creating new PublicKey Bundle on DSF FHIR server...");
					Bundle bundleToCreate = createPublicKeyBundle();
					bundleOnServer = clientProvider.getLocalWebserviceClient().createConditionaly(bundleToCreate,
							"identifier=" + ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY + "|"
									+ ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY);
				}
				else
				{
					throw new RuntimeException("Exist " + output.getTotal() + " Bundle with identifier="
							+ ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY + "|"
							+ ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY
							+ ", expected only one");
				}

				logger.info("PublicKey Bundle has id='{}'", bundleOnServer.getId());
			}
		}
		catch (Exception exception)
		{
			logger.warn("Error while creating PublicKey Bundle: {}", exception.getMessage());
			throw new RuntimeException("Error while creating PublicKey Bundle: " + exception.getMessage(), exception);
		}
	}

	private Bundle createPublicKeyBundle()
	{
		Date date = new Date();

		Binary binary = new Binary().setContentType("application/pem-certificate-chain");
		binary.setContent(getPublicKey().getEncoded());
		binary.setId(UUID.randomUUID().toString());

		DocumentReference documentReference = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReference.getMasterIdentifier().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY)
				.setValue(ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY);
		documentReference.addAuthor().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
				.setValue(organizationProvider.getLocalIdentifierValue());
		documentReference.setDate(date);
		documentReference.addContent().getAttachment().setContentType("application/pem-certificate-chain")
				.setUrl("urn:uuid:" + binary.getId()).setHash(DigestUtils.sha256(publicKey.getEncoded()));

		Bundle bundle = new Bundle().setType(COLLECTION);
		bundle.getIdentifier().setSystem(ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY)
				.setValue(ConstantsDataTransfer.CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY);
		bundle.setTimestamp(date);
		bundle.addEntry().setResource(documentReference).setFullUrl("urn:uuid:" + documentReference.getId());
		bundle.addEntry().setResource(binary).setFullUrl("urn:uuid:" + binary.getId());

		readAccessHelper.addAll(bundle);

		LoggingHelper.logDebugBundle("Created Bundle", bundle);

		return bundle;
	}

	@Override
	public PrivateKey getPrivateKey()
	{
		return privateKey;
	}

	@Override
	public PublicKey getPublicKey()
	{
		return publicKey;
	}
}
