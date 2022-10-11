package de.medizininformatik_initiative.processes.kds.client;

import static ca.uhn.fhir.rest.api.Constants.HEADER_PREFER;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;
import de.medizininformatik_initiative.processes.kds.client.logging.HapiClientLogger;

public class KdsClientImpl implements KdsClient
{
	private static final Logger logger = LoggerFactory.getLogger(KdsClientImpl.class);

	private final IRestfulClientFactory clientFactory;

	private final String kdsServerBase;

	private final String kdsServerBasicAuthUsername;
	private final String kdsServerBasicAuthPassword;
	private final String kdsServerBearerToken;

	private final boolean hapiClientVerbose;

	private final FhirContext fhirContext;

	private final String localIdentifierValue;

	private final DataLogger dataLogger;

	public KdsClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, int connectionRequestTimeout, String kdsServerBasicAuthUsername,
			String kdsServerBasicAuthPassword, String kdsServerBearerToken, String kdsServerBase, String proxyUrl,
			String proxyUsername, String proxyPassword, boolean hapiClientVerbose, FhirContext fhirContext,
			String localIdentifierValue, DataLogger dataLogger)
	{
		clientFactory = createClientFactory(trustStore, keyStore, keyStorePassword, connectTimeout, socketTimeout,
				connectionRequestTimeout);

		this.kdsServerBase = kdsServerBase;

		this.kdsServerBasicAuthUsername = kdsServerBasicAuthUsername;
		this.kdsServerBasicAuthPassword = kdsServerBasicAuthPassword;
		this.kdsServerBearerToken = kdsServerBearerToken;

		configureProxy(clientFactory, proxyUrl, proxyUsername, proxyPassword);

		this.hapiClientVerbose = hapiClientVerbose;

		this.fhirContext = fhirContext;

		this.localIdentifierValue = localIdentifierValue;

		this.dataLogger = dataLogger;
	}

	private void configureProxy(IRestfulClientFactory clientFactory, String proxyUrl, String proxyUsername,
			String proxyPassword)
	{
		if (proxyUrl != null && !proxyUrl.isBlank())
		{
			try
			{
				URL url = new URL(proxyUrl);
				clientFactory.setProxy(url.getHost(), url.getPort());
				clientFactory.setProxyCredentials(proxyUsername, proxyPassword);

				logger.info("Using proxy for KDS FHIR server connection with {host: {}, port: {}, username: {}}",
						url.getHost(), url.getPort(), proxyUsername);
			}
			catch (MalformedURLException e)
			{
				logger.error("Could not configure proxy", e);
			}
		}
	}

	protected ApacheRestfulClientFactoryWithTlsConfig createClientFactory(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, int connectTimeout, int socketTimeout, int connectionRequestTimeout)
	{
		FhirContext fhirContext = FhirContext.forR4();
		ApacheRestfulClientFactoryWithTlsConfig hapiClientFactory = new ApacheRestfulClientFactoryWithTlsConfig(
				fhirContext, trustStore, keyStore, keyStorePassword);
		hapiClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);

		hapiClientFactory.setConnectTimeout(connectTimeout);
		hapiClientFactory.setSocketTimeout(socketTimeout);
		hapiClientFactory.setConnectionRequestTimeout(connectionRequestTimeout);

		fhirContext.setRestfulClientFactory(hapiClientFactory);
		return hapiClientFactory;
	}

	private void configuredWithBasicAuth(IGenericClient client)
	{
		if (kdsServerBasicAuthUsername != null && kdsServerBasicAuthPassword != null)
			client.registerInterceptor(
					new BasicAuthInterceptor(kdsServerBasicAuthUsername, kdsServerBasicAuthPassword));
	}

	private void configureBearerTokenAuthInterceptor(IGenericClient client)
	{
		if (kdsServerBearerToken != null)
			client.registerInterceptor(new BearerTokenAuthInterceptor(kdsServerBearerToken));
	}

	private void configureLoggingInterceptor(IGenericClient client)
	{
		if (hapiClientVerbose)
		{
			LoggingInterceptor loggingInterceptor = new LoggingInterceptor(true);
			loggingInterceptor.setLogger(new HapiClientLogger(logger));
			client.registerInterceptor(loggingInterceptor);
		}
	}

	@Override
	public String getLocalIdentifierValue()
	{
		return localIdentifierValue;
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	public String getFhirBaseUrl()
	{
		return kdsServerBase;
	}

	@Override
	public IGenericClient getGenericFhirClient()
	{
		IGenericClient client = clientFactory.newGenericClient(kdsServerBase);

		configuredWithBasicAuth(client);
		configureBearerTokenAuthInterceptor(client);
		configureLoggingInterceptor(client);

		return client;
	}

	@Override
	public void testConnection()
	{
		CapabilityStatement statement = getGenericFhirClient().capabilities().ofType(CapabilityStatement.class)
				.execute();

		logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
				statement.getSoftware().getVersion());
	}

	@Override
	public Resource readByIdType(IdType idType)
	{
		IReadTyped<IBaseResource> readTyped = getGenericFhirClient().read().resource(idType.getResourceType());
		Resource toReturn = readByIdType(readTyped, idType);

		dataLogger.logResource("Read Resource from url=" + idType.getValue(), toReturn);

		return toReturn;
	}

	private Resource readByIdType(IReadTyped<IBaseResource> readTyped, IdType idType)
	{
		if (idType.hasVersionIdPart())
			return (Resource) readTyped.withIdAndVersion(idType.getIdPart(), idType.getVersionIdPart()).execute();
		else
			return (Resource) readTyped.withId(idType.getIdPart()).execute();
	}

	@Override
	public Binary readBinary(IdType idType)
	{
		Binary toReturn = getGenericFhirClient().read().resource(Binary.class).withId(idType.getIdPart()).execute();

		dataLogger.logResource("Read Binary from url=" + idType.getValue(), toReturn);

		return toReturn;
	}

	@Override
	public Bundle searchDocumentReferences(String system, String code)
	{
		Bundle toReturn = getGenericFhirClient().search().forResource(DocumentReference.class)
				.where(DocumentReference.IDENTIFIER.exactly().systemAndIdentifier(system, code))
				.returnBundle(Bundle.class).execute();

		dataLogger.logResource("DocumentReference Search-Response Bundle based on system|code=" + system + "|" + code,
				toReturn);

		return toReturn;
	}

	@Override
	public Bundle executeTransactionBundle(Bundle toExecute)
	{
		dataLogger.logResource("Executing Transaction Bundle", toExecute);

		Bundle toReturn = getGenericFhirClient().transaction().withBundle(toExecute)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();

		dataLogger.logResource("Transaction Bundle Response", toReturn);

		return toReturn;
	}

	@Override
	public Bundle executeBatchBundle(Bundle toExecute)
	{
		dataLogger.logResource("Executing Batch Bundle", toExecute);

		Bundle toReturn = getGenericFhirClient().transaction().withBundle(toExecute)
				.withAdditionalHeader(HEADER_PREFER, "handling=strict").execute();

		dataLogger.logResource("Batch Bundle Response", toReturn);

		return toReturn;
	}

	@Override
	public MethodOutcome createResource(Resource toCreate)
	{
		dataLogger.logResource("Creating " + toCreate.getResourceType().name(), toCreate);

		MethodOutcome outcome = getGenericFhirClient().create().resource(toCreate).execute();

		dataLogger.logMethodOutcome("Create Task MethodOutcome", outcome);

		return outcome;
	}
}
