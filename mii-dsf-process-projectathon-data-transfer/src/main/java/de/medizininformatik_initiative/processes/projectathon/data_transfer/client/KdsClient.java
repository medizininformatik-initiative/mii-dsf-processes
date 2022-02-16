package de.medizininformatik_initiative.processes.projectathon.data_transfer.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.fhir.KdsFhirClient;

public interface KdsClient
{
	FhirContext getFhirContext();

	void testConnection();

	KdsFhirClient getFhirClient();

	IGenericClient getGenericFhirClient();

	String getLocalIdentifierValue();

	String getFhirBaseUrl();
}
