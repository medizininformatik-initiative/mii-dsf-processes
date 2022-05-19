package de.medizininformatik_initiative.process.report;

import static de.medizininformatik_initiative.process.report.ConstantsReport.PROCESS_NAME_FULL_REPORT_AUTOSTART;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROCESS_NAME_FULL_REPORT_RECEIVE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND;

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
import org.springframework.core.env.PropertyResolver;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.report.spring.config.ReportingConfig;

public class ReportProcessPluginDefinition implements ProcessPluginDefinition
{

	public static final String VERSION = "0.2.0";
	public static final LocalDate RELEASE_DATE = LocalDate.of(2022, 4, 14);

	@Override
	public String getName()
	{
		return "mii-process-reporting";
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
		return Stream.of("bpe/report-autostart.bpmn", "bpe/report-send.bpmn", "bpe/report-receive.bpmn");
	}

	@Override
	public Stream<Class<?>> getSpringConfigClasses()
	{
		return Stream.of(ReportingConfig.class);
	}

	@Override
	public ResourceProvider getResourceProvider(FhirContext fhirContext, ClassLoader classLoader,
			PropertyResolver resolver)
	{
		var aAutostart = ActivityDefinitionResource.file("fhir/ActivityDefinition/mii-report-autostart.xml");
		var aReceive = ActivityDefinitionResource.file("fhir/ActivityDefinition/mii-report-receive.xml");
		var aSend = ActivityDefinitionResource.file("fhir/ActivityDefinition/mii-report-send.xml");

		var cReport = CodeSystemResource.file("fhir/CodeSystem/mii-report.xml");
		var cReportStatus = CodeSystemResource.file("fhir/CodeSystem/mii-report-status.xml");

		var eReportStatusError = StructureDefinitionResource
				.file("fhir/StructureDefinition/extension-mii-report-status-error.xml");

		var sAutostartStart = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-report-task-autostart-start.xml");
		var sAutostartStop = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-report-task-autostart-stop.xml");
		var sReceive = StructureDefinitionResource.file("fhir/StructureDefinition/mii-report-task-receive.xml");
		var sSend = StructureDefinitionResource.file("fhir/StructureDefinition/mii-report-task-send.xml");
		var sSendStart = StructureDefinitionResource.file("fhir/StructureDefinition/mii-report-task-send-start.xml");

		var vReport = ValueSetResource.file("fhir/ValueSet/mii-report.xml");
		var vReportStatusReceive = ValueSetResource.file("fhir/ValueSet/mii-report-status-receive.xml");
		var vReportStatusSend = ValueSetResource.file("fhir/ValueSet/mii-report-status-send.xml");

		Map<String, List<AbstractResource>> resourcesByProcessKeyAndVersion = Map.of(
				PROCESS_NAME_FULL_REPORT_AUTOSTART + "/" + VERSION,
				Arrays.asList(aAutostart, cReport, sAutostartStart, sAutostartStop, vReport),
				PROCESS_NAME_FULL_REPORT_RECEIVE + "/" + VERSION,
				Arrays.asList(aReceive, cReport, cReportStatus, eReportStatusError, sSend, vReport,
						vReportStatusReceive),
				PROCESS_NAME_FULL_REPORT_SEND + "/" + VERSION, Arrays.asList(aSend, cReport, cReportStatus,
						eReportStatusError, sReceive, sSendStart, vReport, vReportStatusSend));

		return ResourceProvider.read(VERSION, RELEASE_DATE,
				() -> fhirContext.newXmlParser().setStripVersionsFromReferences(false), classLoader, resolver,
				resourcesByProcessKeyAndVersion);
	}
}
