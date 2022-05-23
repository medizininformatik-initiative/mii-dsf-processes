package de.medizininformatik_initiative.processes.kds.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.processes.kds.client.fhir.KdsFhirClient;

public interface KdsClient
{
	FhirContext getFhirContext();

	void testConnection();

	KdsFhirClient getFhirClient();

	IGenericClient getGenericFhirClient();

	String getLocalIdentifierValue();

	String getFhirBaseUrl();
}
