package de.medizininformatik_initiative.processes.kds.client.logging;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class DataLogger
{
	private static final Logger logger = LoggerFactory.getLogger(DataLogger.class);

	private final boolean enabled;
	private final FhirContext fhirContext;

	public DataLogger(boolean enabled, FhirContext fhirContext)
	{
		this.enabled = enabled;
		this.fhirContext = fhirContext;
	}

	public void logBundle(String logMessage, Bundle resource)
	{
		Bundle copy = resource.copy();

		// Replace actual Binary content with content size in bytes to not leak sensitive information
		copy.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
				.map(Bundle.BundleEntryComponent::getResource).filter(r -> r instanceof Binary).map(r -> (Binary) r)
				.forEach(b -> b.setContent(createReplacementContent(b)));

		logResource(logMessage, copy);
	}

	public void logBinary(String logMessage, Binary resource)
	{
		Binary copy = resource.copy();

		// Replace actual Binary content with content size in bytes to not leak sensitive information
		copy.setContent(createReplacementContent(copy));

		logResource(logMessage, copy);
	}

	public void logDomainResource(String logMessage, DomainResource resource)
	{
		logResource(logMessage, resource);
	}

	private void logResource(String logMessage, Resource resource)
	{
		if (enabled)
			logger.debug("{}: {}", logMessage, asString(resource));
	}

	private static byte[] createReplacementContent(Binary binary)
	{
		return ("original content (" + binary.getContent().length + " bytes) replaced")
				.getBytes(StandardCharsets.UTF_8);
	}

	private String asString(Resource resource)
	{
		return fhirContext.newJsonParser().encodeResourceToString(resource);
	}
}
