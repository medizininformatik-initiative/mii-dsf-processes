package de.medizininformatik_initiative.processes.test.data.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.highmed.dsf.fhir.service.ReferenceCleaner;
import org.highmed.dsf.fhir.service.ReferenceCleanerImpl;
import org.highmed.dsf.fhir.service.ReferenceExtractor;
import org.highmed.dsf.fhir.service.ReferenceExtractorImpl;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class BundleGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(BundleGenerator.class);

	private final FhirContext fhirContext = FhirContext.forR4();
	private final ReferenceExtractor extractor = new ReferenceExtractorImpl();
	private final ReferenceCleaner cleaner = new ReferenceCleanerImpl(extractor);

	private Bundle bundle;

	private Bundle readAndCleanBundle(Path bundleTemplateFile)
	{
		try (InputStream in = Files.newInputStream(bundleTemplateFile))
		{
			Bundle bundle = newXmlParser().parseResource(Bundle.class, in);

			// FIXME hapi parser can't handle embedded resources and creates them while parsing bundles
			return cleaner.cleanReferenceResourcesIfBundle(bundle);
		}
		catch (IOException e)
		{
			logger.error("Error while reading bundle from " + bundleTemplateFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private void writeBundle(Path bundleFile, Bundle bundle)
	{
		try (OutputStream out = Files.newOutputStream(bundleFile);
				OutputStreamWriter writer = new OutputStreamWriter(out))
		{
			newXmlParser().encodeResourceToWriter(bundle, writer);
		}
		catch (IOException e)
		{
			logger.error("Error while writing bundle to " + bundleFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private IParser newXmlParser()
	{
		IParser parser = fhirContext.newXmlParser();
		parser.setStripVersionsFromReferences(false);
		parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
		parser.setPrettyPrint(true);
		return parser;
	}

	public void createDockerTestBundles(
			Map<String, CertificateGenerator.CertificateFiles> clientCertificateFilesByCommonName)
	{
		createDockerTestBundle(clientCertificateFilesByCommonName);
	}

	private void createDockerTestBundle(
			Map<String, CertificateGenerator.CertificateFiles> clientCertificateFilesByCommonName)
	{
		Path medic3BundleTemplateFile = Paths.get("src/main/resources/bundle-templates/bundle.xml");

		bundle = readAndCleanBundle(medic3BundleTemplateFile);

		Organization organizationCos = (Organization) bundle.getEntry().get(0).getResource();
		Extension organizationCosThumbprintExtension = organizationCos
				.getExtensionByUrl("http://highmed.org/fhir/StructureDefinition/extension-certificate-thumbprint");
		organizationCosThumbprintExtension.setValue(new StringType(
				clientCertificateFilesByCommonName.get("cos-client").getCertificateSha512ThumbprintHex()));

		Organization organizationDic = (Organization) bundle.getEntry().get(1).getResource();
		Extension organizationMedic1thumbprintExtension = organizationDic
				.getExtensionByUrl("http://highmed.org/fhir/StructureDefinition/extension-certificate-thumbprint");
		organizationMedic1thumbprintExtension.setValue(new StringType(
				clientCertificateFilesByCommonName.get("dic-client").getCertificateSha512ThumbprintHex()));

		writeBundle(Paths.get("bundle/bundle.xml"), bundle);
	}

	public void copyDockerTestBundles()
	{
		Path dicBundleFile = Paths.get("../mii-dsf-processes-docker-test-setup/dic/fhir/conf/bundle.xml");
		logger.info("Copying fhir bundle to {}", dicBundleFile);
		writeBundle(dicBundleFile, bundle);

		Path cosBundleFile = Paths.get("../mii-dsf-processes-docker-test-setup/cos/fhir/conf/bundle.xml");
		logger.info("Copying fhir bundle to {}", cosBundleFile);
		writeBundle(cosBundleFile, bundle);

	}
}
