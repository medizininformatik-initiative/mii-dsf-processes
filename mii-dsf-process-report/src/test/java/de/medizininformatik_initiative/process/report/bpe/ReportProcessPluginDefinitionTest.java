package de.medizininformatik_initiative.process.report.bpe;

import static de.medizininformatik_initiative.process.report.ConstantsReport.PROCESS_NAME_FULL_REPORT_AUTOSTART;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROCESS_NAME_FULL_REPORT_RECEIVE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.PROCESS_NAME_FULL_REPORT_SEND;
import static de.medizininformatik_initiative.process.report.ReportProcessPluginDefinition.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.junit.Test;
import org.springframework.core.env.StandardEnvironment;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.report.ReportProcessPluginDefinition;

public class ReportProcessPluginDefinitionTest
{
	@Test
	public void testResourceLoading() throws Exception
	{
		ProcessPluginDefinition definition = new ReportProcessPluginDefinition();
		ResourceProvider provider = definition.getResourceProvider(FhirContext.forR4(), getClass().getClassLoader(),
				new StandardEnvironment());
		assertNotNull(provider);

		var reportAutostart = provider.getResources(PROCESS_NAME_FULL_REPORT_AUTOSTART + "/" + VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(reportAutostart);
		assertEquals(5, reportAutostart.count());

		var reportReceive = provider.getResources(PROCESS_NAME_FULL_REPORT_RECEIVE + "/" + VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(reportReceive);
		assertEquals(7, reportReceive.count());

		var reportSend = provider.getResources(PROCESS_NAME_FULL_REPORT_SEND + "/" + VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(reportSend);
		assertEquals(8, reportSend.count());

	}
}
