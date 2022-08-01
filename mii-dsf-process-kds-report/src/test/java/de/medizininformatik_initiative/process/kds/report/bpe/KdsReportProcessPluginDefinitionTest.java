package de.medizininformatik_initiative.process.kds.report.bpe;

import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_RECEIVE;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_SEND;
import static de.medizininformatik_initiative.process.kds.report.KdsReportProcessPluginDefinition.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.junit.Test;
import org.springframework.core.env.StandardEnvironment;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.kds.report.KdsReportProcessPluginDefinition;

public class KdsReportProcessPluginDefinitionTest
{
	@Test
	public void testResourceLoading()
	{
		ProcessPluginDefinition definition = new KdsReportProcessPluginDefinition();
		ResourceProvider provider = definition.getResourceProvider(FhirContext.forR4(), getClass().getClassLoader(),
				new StandardEnvironment());
		assertNotNull(provider);

		var kdsReportAutostart = provider.getResources(PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART + "/" + VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(kdsReportAutostart);
		assertEquals(5, kdsReportAutostart.count());

		var kdsReportReceive = provider.getResources(PROCESS_NAME_FULL_KDS_REPORT_RECEIVE + "/" + VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(kdsReportReceive);
		assertEquals(9, kdsReportReceive.count());

		var kdsReportSend = provider.getResources(PROCESS_NAME_FULL_KDS_REPORT_SEND + "/" + VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(kdsReportSend);
		assertEquals(10, kdsReportSend.count());
	}
}
