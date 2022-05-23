package de.medizininformatik_initiative.processes.kds.client.fhir;

import static ca.uhn.fhir.rest.api.Constants.HEADER_PREFER;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;

import de.medizininformatik_initiative.processes.kds.client.KdsClient;

public class KdsFhirClientImpl implements KdsFhirClient
{
	private KdsClient kdsClient;

	public KdsFhirClientImpl(KdsClient kdsClient)
	{
		this.kdsClient = kdsClient;
	}

	@Override
	public Bundle searchDocumentReferences(String system, String code)
	{
		return kdsClient.getGenericFhirClient().search().forResource(DocumentReference.class)
				.where(DocumentReference.IDENTIFIER.exactly().systemAndIdentifier(system, code))
				.returnBundle(Bundle.class).execute();
	}

	@Override
	public Binary readBinary(String url)
	{
		return kdsClient.getGenericFhirClient().read().resource(Binary.class).withId(new IdType(url).getIdPart())
				.execute();
	}

	@Override
	public Bundle executeTransactionBundle(Bundle toExecute)
	{
		return kdsClient.getGenericFhirClient().transaction().withBundle(toExecute)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();
	}

	@Override
	public Bundle executeBatchBundle(Bundle toExecute)
	{
		return kdsClient.getGenericFhirClient().transaction().withBundle(toExecute)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();
	}
}