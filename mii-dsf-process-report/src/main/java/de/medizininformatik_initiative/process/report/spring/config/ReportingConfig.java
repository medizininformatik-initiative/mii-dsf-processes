package de.medizininformatik_initiative.process.report.spring.config;

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
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.kds.client.spring.config.PropertiesConfig;

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

	@Autowired
	private PropertiesConfig kdsFhirClientConfig;

	@ProcessDocumentation(processNames = {
			"medizininformatik-initiativede_reportSend" }, description = "The KDS FHIR server type, possible values are [blaze, other] ; must be set, if a Blaze server is used")
	@Value("${de.medizininformatik.initiative.kds.fhir.server.type:other}")
	private String fhirStoreType;

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
		return new DownloadSearchBundle(clientProvider, taskHelper, readAccessHelper, kdsFhirClientConfig.dataLogger());
	}

	@Bean
	public CheckSearchBundle checkSearchBundle()
	{
		return new CheckSearchBundle(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public CreateReport createReport()
	{
		return new CreateReport(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				kdsFhirClientConfig.kdsClientFactory(), fhirStoreType, kdsFhirClientConfig.dataLogger());
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
