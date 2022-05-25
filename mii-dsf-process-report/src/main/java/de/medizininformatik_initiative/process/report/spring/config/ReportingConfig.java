package de.medizininformatik_initiative.process.report.spring.config;

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
import de.medizininformatik_initiative.process.report.message.SendReceipt;
import de.medizininformatik_initiative.process.report.message.SendReport;
import de.medizininformatik_initiative.process.report.message.StartSendReport;
import de.medizininformatik_initiative.process.report.service.CheckSearchBundle;
import de.medizininformatik_initiative.process.report.service.CreateReport;
import de.medizininformatik_initiative.process.report.service.DownloadReport;
import de.medizininformatik_initiative.process.report.service.DownloadSearchBundle;
import de.medizininformatik_initiative.process.report.service.InsertReport;
import de.medizininformatik_initiative.process.report.service.SelectTargetDic;
import de.medizininformatik_initiative.process.report.service.SelectTargetHrp;
import de.medizininformatik_initiative.process.report.service.StartTimer;
import de.medizininformatik_initiative.process.report.service.StopTimer;
import de.medizininformatik_initiative.process.report.service.StoreReceipt;
import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;
import de.medizininformatik_initiative.processes.kds.client.fhir.KdsFhirClient;

@Configuration
public class ReportingConfig
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

	@ProcessDocumentation(processNames = {
			"medizininformatik-initiativede_reportSend" }, description = "The KDS FHIR server type, possible values are [blaze, other] ; must be set, if a Blaze server is used")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.type:other}")
	private String fhirStoreType;

	// Documentation of remaining values in Data Transfer Process
	@Value("${de.medizininformatik.initiative.kds.fhir.server.base.url:#{null}}")
	private String fhirStoreBaseUrl;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.client:de.medizininformatik_initiative.processes.kds.client.fhir.KdsFhirClientImpl}")
	private String fhirStoreClientClass;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.trust.certificates:#{null}}")
	private String fhirStoreTrustStore;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.certificate:#{null}}")
	private String fhirStoreCertificate;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.private.key:#{null}}")
	private String fhirStorePrivateKey;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.private.key.password:#{null}}")
	private char[] fhirStorePrivateKeyPassword;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.basicauth.username:#{null}}")
	private String fhirStoreUsername;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.basicauth.password:#{null}}")
	private String fhirStorePassword;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.bearer.token:#{null}}")
	private String fhirStoreBearerToken;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.connect:10000}")
	private int fhirStoreConnectTimeout;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.connection.request:10000}")
	private int fhirStoreConnectionRequestTimeout;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.timeout.socket:10000}")
	private int fhirStoreSocketTimeout;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.client.verbose:false}")
	private boolean fhirStoreHapiClientVerbose;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.url:#{null}}")
	private String fhirStoreProxyUrl;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.username:#{null}}")
	private String fhirStoreProxyUsername;

	@Value("${de.medizininformatik.initiative.kds.fhir.server.proxy.password:#{null}}")
	private String fhirStoreProxyPassword;

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

	// reportAutostart Process

	@Bean
	public StartTimer startTimer()
	{
		return new StartTimer(clientProvider, taskHelper, readAccessHelper, organizationProvider, endpointProvider);
	}

	@Bean
	public StopTimer stopTimer()
	{
		return new StopTimer(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public StartSendReport startSendReport()
	{
		return new StartSendReport(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	// reportSend Process

	@Bean
	public SelectTargetHrp selectTargetHrp()
	{
		return new SelectTargetHrp(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public DownloadSearchBundle downloadSearchBundle()
	{
		return new DownloadSearchBundle(clientProvider, taskHelper, readAccessHelper, fhirContext);
	}

	@Bean
	public CheckSearchBundle checkSearchBundle()
	{
		return new CheckSearchBundle(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public CreateReport createReport()
	{
		return new CreateReport(clientProvider, taskHelper, readAccessHelper, organizationProvider, kdsClientFactory(),
				fhirContext, fhirStoreType);
	}

	@Bean
	public ReportStatusGenerator reportStatusGenerator()
	{
		return new ReportStatusGenerator();
	}

	@Bean
	public SendReport sendReport()
	{
		return new SendReport(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext,
				reportStatusGenerator());
	}

	@Bean
	public StoreReceipt storeReceipt()
	{
		return new StoreReceipt(clientProvider, taskHelper, readAccessHelper, reportStatusGenerator());
	}

	// reportReceive Process

	@Bean
	public DownloadReport downloadReport()
	{
		return new DownloadReport(clientProvider, taskHelper, readAccessHelper, reportStatusGenerator());
	}

	@Bean
	public InsertReport insertReport()
	{
		return new InsertReport(clientProvider, taskHelper, readAccessHelper, reportStatusGenerator());
	}

	@Bean
	public SelectTargetDic selectTargetDic()
	{
		return new SelectTargetDic(clientProvider, taskHelper, readAccessHelper, endpointProvider);
	}

	@Bean
	public SendReceipt sendReceipt()
	{
		return new SendReceipt(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext,
				reportStatusGenerator());
	}
}
