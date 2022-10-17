package de.medizininformatik_initiative.process.projectathon.data_transfer.spring.config;

import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.tools.generator.ProcessDocumentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_transfer.crypto.KeyProviderImpl;
import de.medizininformatik_initiative.process.projectathon.data_transfer.message.StartReceiveProcess;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.CreateBundle;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.DecryptData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.DeleteData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.DownloadData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.EncryptData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.InsertData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.ReadData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.StoreData;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.ValidateDataCos;
import de.medizininformatik_initiative.process.projectathon.data_transfer.service.ValidateDataDic;
import de.medizininformatik_initiative.process.projectathon.data_transfer.util.MimeTypeHelper;
import de.medizininformatik_initiative.processes.kds.client.spring.config.PropertiesConfig;

@Configuration
@ComponentScan(basePackages = "de.medizininformatik_initiative")
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

	@Autowired
	private PropertiesConfig kdsFhirClientConfig;

	@ProcessDocumentation(required = true, processNames = {
			"medizininformatik-initiativede_dataReceive" }, description = "Location of the COS private-key as 4096 Bit RSA PEM encoded, not encrypted file", recommendation = "Use docker secret file to configure", example = "/run/secrets/cos_private_key.pem")
	@Value("${de.medizininformatik.initiative.cos.private.key:#{null}}")
	private String cosPrivateKeyFile;

	@ProcessDocumentation(required = true, processNames = {
			"medizininformatik-initiativede_dataReceive" }, description = "Location of the COS public-key as 4096 Bit RSA PEM encoded file", recommendation = "Use docker secret file to configure", example = "/run/secrets/cos_public_key.pem")
	@Value("${de.medizininformatik.initiative.cos.public.key:#{null}}")
	private String cosPublicKeyFile;

	@Bean
	public MimeTypeHelper mimeTypeHelper()
	{
		return new MimeTypeHelper(fhirContext);
	}

	// projectathonDataSend

	@Bean
	public ReadData readData()
	{
		return new ReadData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext,
				kdsFhirClientConfig.kdsClientFactory());
	}

	@Bean
	public ValidateDataDic validateDataDic()
	{
		return new ValidateDataDic(fhirClientProvider, taskHelper, readAccessHelper, mimeTypeHelper());
	}

	@Bean
	public CreateBundle createBundle()
	{
		return new CreateBundle(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
				kdsFhirClientConfig.dataLogger());
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
		return new StoreData(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, endpointProvider,
				kdsFhirClientConfig.dataLogger());
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
				readAccessHelper, kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public DecryptData decryptData()
	{
		return new DecryptData(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, keyProvider(),
				kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public ValidateDataCos validateDataCos()
	{
		return new ValidateDataCos(fhirClientProvider, taskHelper, readAccessHelper, mimeTypeHelper());
	}

	@Bean
	public InsertData insertData()
	{
		return new InsertData(fhirClientProvider, taskHelper, readAccessHelper, fhirContext,
				kdsFhirClientConfig.kdsClientFactory());
	}
}
