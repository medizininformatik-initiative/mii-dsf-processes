package de.medizininformatik_initiative.processes.kds.client;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;

public class KdsClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(KdsClientFactory.class);

	private final Path trustStorePath;
	private final Path certificatePath;
	private final Path privateKeyPath;
	private final char[] privateKeyPassword;

	private final int connectTimeout;
	private final int socketTimeout;
	private final int connectionRequestTimeout;

	private final String kdsServerBase;
	private final String kdsServerBasicAuthUsername;
	private final String kdsServerBasicAuthPassword;
	private final String kdsServerBearerToken;

	private final String proxyUrl;
	private final String proxyUsername;
	private final String proxyPassword;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;

	private final String localIdentifierValue;

	private final DataLogger dataLogger;

	public KdsClientFactory(Path trustStorePath, Path certificatePath, Path privateKeyPath, char[] privateKeyPassword,
			int connectTimeout, int socketTimeout, int connectionRequestTimeout, String kdsServerBase,
			String kdsServerBasicAuthUsername, String kdsServerBasicAuthPassword, String kdsServerBearerToken,
			String proxyUrl, String proxyUsername, String proxyPassword, boolean hapiClientVerbose,
			FhirContext fhirContext, String localIdentifierValue, DataLogger dataLogger)
	{
		this.trustStorePath = trustStorePath;
		this.certificatePath = certificatePath;
		this.privateKeyPath = privateKeyPath;
		this.privateKeyPassword = privateKeyPassword;

		this.connectTimeout = connectTimeout;
		this.socketTimeout = socketTimeout;
		this.connectionRequestTimeout = connectionRequestTimeout;

		this.kdsServerBase = kdsServerBase;
		this.kdsServerBasicAuthUsername = kdsServerBasicAuthUsername;
		this.kdsServerBasicAuthPassword = kdsServerBasicAuthPassword;
		this.kdsServerBearerToken = kdsServerBearerToken;

		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;

		this.localIdentifierValue = localIdentifierValue;

		this.dataLogger = dataLogger;
	}

	public void testConnection()
	{
		try
		{
			logger.info(
					"Testing connection to KDS FHIR server with {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
							+ " basicAuthUsername {}, basicAuthPassword {}, bearerToken {}, serverBase: {}, proxyUrl {}, proxyUsername {}, proxyPassword {}}",
					trustStorePath, certificatePath, privateKeyPath, privateKeyPassword != null ? "***" : "null",
					kdsServerBasicAuthUsername, kdsServerBasicAuthPassword != null ? "***" : "null",
					kdsServerBearerToken != null ? "***" : "null", kdsServerBase, proxyUrl, proxyUsername,
					proxyPassword != null ? "***" : "null");

			getKdsClient().testConnection();
		}
		catch (Exception e)
		{
			logger.error("Error while testing connection to KDS FHIR server", e);
		}
	}

	public KdsClient getKdsClient()
	{
		if (configured())
			return createKdsClientImpl();
		else
			return createKdsClientStub();
	}

	private boolean configured()
	{
		return kdsServerBase != null && !kdsServerBase.isBlank();
	}

	protected KdsClient createKdsClientStub()
	{
		return new KdsClientStub(fhirContext, localIdentifierValue);
	}

	protected KdsClient createKdsClientImpl()
	{
		KeyStore trustStore = null;
		char[] keyStorePassword = null;
		if (trustStorePath != null)
		{
			logger.debug("Reading trust-store from {}", trustStorePath.toString());
			trustStore = readTrustStore(trustStorePath);
			keyStorePassword = UUID.randomUUID().toString().toCharArray();
		}

		KeyStore keyStore = null;
		if (certificatePath != null && privateKeyPath != null)
		{
			logger.debug("Creating key-store from {} and {} with password {}", certificatePath.toString(),
					privateKeyPath.toString(), keyStorePassword != null ? "***" : "null");
			keyStore = readKeyStore(certificatePath, privateKeyPath, privateKeyPassword, keyStorePassword);
		}

		return new KdsClientImpl(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout, kdsServerBasicAuthUsername, kdsServerBasicAuthPassword, kdsServerBearerToken,
				kdsServerBase, proxyUrl, proxyUsername, proxyPassword, hapiClientVerbose, fhirContext,
				localIdentifierValue, dataLogger);
	}

	private KeyStore readTrustStore(Path trustPath)
	{
		try
		{
			return CertificateReader.allFromCer(trustPath);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore readKeyStore(Path certificatePath, Path keyPath, char[] keyPassword, char[] keyStorePassword)
	{
		try
		{
			PrivateKey privateKey = PemIo.readPrivateKeyFromPem(keyPath, keyPassword);
			X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);

			return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate },
					UUID.randomUUID().toString(), keyStorePassword);
		}
		catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}
}
