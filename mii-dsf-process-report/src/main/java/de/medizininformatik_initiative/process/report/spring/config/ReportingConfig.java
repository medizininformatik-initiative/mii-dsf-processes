package de.medizininformatik_initiative.process.report.spring.config;

import org.camunda.feel.syntaxtree.In;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.report.message.SendReceipt;
import de.medizininformatik_initiative.process.report.message.SendReport;
import de.medizininformatik_initiative.process.report.message.StartSendReport;
import de.medizininformatik_initiative.process.report.service.CreateReport;
import de.medizininformatik_initiative.process.report.service.DownloadReport;
import de.medizininformatik_initiative.process.report.service.InsertReport;
import de.medizininformatik_initiative.process.report.service.SelectTargetDic;
import de.medizininformatik_initiative.process.report.service.SelectTargetHrp;
import de.medizininformatik_initiative.process.report.service.StartTimer;
import de.medizininformatik_initiative.process.report.service.StopTimer;
import de.medizininformatik_initiative.process.report.service.StoreReceipt;

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
		return new SelectTargetHrp(clientProvider, taskHelper, readAccessHelper, endpointProvider);
	}

	@Bean
	public CreateReport createReport()
	{
		return new CreateReport(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SendReport sendReport()
	{
		return new SendReport(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Bean
	public StoreReceipt storeReceipt()
	{
		return new StoreReceipt(clientProvider, taskHelper, readAccessHelper);
	}

	// reportReceive Process

	@Bean
	public DownloadReport downloadReport()
	{
		return new DownloadReport(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public InsertReport insertReport()
	{
		return new InsertReport(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SelectTargetDic selectTargetDic()
	{
		return new SelectTargetDic(clientProvider, taskHelper, readAccessHelper, endpointProvider);
	}

	@Bean
	public SendReceipt sendReceipt()
	{
		return new SendReceipt(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}
}
