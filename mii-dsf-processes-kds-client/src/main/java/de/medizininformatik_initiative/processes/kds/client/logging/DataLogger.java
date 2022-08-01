package de.medizininformatik_initiative.processes.kds.client.logging;

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

	public void logResource(String logMessage, Resource resource)
	{
		if (enabled)
			logger.debug("{}: {}", logMessage, asString(resource));
	}

	private String asString(Resource resource)
	{
		return fhirContext.newJsonParser().encodeResourceToString(resource);
	}
}
