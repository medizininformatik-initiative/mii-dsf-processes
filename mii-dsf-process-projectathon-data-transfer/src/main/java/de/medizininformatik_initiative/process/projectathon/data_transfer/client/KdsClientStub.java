package de.medizininformatik_initiative.process.projectathon.data_transfer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.projectathon.data_transfer.client.fhir.KdsFhirClient;
import de.medizininformatik_initiative.process.projectathon.data_transfer.client.fhir.KdsFhirClientStub;

public class KdsClientStub implements KdsClient
{
	private static final Logger logger = LoggerFactory.getLogger(KdsClientStub.class);

	private final FhirContext fhirContext;
	private final String localIdentifierValue;
	private final String kdsServerBase;

	KdsClientStub(FhirContext fhirContext, String localIdentifierValue)
	{
		this.fhirContext = fhirContext;
		this.localIdentifierValue = localIdentifierValue;
		this.kdsServerBase = "http://foo.bar/fhir";
	}

	@Override
	public void testConnection()
	{
		logger.warn("Stub implementation, no connection test performed");
	}

	@Override
	public KdsFhirClient getFhirClient()
	{
		return new KdsFhirClientStub(this);
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	public IGenericClient getGenericFhirClient()
	{
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public String getLocalIdentifierValue()
	{
		return localIdentifierValue;
	}

	@Override
	public String getFhirBaseUrl()
	{
		return kdsServerBase;
	}
}
