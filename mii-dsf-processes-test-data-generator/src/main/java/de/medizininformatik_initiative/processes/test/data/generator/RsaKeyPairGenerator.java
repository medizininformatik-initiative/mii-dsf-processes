package de.medizininformatik_initiative.processes.test.data.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.PemIo;

public class RsaKeyPairGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(RsaKeyPairGenerator.class);

	private static final BouncyCastleProvider PROVIDER = new BouncyCastleProvider();

	private KeyPair pair;

	public void createRsaKeyPair()
	{
		Path cosPrivateKeyFile = Paths.get("rsa/cos_private-key.pem");
		Path cosPublicKeyFile = Paths.get("rsa/cos_public-key.pem");

		if (Files.isReadable(cosPrivateKeyFile) && Files.isReadable(cosPublicKeyFile))
		{
			try
			{
				logger.info("Reading COS private-key from {}", cosPrivateKeyFile.toString());
				PrivateKey privateKey = PemIo.readPrivateKeyFromPem(cosPrivateKeyFile);

				logger.info("Reading COS public-key from {}", cosPublicKeyFile.toString());
				RSAPublicKey publicKey = PemIo.readPublicKeyFromPem(cosPublicKeyFile);

				pair = new KeyPair(publicKey, privateKey);
			}
			catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException | PKCSException e)
			{
				logger.error("Error while reading rsa key-pair from " + cosPrivateKeyFile.toString() + " and "
						+ cosPublicKeyFile.toString(), e);
				throw new RuntimeException(e);
			}
		}
		else
		{
			try
			{
				logger.info("Generating 4096 Bit RSA key-pair");
				pair = CertificateHelper.createRsaKeyPair4096Bit();

				logger.info("Writing COS private-key to {}", cosPrivateKeyFile.toString());
				PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(PROVIDER, cosPrivateKeyFile, pair.getPrivate());

				logger.info("Writing COS public-key to {}", cosPublicKeyFile.toString());
				PemIo.writePublicKeyToPem((RSAPublicKey) pair.getPublic(), cosPublicKeyFile);
			}
			catch (NoSuchAlgorithmException | IOException | OperatorCreationException e)
			{
				logger.error("Error while creating or writing rsa key-pair to " + cosPrivateKeyFile.toString() + " and "
						+ cosPublicKeyFile.toString(), e);
				throw new RuntimeException(e);
			}
		}
	}

	public void copyDockerTestRsaKeyPair()
	{
		Path cosPrivateKeyFile = Paths.get("../mii-dsf-processes-docker-test-setup/secrets/cos_private_key.pem");

		Path cosPublicKeyFile = Paths.get("../mii-dsf-processes-docker-test-setup/secrets/cos_public_key.pem");

		try
		{
			logger.info("Copying COS private-key to {}", cosPrivateKeyFile.toString());
			PemIo.writeNotEncryptedPrivateKeyToOpenSslClassicPem(PROVIDER, cosPrivateKeyFile, pair.getPrivate());

			logger.info("Copying COS public-key to {}", cosPublicKeyFile.toString());
			PemIo.writePublicKeyToPem((RSAPublicKey) pair.getPublic(), cosPublicKeyFile);
		}
		catch (IOException | OperatorCreationException e)
		{
			logger.error("Error copying key-pair", e);
			throw new RuntimeException(e);
		}
	}
}
