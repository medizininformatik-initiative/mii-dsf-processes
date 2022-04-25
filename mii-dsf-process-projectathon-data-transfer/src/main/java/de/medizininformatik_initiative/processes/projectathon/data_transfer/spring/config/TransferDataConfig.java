package de.medizininformatik_initiative.processes.projectathon.data_transfer.spring.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.tools.generator.ProcessDocumentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.KdsClientFactory;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.fhir.KdsFhirClient;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto.KeyProvider;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto.KeyProviderImpl;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.message.StartReceiveProcess;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.CreateBundle;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.DecryptData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.DeleteData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.DownloadData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.EncryptData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.InsertData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.ReadData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.StoreData;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.ValidateDataCos;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.service.ValidateDataDic;

@Configuration
public class TransferDataConfig
{
	@Autowired
	private FhirWebserviceClientProvider fhirClientProvider;

	@Autowired
	private TaskHelper taskHelper;

	@Autowired
	private ReadAccessHelper readAccessHelper;

	@Autowired
	private OrganizationProvider organizationProvider;

	@Autowired
	private EndpointProvider endpointProvider;

	@Autowired
	private FhirContext fhirContext;

	@ProcessDocumentation(required = true, description = "The base address of the KDS FHIR server to read/store FHIR resources", example = "http://foo.bar/fhir")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.base.url:#{null}}")
	private String fhirStoreBaseUrl;

	@ProcessDocumentation(description = "Client implementation used to connect to the KDS FHIR server in order to read/store FHIR resources")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.client:de.medizininformatik_initiative.processes.projectathon.data_transfer.client.fhir.KdsFhirClientImpl}")
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

	@ProcessDocumentation(required = true, processNames = {
			"medizininformatik-initiativede_dataReceive" }, description = "Location of the COS private-key as 4096 Bit RSA PEM encoded, not encrypted file", recommendation = "Use docker secret file to configure", example = "/run/secrets/cos_private_key.pem")
	@Value("${de.medizininformatik.initiative.cos.private.key:#{null}}")
	private String cosPrivateKeyFile;

	@ProcessDocumentation(required = true, processNames = {
			"medizininformatik-initiativede_dataReceive" }, description = "Location of the COS public-key as 4096 Bit RSA PEM encoded file", recommendation = "Use docker secret file to configure", example = "/run/secrets/cos_public_key.pem")
	@Value("${de.medizininformatik.initiative.cos.public.key:#{null}}")
	private String cosPublicKeyFile;

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
					(Class<KdsFhirClient>) Class.forName(fhirStoreClientClass), localIdentifierValue);
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

	// projectathonDataSend

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, kdsClientFactory());
	}

	@Bean
	public ValidateDataDic validateDataDic()
	{
		return new ValidateDataDic(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider);
	}

	@Bean
	public CreateBundle createBundle()
	{
		return new CreateBundle(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider);
	}

	@Bean
	public EncryptData encryptData()
	{
		return new EncryptData(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public StoreData storeData()
	{
		return new StoreData(fhirClientProvider, taskHelper, readAccessHelper, endpointProvider);
	}

	@Bean
	public StartReceiveProcess startReceiveProcess()
	{
		return new StartReceiveProcess(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	@Bean
	public DeleteData deleteData()
	{
		return new DeleteData(fhirClientProvider, taskHelper, readAccessHelper);
	}

	// projectathonDataReceive

	@Bean
	public DownloadData downloadData()
	{
		return new DownloadData(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public KeyProvider keyProvider()
	{
		return KeyProviderImpl.fromFiles(cosPrivateKeyFile, cosPublicKeyFile, fhirClientProvider, organizationProvider,
				readAccessHelper);
	}

	@Bean
	public DecryptData decryptData()
	{
		return new DecryptData(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, keyProvider());
	}

	@Bean
	public ValidateDataCos validateDataCos()
	{
		return new ValidateDataCos(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public InsertData insertData()
	{
		return new InsertData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, kdsClientFactory());
	}
}
