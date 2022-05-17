package de.medizininformatik_initiative.process.projectathon.data_transfer.bpe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.junit.Test;
import org.springframework.core.env.StandardEnvironment;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.DataTransferProcessPluginDefinition;

public class DataTransferProcessPluginDefinitionTest
{
	@Test
	public void testResourceLoading() throws Exception
	{
		ProcessPluginDefinition definition = new DataTransferProcessPluginDefinition();
		ResourceProvider provider = definition.getResourceProvider(FhirContext.forR4(), getClass().getClassLoader(),
				new StandardEnvironment());
		assertNotNull(provider);

		var send = provider.getResources(
				ConstantsDataTransfer.PROCESS_NAME_FULL_DATA_SEND + "/" + DataTransferProcessPluginDefinition.VERSION,
				s -> ResourceProvider.empty());
		assertNotNull(send);
		assertEquals(7, send.count());

		var receive = provider.getResources(ConstantsDataTransfer.PROCESS_NAME_FULL_DATA_RECEIVE + "/"
				+ DataTransferProcessPluginDefinition.VERSION, s -> ResourceProvider.empty());
		assertNotNull(receive);
		assertEquals(7, receive.count());
	}
}
