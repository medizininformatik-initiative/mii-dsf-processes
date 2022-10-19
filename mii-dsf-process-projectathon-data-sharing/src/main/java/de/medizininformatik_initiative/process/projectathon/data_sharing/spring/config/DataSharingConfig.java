package de.medizininformatik_initiative.process.projectathon.data_sharing.spring.config;

import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.questionnaire.QuestionnaireResponseHelper;
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
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendInitializeNewProjectDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendMergeDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendMergedDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendReceivedDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.questionnaire.ReleaseDataSetListener;
import de.medizininformatik_initiative.process.projectathon.data_sharing.questionnaire.ReleaseMergedDataSetListener;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.CommunicateMissingDataSetsCoordinate;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.CommunicateReceivedDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.ExtractMergedDataSetLocation;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.PrepareCoordination;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.SelectCosTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.SelectDicTargets;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.CheckQuestionnaireDataSetReleaseInput;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.CreateDataSetBundle;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.DeleteDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.EncryptDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.HandleErrorExecute;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.PrepareExecution;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.ReadDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.SelectDataSetTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.StoreDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.ValidateDataSetExecute;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.CheckQuestionnaireMergedDataSetReleaseInput;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.CommunicateMissingDataSetsMerge;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.DecryptDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.DownloadDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.HandleErrorMergeReceive;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.HandleErrorMergeRelease;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.InsertDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.PrepareMerging;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.SelectHrpTarget;
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
	private QuestionnaireResponseHelper questionnaireResponseHelper;

	@Autowired
	private ReadAccessHelper readAccessHelper;

	@Autowired
	private OrganizationProvider organizationProvider;

	@Autowired
	private EndpointProvider endpointProvider;

	@Autowired
	private MailService mailService;

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

	@Bean
	public CommunicateReceivedDataSet communicateReceivedDataSet()
	{
		return new CommunicateReceivedDataSet(clientProvider, taskHelper, readAccessHelper, mailService);
	}

	@Bean
	public CommunicateMissingDataSetsCoordinate communicateMissingDataSetsCoordinate()
	{
		return new CommunicateMissingDataSetsCoordinate(clientProvider, taskHelper, readAccessHelper, mailService);
	}

	@Bean
	public ExtractMergedDataSetLocation extractMergedDataSetLocation()
	{
		return new ExtractMergedDataSetLocation(clientProvider, taskHelper, readAccessHelper);
	}

	// EXECUTE DATA SHARING PROCESS

	@Bean
	public PrepareExecution prepareExecution()
	{
		return new PrepareExecution(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public ReleaseDataSetListener releaseDataSetListener()
	{
		return new ReleaseDataSetListener(clientProvider, organizationProvider, questionnaireResponseHelper, taskHelper,
				readAccessHelper, kdsFhirClientConfig.kdsClientFactory(), mailService);
	}

	@Bean
	public HandleErrorExecute handleErrorExecute()
	{
		return new HandleErrorExecute(clientProvider, taskHelper, readAccessHelper, mailService);
	}

	@Bean
	public CheckQuestionnaireDataSetReleaseInput checkQuestionnaireDataSetReleaseInput()
	{
		return new CheckQuestionnaireDataSetReleaseInput(clientProvider, taskHelper, readAccessHelper);
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
		return new StoreDataSet(clientProvider, taskHelper, organizationProvider, readAccessHelper,
				kdsFhirClientConfig.dataLogger(), mailService);
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
	public PrepareMerging prepareMerging()
	{
		return new PrepareMerging(clientProvider, taskHelper, readAccessHelper, endpointProvider);
	}

	@Bean
	public SendInitializeNewProjectDataSharing sendInitializeNewProjectDataSharing()
	{
		return new SendInitializeNewProjectDataSharing(clientProvider, taskHelper, readAccessHelper,
				organizationProvider, fhirContext, kdsFhirClientConfig.kdsClientFactory());
	}

	@Bean
	public HandleErrorMergeReceive handleErrorMergeReceive()
	{
		return new HandleErrorMergeReceive(clientProvider, taskHelper, readAccessHelper, mailService);
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
				kdsFhirClientConfig.kdsClientFactory(), mailService);
	}

	@Bean
	public SendReceivedDataSet sendReceivedDataSet()
	{
		return new SendReceivedDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Bean
	public HandleErrorMergeRelease handleErrorMergeRelease()
	{
		return new HandleErrorMergeRelease(clientProvider, taskHelper, readAccessHelper, mailService);
	}

	@Bean
	public CommunicateMissingDataSetsMerge communicateMissingDataSetsMerge()
	{
		return new CommunicateMissingDataSetsMerge(clientProvider, taskHelper, readAccessHelper, mailService);
	}

	@Bean
	public ReleaseMergedDataSetListener releaseMergedDataSetListener()
	{
		return new ReleaseMergedDataSetListener(clientProvider, organizationProvider, questionnaireResponseHelper,
				taskHelper, readAccessHelper, mailService);
	}

	@Bean
	public CheckQuestionnaireMergedDataSetReleaseInput checkQuestionnaireMergedDataSetReleaseInput()
	{
		return new CheckQuestionnaireMergedDataSetReleaseInput(clientProvider, taskHelper, readAccessHelper);
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
