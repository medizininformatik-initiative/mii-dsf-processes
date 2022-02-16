package de.medizininformatik_initiative.processes.projectathon.data_transfer.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.fhir.KdsFhirClient;

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
	private final Class<KdsFhirClient> kdsFhirClientClass;

	private final String localIdentifierValue;

	public KdsClientImpl(KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, int connectTimeout,
			int socketTimeout, int connectionRequestTimeout, String kdsServerBasicAuthUsername,
			String kdsServerBasicAuthPassword, String kdsServerBearerToken, String kdsServerBase, String proxyUrl,
			String proxyUsername, String proxyPassword, boolean hapiClientVerbose, FhirContext fhirContext,
			Class<KdsFhirClient> kdsFhirClientClass, String localIdentifierValue)
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
		this.kdsFhirClientClass = kdsFhirClientClass;

		this.localIdentifierValue = localIdentifierValue;
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
	public void testConnection()
	{
		CapabilityStatement statement = getGenericFhirClient().capabilities().ofType(CapabilityStatement.class)
				.execute();

		logger.info("Connection test OK {} - {}", statement.getSoftware().getName(),
				statement.getSoftware().getVersion());
	}

	@Override
	public KdsFhirClient getFhirClient()
	{
		try
		{
			Constructor<KdsFhirClient> constructor = kdsFhirClientClass.getConstructor(KdsClient.class);

			return constructor.newInstance(this);
		}
		catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e)
		{
			logger.warn("Error while creating KDS FHIR client: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
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
