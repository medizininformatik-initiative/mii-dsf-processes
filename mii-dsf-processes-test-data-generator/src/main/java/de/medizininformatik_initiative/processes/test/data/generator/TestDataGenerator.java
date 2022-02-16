package de.medizininformatik_initiative.processes.test.data.generator;

import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.processes.test.data.generator.CertificateGenerator.CertificateFiles;
import de.rwh.utils.crypto.CertificateAuthority;

public class TestDataGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

	private static final CertificateGenerator certificateGenerator = new CertificateGenerator();
	private static final BundleGenerator bundleGenerator = new BundleGenerator();
	private static final RsaKeyPairGenerator rsaKeyPairGenerator = new RsaKeyPairGenerator();
	private static final EnvGenerator envGenerator = new EnvGenerator();

	static
	{
		CertificateAuthority.registerBouncyCastleProvider();
	}

	public static void main(String[] args)
	{
		certificateGenerator.generateCertificates();

		certificateGenerator.copyDockerTestClientCerts();
		certificateGenerator.copyDockerTestServerCert();

		Map<String, CertificateFiles> clientCertificateFilesByCommonName = certificateGenerator
				.getClientCertificateFilesByCommonName();

		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");
		Path p12File = certificateGenerator.createP12(webbrowserTestUser);
		logger.warn(
				"Install client-certificate and CA certificate from \"{}\" into your browsers certificate store to access fhir and bpe servers with your webbrowser",
				p12File.toAbsolutePath().toString());

		bundleGenerator.createDockerTestBundles(clientCertificateFilesByCommonName);
		bundleGenerator.copyDockerTestBundles();

		rsaKeyPairGenerator.createRsaKeyPair();
		rsaKeyPairGenerator.copyDockerTestRsaKeyPair();

		envGenerator.generateAndWriteDockerTestFhirEnvFiles(clientCertificateFilesByCommonName);
	}
}
