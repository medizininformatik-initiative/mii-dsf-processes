package de.medizininformatik_initiative.process.projectathon.data_sharing.spring.config;

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
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.KeyProviderImpl;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendExecuteDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendMergeDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendMergedDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.PrepareCoordination;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.SelectCosTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.SelectDicTargets;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.CreateDataSetBundle;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.DeleteDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.EncryptDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.PrepareExecution;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.ReadDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.SelectDataSetTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.StoreDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.ValidateDataSetExecute;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.DecryptDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.DownloadDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.InsertDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.SelectHrpTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.StoreCorrelationKeys;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.ValidateDataSetMerge;
import de.medizininformatik_initiative.process.projectathon.data_sharing.util.MimeTypeHelper;
import de.medizininformatik_initiative.processes.kds.client.spring.config.PropertiesConfig;

@Configuration
@ComponentScan(basePackages = "de.medizininformatik_initiative")
public class DataSharingConfig
{
	@Autowired
	private FhirWebserviceClientProvider clientProvider;

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

	// COORDINATE DATA SHARING PROCESS

	@Bean
	public PrepareCoordination prepareCoordination()
	{
		return new PrepareCoordination(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SelectDicTargets selectDicTargets()
	{
		return new SelectDicTargets(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SelectCosTarget selectCosTarget()
	{
		return new SelectCosTarget(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SendMergeDataSharing sendMergeDataSharing()
	{
		return new SendMergeDataSharing(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	@Bean
	public SendExecuteDataSharing sendExecuteDataSharing()
	{
		return new SendExecuteDataSharing(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	// EXECUTE DATA SHARING PROCESS

	@Bean
	public PrepareExecution prepareExecution()
	{
		return new PrepareExecution(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SelectDataSetTarget selectDataSetTarget()
	{
		return new SelectDataSetTarget(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public ReadDataSet readDataSet()
	{
		return new ReadDataSet(clientProvider, taskHelper, readAccessHelper, kdsFhirClientConfig.kdsClientFactory());
	}

	@Bean
	public MimeTypeHelper mimeTypeHelper()
	{
		return new MimeTypeHelper(fhirContext);
	}

	@Bean
	public ValidateDataSetExecute validateDataSetExecute()
	{
		return new ValidateDataSetExecute(clientProvider, taskHelper, readAccessHelper, mimeTypeHelper());
	}

	@Bean
	public CreateDataSetBundle createDataSetBundle()
	{
		return new CreateDataSetBundle(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public EncryptDataSet encryptDataSet()
	{
		return new EncryptDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider);
	}

	@Bean
	public StoreDataSet storeDataSet()
	{
		return new StoreDataSet(clientProvider, taskHelper, readAccessHelper, kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public SendDataSet sendDataSet()
	{
		return new SendDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Bean
	public DeleteDataSet deleteDataSet()
	{
		return new DeleteDataSet(clientProvider, taskHelper, readAccessHelper);
	}

	// MERGE DATA SHARING PROCESS

	@Bean
	public StoreCorrelationKeys storeCorrelationKeys()
	{
		return new StoreCorrelationKeys(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public DownloadDataSet downloadDataSet()
	{
		return new DownloadDataSet(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public KeyProvider keyProvider()
	{
		return KeyProviderImpl.fromFiles(cosPrivateKeyFile, cosPublicKeyFile, clientProvider, organizationProvider,
				readAccessHelper, kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public DecryptDataSet decryptDataSet()
	{
		return new DecryptDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider, keyProvider(),
				kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public ValidateDataSetMerge validateDataSetMerge()
	{
		return new ValidateDataSetMerge(clientProvider, taskHelper, readAccessHelper, mimeTypeHelper());
	}

	@Bean
	public InsertDataSet insertDataSet()
	{
		return new InsertDataSet(clientProvider, taskHelper, readAccessHelper, fhirContext,
				kdsFhirClientConfig.kdsClientFactory());
	}

	@Bean
	public SelectHrpTarget selectHrpTarget()
	{
		return new SelectHrpTarget(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SendMergedDataSet sendMergedDataSet()
	{
		return new SendMergedDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}
}
