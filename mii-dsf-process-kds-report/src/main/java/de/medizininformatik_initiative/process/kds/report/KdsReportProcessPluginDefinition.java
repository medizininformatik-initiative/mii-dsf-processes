package de.medizininformatik_initiative.process.kds.report;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.AbstractResource;
import org.highmed.dsf.fhir.resources.ActivityDefinitionResource;
import org.highmed.dsf.fhir.resources.CodeSystemResource;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.highmed.dsf.fhir.resources.StructureDefinitionResource;
import org.highmed.dsf.fhir.resources.ValueSetResource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.kds.report.spring.config.KdsReportConfig;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class KdsReportProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String VERSION = "0.3.1";
	public static final LocalDate RELEASE_DATE = LocalDate.of(2022, 10, 21);

	@Override
	public String getName()
	{
		return "mii-process-kds-report";
	}

	@Override
	public String getVersion()
	{
		return VERSION;
	}

	@Override
	public LocalDate getReleaseDate()
	{
		return RELEASE_DATE;
	}

	@Override
	public Stream<String> getBpmnFiles()
	{
		return Stream.of("bpe/kds-report-autostart.bpmn", "bpe/kds-report-send.bpmn", "bpe/kds-report-receive.bpmn");
	}

	@Override
	public Stream<Class<?>> getSpringConfigClasses()
	{
		return Stream.of(KdsReportConfig.class);
	}

	@Override
	public ResourceProvider getResourceProvider(FhirContext fhirContext, ClassLoader classLoader,
			PropertyResolver resolver)
	{
		var aAutostart = ActivityDefinitionResource.file("fhir/ActivityDefinition/mii-kds-report-autostart.xml");
		var aReceive = ActivityDefinitionResource.file("fhir/ActivityDefinition/mii-kds-report-receive.xml");
		var aSend = ActivityDefinitionResource.file("fhir/ActivityDefinition/mii-kds-report-send.xml");

		var cReport = CodeSystemResource.file("fhir/CodeSystem/mii-kds-report.xml");
		var cReportStatus = CodeSystemResource.file("fhir/CodeSystem/mii-kds-report-status.xml");

		var eReportStatusError = StructureDefinitionResource
				.file("fhir/StructureDefinition/extension-mii-kds-report-status-error.xml");

		var sAutostartStart = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-kds-report-task-autostart-start.xml");
		var sAutostartStop = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-kds-report-task-autostart-stop.xml");
		var sSearchBundle = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-kds-report-search-bundle.xml");
		var sSearchBundleResponse = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-kds-report-search-bundle-response.xml");
		var sReceive = StructureDefinitionResource.file("fhir/StructureDefinition/mii-kds-report-task-receive.xml");
		var sSend = StructureDefinitionResource.file("fhir/StructureDefinition/mii-kds-report-task-send.xml");
		var sSendStart = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-kds-report-task-send-start.xml");

		var vReport = ValueSetResource.file("fhir/ValueSet/mii-kds-report.xml");
		var vReportStatusReceive = ValueSetResource.file("fhir/ValueSet/mii-kds-report-status-receive.xml");
		var vReportStatusSend = ValueSetResource.file("fhir/ValueSet/mii-kds-report-status-send.xml");

		Map<String, List<AbstractResource>> resourcesByProcessKeyAndVersion = Map.of(
				ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART + "/" + VERSION,
				Arrays.asList(aAutostart, cReport, sAutostartStart, sAutostartStop, vReport),
				ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_RECEIVE + "/" + VERSION,
				Arrays.asList(aReceive, cReport, cReportStatus, eReportStatusError, sSend, sSearchBundle,
						sSearchBundleResponse, vReport, vReportStatusReceive),
				ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND + "/" + VERSION,
				Arrays.asList(aSend, cReport, cReportStatus, eReportStatusError, sReceive, sSearchBundle,
						sSearchBundleResponse, sSendStart, vReport, vReportStatusSend));

		return ResourceProvider.read(VERSION, RELEASE_DATE,
				() -> fhirContext.newXmlParser().setStripVersionsFromReferences(false), classLoader, resolver,
				resourcesByProcessKeyAndVersion);
	}

	@Override
	public void onProcessesDeployed(ApplicationContext pluginApplicationContext, List<String> activeProcesses)
	{
		if (activeProcesses.contains(ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND))
			pluginApplicationContext.getBean(KdsClientFactory.class).testConnection();
	}
}
