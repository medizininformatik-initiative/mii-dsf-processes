package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class CheckSearchBundle extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckSearchBundle.class);

	private static final Pattern MODIFIERS = Pattern.compile(":.*");

	private static final String CAPABILITY_STATEMENT_PATH = "metadata";
	private static final String SUMMARY_SEARCH_PARAM = "_summary";
	private static final String SUMMARY_SEARCH_PARAM_VALUE_COUNT = "count";
	private static final List<String> VALID_SEARCH_PARAMS = List.of("_profile", "type", SUMMARY_SEARCH_PARAM);

	public CheckSearchBundle(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE);
		List<Bundle.BundleEntryComponent> searches = bundle.getEntry();

		testNoResources(searches);
		testRequestMethod(searches);
		testRequestUrls(searches);

		logger.info("Search Bundle contains only valid requests of type GET and valid search params {}",
				VALID_SEARCH_PARAMS);
	}

	private void testNoResources(List<Bundle.BundleEntryComponent> searches)
	{
		if (searches.stream().map(Bundle.BundleEntryComponent::getResource).anyMatch(Objects::nonNull))
			throw new RuntimeException("Search Bundle contains resources");
	}

	private void testRequestMethod(List<Bundle.BundleEntryComponent> searches)
	{
		if (searches.stream().map(s -> s.getRequest().getMethod()).anyMatch(m -> !Bundle.HTTPVerb.GET.equals(m)))
			throw new RuntimeException("Search Bundle contains HTTP method other then GET");
	}

	private void testRequestUrls(List<Bundle.BundleEntryComponent> searches)
	{
		List<UriComponents> uriComponents = searches.stream()
				.map(s -> UriComponentsBuilder.fromUriString(s.getRequest().getUrl()).build())
				.collect(Collectors.toList());

		testContainsSummaryCount(uriComponents);
		testContainsValidSearchParams(uriComponents);
	}

	private void testContainsSummaryCount(List<UriComponents> uriComponents)
	{
		uriComponents.stream().filter(u -> !CAPABILITY_STATEMENT_PATH.equals(u.getPath()))
				.map(u -> u.getQueryParams().toSingleValueMap()).forEach(this::testSummaryCount);
	}

	private void testSummaryCount(Map<String, String> queryParams)
	{
		if (!SUMMARY_SEARCH_PARAM_VALUE_COUNT.equals(queryParams.get(SUMMARY_SEARCH_PARAM)))
			throw new RuntimeException("Search Bundle contains request url without _summary=count");
	}

	private void testContainsValidSearchParams(List<UriComponents> uriComponents)
	{
		uriComponents.stream().filter(u -> !CAPABILITY_STATEMENT_PATH.equals(u.getPath()))
				.map(u -> u.getQueryParams().toSingleValueMap()).forEach(this::testSearchParams);
	}

	private void testSearchParams(Map<String, String> queryParams)
	{
		if (queryParams.keySet().stream().map(s -> MODIFIERS.matcher(s).replaceAll(""))
				.anyMatch(s -> !VALID_SEARCH_PARAMS.contains(s)))
			throw new RuntimeException("Search Bundle contains invalid search params, only allowed search params are "
					+ VALID_SEARCH_PARAMS);
	}

}
