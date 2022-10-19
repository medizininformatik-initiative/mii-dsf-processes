package de.medizininformatik_initiative.process.kds.report.spring.config;

import org.highmed.dsf.bpe.service.MailService;
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
import de.medizininformatik_initiative.process.kds.report.message.SendKdsReport;
import de.medizininformatik_initiative.process.kds.report.message.SendReceipt;
import de.medizininformatik_initiative.process.kds.report.message.StartSendKdsReport;
import de.medizininformatik_initiative.process.kds.report.service.CheckSearchBundle;
import de.medizininformatik_initiative.process.kds.report.service.CreateKdsReport;
import de.medizininformatik_initiative.process.kds.report.service.DownloadKdsReport;
import de.medizininformatik_initiative.process.kds.report.service.DownloadSearchBundle;
import de.medizininformatik_initiative.process.kds.report.service.InsertKdsReport;
import de.medizininformatik_initiative.process.kds.report.service.SelectTargetDic;
import de.medizininformatik_initiative.process.kds.report.service.SelectTargetHrp;
import de.medizininformatik_initiative.process.kds.report.service.StartTimer;
import de.medizininformatik_initiative.process.kds.report.service.StoreReceipt;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;
import de.medizininformatik_initiative.processes.kds.client.spring.config.PropertiesConfig;

@Configuration
@ComponentScan(basePackages = "de.medizininformatik_initiative")
public class KdsReportConfig
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

	@Autowired
	private MailService mailService;

	@ProcessDocumentation(processNames = {
			"medizininformatik-initiativede_kdsReportSend" }, description = "The KDS FHIR server type, possible values are [blaze, other] ; must be set, if a Blaze server is used")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.type:other}")
	private String fhirStoreType;

	// kdsReportAutostart Process

	@Bean
	public StartTimer startTimer()
	{
		return new StartTimer(clientProvider, taskHelper, readAccessHelper, organizationProvider, endpointProvider);
	}

	@Bean
	public StartSendKdsReport startSendKdsReport()
	{
		return new StartSendKdsReport(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	// kdsReportSend Process

	@Bean
	public SelectTargetHrp selectTargetHrp()
	{
		return new SelectTargetHrp(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public DownloadSearchBundle downloadSearchBundle()
	{
		return new DownloadSearchBundle(clientProvider, taskHelper, readAccessHelper, kdsReportStatusGenerator(),
				kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public CheckSearchBundle checkSearchBundle()
	{
		return new CheckSearchBundle(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public CreateKdsReport createKdsReport()
	{
		return new CreateKdsReport(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				kdsFhirClientConfig.kdsClientFactory(), fhirStoreType, kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public KdsReportStatusGenerator kdsReportStatusGenerator()
	{
		return new KdsReportStatusGenerator();
	}

	@Bean
	public SendKdsReport sendKdsReport()
	{
		return new SendKdsReport(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext,
				kdsReportStatusGenerator());
	}

	@Bean
	public StoreReceipt storeReceipt()
	{
		return new StoreReceipt(clientProvider, taskHelper, readAccessHelper, kdsReportStatusGenerator(), mailService);
	}

	// kdsReportReceive Process

	@Bean
	public DownloadKdsReport downloadKdsReport()
	{
		return new DownloadKdsReport(clientProvider, taskHelper, readAccessHelper, kdsReportStatusGenerator());
	}

	@Bean
	public InsertKdsReport insertKdsReport()
	{
		return new InsertKdsReport(clientProvider, taskHelper, readAccessHelper, kdsReportStatusGenerator(),
				mailService);
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
				kdsReportStatusGenerator());
	}
}
