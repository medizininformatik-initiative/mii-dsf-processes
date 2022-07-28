package de.medizininformatik_initiative.processes.kds.client;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public interface KdsClient
{
	String getLocalIdentifierValue();

	FhirContext getFhirContext();

	String getFhirBaseUrl();

	IGenericClient getGenericFhirClient();

	void testConnection();

	Bundle searchDocumentReferences(String system, String code);

	Binary readBinary(String url);

	Bundle executeTransactionBundle(Bundle toExecute);

	Bundle executeBatchBundle(Bundle toExecute);
}
