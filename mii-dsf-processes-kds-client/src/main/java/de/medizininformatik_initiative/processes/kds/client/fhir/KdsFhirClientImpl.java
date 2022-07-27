package de.medizininformatik_initiative.processes.kds.client.fhir;

import static ca.uhn.fhir.rest.api.Constants.HEADER_PREFER;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;

import de.medizininformatik_initiative.processes.kds.client.KdsClient;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class KdsFhirClientImpl implements KdsFhirClient
{
	private final KdsClient kdsClient;
	private final DataLogger dataLogger;

	public KdsFhirClientImpl(KdsClient kdsClient, DataLogger dataLogger)
	{
		this.kdsClient = kdsClient;
		this.dataLogger = dataLogger;
	}

	@Override
	public Bundle searchDocumentReferences(String system, String code)
	{
		Bundle toReturn = kdsClient.getGenericFhirClient().search().forResource(DocumentReference.class)
				.where(DocumentReference.IDENTIFIER.exactly().systemAndIdentifier(system, code))
				.returnBundle(Bundle.class).execute();

		dataLogger.logResource("DocumentReference Search-Response Bundle based on system|code=" + system + "|" + code,
				toReturn);

		return toReturn;
	}

	@Override
	public Binary readBinary(String url)
	{
		Binary toReturn = kdsClient.getGenericFhirClient().read().resource(Binary.class)
				.withId(new IdType(url).getIdPart()).execute();

		dataLogger.logResource("Read Binary from url=" + url, toReturn);

		return toReturn;
	}

	@Override
	public Bundle executeTransactionBundle(Bundle toExecute)
	{
		dataLogger.logResource("Executing Transaction Bundle", toExecute);

		Bundle toReturn = kdsClient.getGenericFhirClient().transaction().withBundle(toExecute)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();

		dataLogger.logResource("Transaction Bundle Response", toReturn);

		return toReturn;
	}

	@Override
	public Bundle executeBatchBundle(Bundle toExecute)
	{
		dataLogger.logResource("Executing Batch Bundle", toExecute);

		Bundle toReturn = kdsClient.getGenericFhirClient().transaction().withBundle(toExecute)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();

		dataLogger.logResource("Batch Bundle Response", toReturn);

		return toReturn;
	}
}
