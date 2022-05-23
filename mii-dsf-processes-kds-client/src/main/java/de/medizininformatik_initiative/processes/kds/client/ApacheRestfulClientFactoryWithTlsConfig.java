package de.medizininformatik_initiative.processes.kds.client;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.client.apache.ApacheHttpClient;
import ca.uhn.fhir.rest.client.api.Header;
import ca.uhn.fhir.rest.client.api.IHttpClient;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;

public class ApacheRestfulClientFactoryWithTlsConfig extends RestfulClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(ApacheRestfulClientFactoryWithTlsConfig.class);

	private HttpClient myHttpClient;
	private HttpHost myProxy;

	private final KeyStore trustStore;
	private final KeyStore keyStore;
	private final char[] keyStorePassword;

	private final Map<String, ApacheHttpClient> clientByServerBase = new HashMap<>();

	public ApacheRestfulClientFactoryWithTlsConfig(FhirContext fhirContext, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword)
	{
		super(fhirContext);

		this.trustStore = trustStore;
		this.keyStore = keyStore;
		this.keyStorePassword = keyStorePassword;
	}

	@Override
	protected synchronized ApacheHttpClient getHttpClient(String serverBase)
	{
		if (clientByServerBase.containsKey(serverBase))
		{
			logger.debug("Reusing ApacheHttpClient for ServerBase {}", serverBase);
			return clientByServerBase.get(serverBase);
		}
		else
		{
			logger.debug("Returning new ApacheHttpClient for ServerBase {}", serverBase);
			ApacheHttpClient client = new ApacheHttpClient(getNativeHttpClient(), new StringBuilder(serverBase), null,
					null, null, null);
			clientByServerBase.put(serverBase, client);
			return client;
		}
	}

	@Override
	public synchronized IHttpClient getHttpClient(StringBuilder theUrl, Map<String, List<String>> theIfNoneExistParams,
			String theIfNoneExistString, RequestTypeEnum theRequestType, List<Header> theHeaders)
	{
		return new ApacheHttpClient(getNativeHttpClient(), theUrl, theIfNoneExistParams, theIfNoneExistString,
				theRequestType, theHeaders);
	}

	public HttpClient getNativeHttpClient()
	{
		if (myHttpClient == null)
		{
			SSLContext sslContext = getSslContext();

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", new SSLConnectionSocketFactory(sslContext)).build();

			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
					socketFactoryRegistry, null, null, null, 5000, TimeUnit.MILLISECONDS);

			connectionManager.setMaxTotal(getPoolMaxTotal());
			connectionManager.setDefaultMaxPerRoute(getPoolMaxPerRoute());

			RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(getSocketTimeout())
					.setConnectTimeout(getConnectTimeout()).setConnectionRequestTimeout(getConnectionRequestTimeout())
					.setProxy(myProxy).build();

			HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connectionManager)
					.setSSLContext(sslContext).setDefaultRequestConfig(defaultRequestConfig).disableCookieManagement();

			if (myProxy != null && StringUtils.isNotBlank(getProxyUsername())
					&& StringUtils.isNotBlank(getProxyPassword()))
			{
				CredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(new AuthScope(myProxy.getHostName(), myProxy.getPort()),
						new UsernamePasswordCredentials(getProxyUsername(), getProxyPassword()));
				builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
				builder.setDefaultCredentialsProvider(credsProvider);
			}

			myHttpClient = builder.build();
		}

		return myHttpClient;
	}

	protected SSLContext getSslContext()
	{
		try
		{
			SSLContextBuilder custom = SSLContexts.custom();

			if (trustStore != null)
				custom = custom.loadTrustMaterial(trustStore, null);
			if (keyStore != null && keyStorePassword != null)
				custom = custom.loadKeyMaterial(keyStore, keyStorePassword);

			return custom.build();
		}
		catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void resetHttpClient()
	{
		this.myHttpClient = null;
	}

	/**
	 * Only allows to set an instance of type org.apache.http.client.HttpClient
	 *
	 * @see ca.uhn.fhir.rest.client.api.IRestfulClientFactory#setHttpClient(Object)
	 */
	@Override
	public synchronized void setHttpClient(Object theHttpClient)
	{
		this.myHttpClient = (HttpClient) theHttpClient;
	}

	@Override
	public void setProxy(String theHost, Integer thePort)
	{
		if (theHost != null)
		{
			myProxy = new HttpHost(theHost, thePort, "http");
		}
		else
		{
			myProxy = null;
		}
	}
}
