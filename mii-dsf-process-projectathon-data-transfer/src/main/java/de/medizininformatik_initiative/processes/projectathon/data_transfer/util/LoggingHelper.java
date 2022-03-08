package de.medizininformatik_initiative.processes.projectathon.data_transfer.util;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class LoggingHelper
{
	private static final Logger logger = LoggerFactory.getLogger(LoggingHelper.class);

	public static void logDebugBundle(String message, Bundle bundle)
	{
		Bundle copy = bundle.copy();

		// replace actual content with content size in bytes to not leak sensitive information
		copy.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
				.map(Bundle.BundleEntryComponent::getResource).filter(r -> r instanceof Binary).map(r -> (Binary) r)
				.forEach(b -> b.setContent(createReplacementContent(b)));

		logDebugResource(message, copy);
	}

	public static void logDebugBinary(String message, Binary binary)
	{
		Binary copy = binary.copy();

		// replace actual content with content size in bytes to not leak sensitive information
		copy.setContent(createReplacementContent(copy));

		logDebugResource(message, copy);
	}

	public static void logDebugResource(String message, Resource resource)
	{
		logger.debug(message + ": {}",
				FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(resource));
	}

	private static byte[] createReplacementContent(Binary binary)
	{
		return ("original content (" + binary.getContent().length + " bytes) replaced")
				.getBytes(StandardCharsets.UTF_8);
	}
}
