package de.medizininformatik_initiative.process.projectathon.data_sharing.bpe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.junit.Test;
import org.springframework.core.env.StandardEnvironment;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.DataSharingProcessPluginDefinition;

public class DataSharingProcessPluginDefinitionTest
{
	@Test
	public void testResourceLoading() throws Exception
	{
		ProcessPluginDefinition definition = new DataSharingProcessPluginDefinition();
		ResourceProvider provider = definition.getResourceProvider(FhirContext.forR4(), getClass().getClassLoader(),
				new StandardEnvironment());
		assertNotNull(provider);

		var coordinate = provider.getResources(ConstantsDataSharing.PROCESS_NAME_FULL_COORDINATE_DATA_SHARING + "/"
				+ DataSharingProcessPluginDefinition.VERSION, s -> ResourceProvider.empty());
		assertNotNull(coordinate);
		assertEquals(6, coordinate.count());

		var execute = provider.getResources(ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING + "/"
				+ DataSharingProcessPluginDefinition.VERSION, s -> ResourceProvider.empty());
		assertNotNull(execute);
		assertEquals(5, execute.count());

		var merge = provider.getResources(ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING + "/"
				+ DataSharingProcessPluginDefinition.VERSION, s -> ResourceProvider.empty());
		assertNotNull(merge);
		assertEquals(8, merge.count());
	}
}
