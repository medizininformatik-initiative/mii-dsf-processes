package de.medizininformatik_initiative.processes.kds.client.spring.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.highmed.dsf.tools.generator.ProcessDocumentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;
import de.medizininformatik_initiative.processes.kds.client.fhir.KdsFhirClient;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

@Configuration
public class PropertiesConfig
{
	@Autowired
	private FhirContext fhirContext;

	@ProcessDocumentation(required = true, description = "The base address of the KDS FHIR server to read/store FHIR resources", example = "http://foo.bar/fhir")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.base.url:#{null}}")
	private String fhirStoreBaseUrl;

	@ProcessDocumentation(description = "Client implementation used to connect to the KDS FHIR server in order to read/store FHIR resources")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.client:de.medizininformatik_initiative.processes.kds.client.fhir.KdsFhirClientImpl}")
	private String fhirStoreClientClass;

	@ProcessDocumentation(description = "PEM encoded file with one or more trusted root certificate to validate the KDS FHIR server certificate when connecting via https", recommendation = "Use docker secret file to configure", example = "/run/secrets/hospital_ca.pem")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.trust.certificates:#{null}}")
	private String fhirStoreTrustStore;

	@ProcessDocumentation(description = "PEM encoded file with client-certificate, if KDS FHIR server requires mutual TLS authentication", recommendation = "Use docker secret file to configure", example = "/run/secrets/kds_server_client_certificate.pem")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.certificate:#{null}}")
	private String fhirStoreCertificate;

	@ProcessDocumentation(description = "Private key corresponding to the KDS FHIR server client-certificate as PEM encoded file. Use *${env_variable}_PASSWORD* or *${env_variable}_PASSWORD_FILE* if private key is encrypted", recommendation = "Use docker secret file to configure", example = "/run/secrets/kds_server_private_key.pem")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.private.key:#{null}}")
	private String fhirStorePrivateKey;

	@ProcessDocumentation(description = "Password to decrypt the KDS FHIR server client-certificate encrypted private key", recommendation = "Use docker secret file to configure by using *${env_variable}_FILE*", example = "/run/secrets/kds_server_private_key.pem.password")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.private.key.password:#{null}}")
	private char[] fhirStorePrivateKeyPassword;

	@ProcessDocumentation(description = "Basic authentication username, set if the server containing the FHIR KDS data requests authentication using basic auth")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.basicauth.username:#{null}}")
	private String fhirStoreUsername;

	@ProcessDocumentation(description = "Basic authentication password, set if the server containing the FHIR KDS data requests authentication using basic auth", recommendation = "Use docker secret file to configure by using *${env_variable}_FILE*", example = "/run/secrets/kds_server_basicauth.password")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.basicauth.password:#{null}}")
	private String fhirStorePassword;

	@ProcessDocumentation(description = "Bearer token for authentication, set if the server containing the FHIR KDS data requests authentication using a bearer token, cannot be set using docker secrets")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.bearer.token:#{null}}")
	private String fhirStoreBearerToken;

	@ProcessDocumentation(description = "The timeout in milliseconds until a connection is established between the KDS client and the KDS FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.connect:10000}")
	private int fhirStoreConnectTimeout;

	@ProcessDocumentation(description = "The timeout in milliseconds used when requesting a connection from the connection manager between the KDS client and the KDS FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.connection.request:10000}")
	private int fhirStoreConnectionRequestTimeout;

	@ProcessDocumentation(description = "Maximum period of inactivity in milliseconds between two consecutive data packets of the KDS client and the KDS FHIR server", recommendation = "Change default value only if timeout exceptions occur")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.socket:10000}")
	private int fhirStoreSocketTimeout;

	@ProcessDocumentation(description = "The KDS client will log additional debug output", recommendation = "Change default value only if exceptions occur")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.client.verbose:false}")
	private boolean fhirStoreHapiClientVerbose;

	@ProcessDocumentation(description = "Proxy location, set if the server containing the FHIR KDS data can only be reached through a proxy", example = "http://proxy.foo:8080")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.url:#{null}}")
	private String fhirStoreProxyUrl;

	@ProcessDocumentation(description = "Proxy username, set if the server containing the FHIR KDS data can only be reached through a proxy which requests authentication")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.username:#{null}}")
	private String fhirStoreProxyUsername;

	@ProcessDocumentation(description = "Proxy password, set if the server containing the FHIR KDS data can only be reached through a proxy which requests authentication", recommendation = "Use docker secret file to configure by using *${env_variable}_FILE*")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.password:#{null}}")
	private String fhirStoreProxyPassword;

	@ProcessDocumentation(description = "To enable debug logging of FHIR resources set to `true`")
	@Value("${de.medizininformatik.initiative.kds.fhir.dataLoggingEnabled:false}")
	private boolean fhirDataLoggingEnabled;

	@Value("${org.highmed.dsf.bpe.fhir.server.organization.identifier.value}")
	private String localIdentifierValue;

	@Bean
	@SuppressWarnings("unchecked")
	public KdsClientFactory kdsClientFactory()
	{
		Path trustStorePath = checkExists(fhirStoreTrustStore);
		Path certificatePath = checkExists(fhirStoreCertificate);
		Path privateKeyPath = checkExists(fhirStorePrivateKey);

		try
		{
			return new KdsClientFactory(trustStorePath, certificatePath, privateKeyPath, fhirStorePrivateKeyPassword,
					fhirStoreConnectTimeout, fhirStoreSocketTimeout, fhirStoreConnectionRequestTimeout,
					fhirStoreBaseUrl, fhirStoreUsername, fhirStorePassword, fhirStoreBearerToken, fhirStoreProxyUrl,
					fhirStoreProxyUsername, fhirStoreProxyPassword, fhirStoreHapiClientVerbose, fhirContext,
					(Class<KdsFhirClient>) Class.forName(fhirStoreClientClass), localIdentifierValue, dataLogger());
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Path checkExists(String file)
	{
		if (file == null)
			return null;
		else
		{
			Path path = Paths.get(file);

			if (!Files.isReadable(path))
				throw new RuntimeException(path.toString() + " not readable");

			return path;
		}
	}

	@Bean
	public DataLogger dataLogger()
	{
		return new DataLogger(fhirDataLoggingEnabled, fhirContext);
	}
}
