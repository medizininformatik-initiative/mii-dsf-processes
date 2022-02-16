package de.medizininformatik_initiative.processes.projectathon.data_transfer.spring.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.documentation.generator.Documentation;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.KdsClientFactory;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.fhir.KdsFhirClient;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto.KeyProvider;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto.KeyProviderImpl;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.message.StartReceiveProcess;
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

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_BASE_URL", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = true, description = "The base address of the KDS FHIR server to read/store FHIR resources", recommendation = "None", example = "http://foo.bar/fhir")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.base.url:#{null}}")
	private String fhirStoreBaseUrl;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_CLIENT", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Client implementation used to connect to the KDS FHIR server in order to read/store FHIR resources", recommendation = "Use default value", example = "None")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.client:de.medizininformatik_initiative.processes.projectathon.data_transfer.client.fhir.KdsFhirClientImpl}")
	private String fhirStoreClientClass;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_TRUST_CERTIFICATES", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "PEM encoded file with one or more trusted root certificate to validate the KDS FHIR server certificate when connecting via https", recommendation = "Use docker secret file to configure", example = "/run/secrets/hospital_ca.pem")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.trust.certificates:#{null}}")
	private String fhirStoreTrustStore;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_CERTIFICATE", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "PEM encoded file with client-certificate, if KDS FHIR server requires mutual TLS authentication", recommendation = "Use docker secret file to configure", example = "/run/secrets/kds_server_client_certificate.pem")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.certificate:#{null}}")
	private String fhirStoreCertificate;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PRIVATE_KEY", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Private key corresponding to the KDS FHIR server client-certificate as PEM encoded file. Use *DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PRIVATE_KEY_PASSWORD* or *DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PRIVATE_KEY_PASSWORD_FILE* if private key is encrypted", recommendation = "Use docker secret file to configure", example = "/run/secrets/kds_server_private_key.pem")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.private.key:#{null}}")
	private String fhirStorePrivateKey;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PRIVATE_KEY_PASSWORD or DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PRIVATE_KEY_PASSWORD_FILE", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Password to decrypt the KDS FHIR server client-certificate encrypted private key", recommendation = "Use docker secret file to configure by using *DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PRIVATE_KEY_PASSWORD_FILE*. **Caution!** Editors like nano will add a `LF` (hex `0A`) character at the end of the last line. Make sure that the password file does not end with the `LF` character. For example by starting nano with `nano -L file.password`. If you want to check that the file does not end with an `LF` (hex `0A`) character, use `xxd file.password` to look at a hexdump.", example = "/run/secrets/kds_server_private_key.pem.password")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.private.key.password:#{null}}")
	private char[] fhirStorePrivateKeyPassword;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_BASICAUTH_USERNAME", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Basic authentication username, set if the server containing the FHIR KDS data requests authentication using basic auth", recommendation = "None", example = "None")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.basicauth.username:#{null}}")
	private String fhirStoreUsername;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_BASICAUTH_PASSWORD or DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_BASICAUTH_PASSWORD_FILE", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Basic authentication password, set if the server containing the FHIR KDS data requests authentication using basic auth", recommendation = "Use docker secret file to configure by using *DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_BASICAUTH_PASSWORD_FILE*. **Caution!** Editors like nano will add a `LF` (hex `0A`) character at the end of the last line. Make sure that the password file does not end with the `LF` character. For example by starting nano with `nano -L file.password`. If you want to check that the file does not end with an `LF` (hex `0A`) character, use `xxd file.password` to look at a hexdump.", example = "/run/secrets/kds_server_basicauth.password")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.basicauth.password:#{null}}")
	private String fhirStorePassword;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_BEARER_TOKEN", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Bearer token for authentication, set if the server containing the FHIR KDS data requests authentication using a bearer token, cannot be set using docker secrets", recommendation = "None", example = "None")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.bearer.token:#{null}}")
	private String fhirStoreBearerToken;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_TIMEOUT_CONNECT", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "The timeout in milliseconds until a connection is established between the KDS client and the KDS FHIR server", recommendation = "Change default value only if timeout exceptions occur", example = "See default value")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.connect:10000}")
	private int fhirStoreConnectTimeout;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_TIMEOUT_CONNECTION_REQUEST", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "The timeout in milliseconds used when requesting a connection from the connection manager between the KDS client and the KDS FHIR server", recommendation = "Change default value only if timeout exceptions occur", example = "See default value")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.connection.request:10000}")
	private int fhirStoreConnectionRequestTimeout;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_TIMEOUT_SOCKET", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Maximum period of inactivity in milliseconds between two consecutive data packets of the KDS client and the KDS FHIR server", recommendation = "Change default value only if timeout exceptions occur", example = "See default value")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.socket:10000}")
	private int fhirStoreSocketTimeout;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_CLIENT_VERBOSE", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "The KDS client will log additional debug output", recommendation = "Change default value only if exceptions occur", example = "See default value")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.client.verbose:false}")
	private boolean fhirStoreHapiClientVerbose;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PROXY_URL", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Proxy location, set if the server containing the FHIR KDS data can only be reached through a proxy", recommendation = "None", example = "http://proxy.foo:8080")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.url:#{null}}")
	private String fhirStoreProxyUrl;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PROXY_USERNAME", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Proxy username, set if the server containing the FHIR KDS data can only be reached through a proxy which requests authentication", recommendation = "None", example = "None")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.username:#{null}}")
	private String fhirStoreProxyUsername;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PROXY_PASSWORD or DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PROXY_PASSWORD_FILE", processNames = "medizininformatik-initiativede_dataSend/, medizininformatik-initiativede_dataReceive/", required = false, description = "Proxy password, set if the server containing the FHIR KDS data can only be reached through a proxy which requests authentication", recommendation = "Use docker secret file to configure by using *DE_MEDIZININFORMATIK_INITIATIVE_KDS_FHIR_SERVER_PROXY_PASSWORD_FILE*. **Caution!** Editors like nano will add a `LF` (hex `0A`) character at the end of the last line. Make sure that the password file does not end with the `LF` character. For example by starting nano with `nano -L file.password`. If you want to check that the file does not end with an `LF` (hex `0A`) character, use `xxd file.password` to look at a hexdump.", example = "None")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.password:#{null}}")
	private String fhirStoreProxyPassword;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_COS_PRIVATE_KEY", processNames = "medizininformatik-initiativede_dataReceive/", required = true, description = "Location of the COS private-key as 4096 Bit RSA PEM encoded, not encrypted file", recommendation = "Use docker secret file to configure", example = "/run/secrets/cos_private_key.pem")
	@Value("${de.medizininformatik.initiative.cos.private.key:#{null}}")
	private String cosPrivateKeyFile;

	@Documentation(environmentVariables = "DE_MEDIZININFORMATIK_INITIATIVE_COS_PUBLIC_KEY", processNames = "medizininformatik-initiativede_dataReceive/", required = true, description = "Location of the COS public-key as 4096 Bit RSA PEM encoded file", recommendation = "Use docker secret file to configure", example = "/run/secrets/cos_public_key.pem")
	@Value("${de.medizininformatik.initiative.cos.public.key:#{null}}")
	private String cosPublicKeyFile;

	@Value("${org.highmed.dsf.bpe.fhir.server.organization.identifier.value}")
	private String localIdentifierValue;

	@Bean
	@SuppressWarnings("unchecked")
	public KdsClientFactory projectathonClientFactory()
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

	// miiProjectathonDataSend

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext, projectathonClientFactory());
	}

	@Bean
	public ValidateDataDic validateDataDic()
	{
		return new ValidateDataDic(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider);
	}

	@Bean
	public EncryptData encryptData()
	{
		return new EncryptData(fhirClientProvider, taskHelper, readAccessHelper, endpointProvider);
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

	// miiProjectathonDataReceive

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
		return new DecryptData(fhirClientProvider, taskHelper, readAccessHelper, keyProvider());
	}

	@Bean
	public ValidateDataCos validateDataCos()
	{
		return new ValidateDataCos(fhirClientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public InsertData insertData()
	{
		return new InsertData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext,
				projectathonClientFactory());
	}
}
