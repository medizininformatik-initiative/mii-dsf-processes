package de.medizininformatik_initiative.process.projectathon.data_transfer.client.fhir;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;

public interface KdsFhirClient
{
	Bundle searchDocumentReferences(String system, String code);

	Binary readBinary(String url);

	Bundle storeBundle(Bundle toStore);
}
