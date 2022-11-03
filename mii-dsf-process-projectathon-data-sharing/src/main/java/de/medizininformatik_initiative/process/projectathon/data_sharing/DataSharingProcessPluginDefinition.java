package de.medizininformatik_initiative.process.projectathon.data_sharing;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.AbstractResource;
import org.highmed.dsf.fhir.resources.ActivityDefinitionResource;
import org.highmed.dsf.fhir.resources.CodeSystemResource;
import org.highmed.dsf.fhir.resources.NamingSystemResource;
import org.highmed.dsf.fhir.resources.QuestionnaireResource;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.highmed.dsf.fhir.resources.StructureDefinitionResource;
import org.highmed.dsf.fhir.resources.ValueSetResource;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_sharing.spring.config.DataSharingConfig;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class DataSharingProcessPluginDefinition implements ProcessPluginDefinition
{
	public static final String VERSION = "0.3.1";
	public static final LocalDate RELEASE_DATE = LocalDate.of(2022, 8, 1);

	@Override
	public String getName()
	{
		return "mii-process-projectathon-data-sharing";
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
		return Stream.of("bpe/coordinate.bpmn", "bpe/execute.bpmn", "bpe/merge.bpmn");
	}

	@Override
	public Stream<Class<?>> getSpringConfigClasses()
	{
		return Stream.of(DataSharingConfig.class);
	}

	@Override
	public ResourceProvider getResourceProvider(FhirContext fhirContext, ClassLoader classLoader,
			PropertyResolver propertyResolver)
	{
		var aCooDaSh = ActivityDefinitionResource
				.file("fhir/ActivityDefinition/mii-projectathon-coordinate-data-sharing.xml");
		var aExeDaSh = ActivityDefinitionResource
				.file("fhir/ActivityDefinition/mii-projectathon-execute-data-sharing.xml");
		var aMerDaSh = ActivityDefinitionResource
				.file("fhir/ActivityDefinition/mii-projectathon-merge-data-sharing.xml");

		var cC = CodeSystemResource.file("fhir/CodeSystem/mii-cryptography.xml");
		var cDaSh = CodeSystemResource.file("fhir/CodeSystem/mii-data-sharing.xml");

		var nP = NamingSystemResource.file("fhir/NamingSystem/mii-project-identifier.xml");

		var qReDa = QuestionnaireResource.file("fhir/Questionnaire/mii-questionnaire-release-data-set.xml");
		var qReMeDa = QuestionnaireResource.file("fhir/Questionnaire/mii-questionnaire-release-merged-data-set.xml");

		var sEmedId = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-extension-medic-identifier.xml");
		var sTcooDaSh = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-task-coordinate-data-sharing.xml");
		var sTexeDaSh = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-task-execute-data-sharing.xml");
		var sTmerDaSh = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-task-merge-data-sharing.xml");
		var sTsenDaSh = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-task-send-data-set.xml");
		var sTsenMerDaSh = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-task-send-merged-data-set.xml");
		var sTsenRecDaSh = StructureDefinitionResource
				.file("fhir/StructureDefinition/mii-projectathon-task-send-received-data-set.xml");

		var vC = ValueSetResource.file("fhir/ValueSet/mii-cryptography.xml");
		var vDaSh = ValueSetResource.file("fhir/ValueSet/mii-data-sharing.xml");

		Map<String, List<AbstractResource>> resourcesByProcessKeyAndVersion = Map.of(//
				ConstantsDataSharing.PROCESS_NAME_FULL_COORDINATE_DATA_SHARING + "/" + VERSION, //
				Arrays.asList(aCooDaSh, cDaSh, nP, sTcooDaSh, sTsenMerDaSh, sTsenRecDaSh, vDaSh), //
				ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING + "/" + VERSION, //
				Arrays.asList(aExeDaSh, cDaSh, nP, qReDa, sTexeDaSh, vDaSh), //
				ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING + "/" + VERSION, //
				Arrays.asList(aMerDaSh, cC, cDaSh, nP, qReMeDa, sEmedId, sTmerDaSh, sTsenDaSh, vC, vDaSh));

		return ResourceProvider.read(VERSION, RELEASE_DATE,
				() -> fhirContext.newXmlParser().setStripVersionsFromReferences(false), classLoader, propertyResolver,
				resourcesByProcessKeyAndVersion);
	}

	@Override
	public void onProcessesDeployed(ApplicationContext pluginApplicationContext, List<String> activeProcesses)
	{
		if (activeProcesses.contains(ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING)
				|| activeProcesses.contains(ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING))
		{
			pluginApplicationContext.getBean(KdsClientFactory.class).testConnection();
		}

		if (activeProcesses.contains(ConstantsDataSharing.PROCESS_NAME_FULL_MERGE_DATA_SHARING))
		{
			pluginApplicationContext.getBean(KeyProvider.class).createPublicKeyIfNotExists();
		}
	}
}
