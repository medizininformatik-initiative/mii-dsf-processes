package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.NAMING_SYSTEM_MII_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class CreateReport extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(CreateReport.class);

	private final OrganizationProvider organizationProvider;
	private final KdsClientFactory kdsClientFactory;

	public CreateReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
			KdsClientFactory kdsClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.kdsClientFactory = kdsClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(kdsClientFactory, "kdsClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Bundle searchBundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE);
		Target target = (Target) execution.getVariable(BPMN_EXECUTION_VARIABLE_TARGET);

		Bundle responseBundle = executeSearchBundle(searchBundle);
		Bundle report = transformToReport(searchBundle, responseBundle, target);
		String reportReference = storeResponseBundle(report);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE,
				Variables.stringValue(reportReference));
	}

	private Bundle executeSearchBundle(Bundle searchBundle)
	{
		return kdsClientFactory.getKdsClient().getFhirClient().executeBatchBundle(searchBundle);
	}

	private Bundle transformToReport(Bundle searchBundle, Bundle responseBundle, Target target)
	{
		Bundle report = new Bundle();
		report.setType(responseBundle.getType());
		report.getIdentifier().setSystem(NAMING_SYSTEM_MII_REPORT)
				.setValue("Report_" + organizationProvider.getLocalIdentifierValue());

		getReadAccessHelper().addLocal(report);
		getReadAccessHelper().addOrganization(report, target.getOrganizationIdentifierValue());

		for (int i = 0; i < searchBundle.getEntry().size(); i++)
		{
			Bundle.BundleEntryComponent responseEntry = responseBundle.getEntry().get(i);
			Bundle.BundleEntryComponent reportEntry = new Bundle.BundleEntryComponent();

			if (responseEntry.getResource() instanceof Bundle || responseEntry.getResource() == null)
				toEntryComponentBundleResource(searchBundle, i, responseEntry, reportEntry);

			if (responseEntry.getResource() instanceof CapabilityStatement)
				toEntryComponentCapabilityStatementResource(responseEntry, reportEntry);

			reportEntry.setResponse(responseEntry.getResponse());
			report.addEntry(reportEntry);
		}

		return report;
	}

	private void toEntryComponentBundleResource(Bundle searchBundle, int counter,
			Bundle.BundleEntryComponent responseEntry, Bundle.BundleEntryComponent reportEntry)
	{
		Bundle reportEntryBundle = new Bundle();
		reportEntryBundle.getMeta().setLastUpdated(new Date());
		reportEntryBundle.addLink().setRelation("self")
				.setUrl(searchBundle.getEntry().get(counter).getRequest().getUrl());
		reportEntryBundle.setType(Bundle.BundleType.SEARCHSET);
		reportEntryBundle.setTotal(0);

		if (responseEntry.getResource() instanceof Bundle)
		{
			Bundle responseEntryBundle = (Bundle) responseEntry.getResource();
			reportEntryBundle.setTotal(responseEntryBundle.getTotal());
			reportEntryBundle.getMeta().setLastUpdated(responseEntryBundle.getMeta().getLastUpdated());

			reportEntry.setResource(reportEntryBundle);
		}
	}

	private void toEntryComponentCapabilityStatementResource(Bundle.BundleEntryComponent responseEntry,
			Bundle.BundleEntryComponent reportEntry)
	{
		CapabilityStatement responseEntryCapabilityStatement = (CapabilityStatement) responseEntry.getResource();
		CapabilityStatement reportEntryCapabilityStatement = new CapabilityStatement();

		reportEntryCapabilityStatement.setKind(CapabilityStatement.CapabilityStatementKind.CAPABILITY);
		reportEntryCapabilityStatement.setStatus(responseEntryCapabilityStatement.getStatus());
		reportEntryCapabilityStatement.setDate(responseEntryCapabilityStatement.getDate());
		reportEntryCapabilityStatement.setName("Server");

		reportEntryCapabilityStatement.getSoftware().setName(responseEntryCapabilityStatement.getSoftware().getName());
		reportEntryCapabilityStatement.getSoftware()
				.setVersion(responseEntryCapabilityStatement.getSoftware().getVersion());

		reportEntryCapabilityStatement.setFhirVersion(responseEntryCapabilityStatement.getFhirVersion());

		reportEntryCapabilityStatement.setFormat(responseEntryCapabilityStatement.getFormat().stream()
				.filter(f -> "application/fhir+xml".equals(f.getCode()) || "application/fhir+json".equals(f.getCode()))
				.collect(Collectors.toList()));

		List<CapabilityStatement.CapabilityStatementRestComponent> rest = responseEntryCapabilityStatement.getRest();

		rest.stream().map(r -> r.setDocumentation(null)).map(r -> r.setSecurity(null)).map(r -> r.setCompartment(null))
				.forEach(r -> r.getInteraction().forEach(in -> in.setDocumentation(null)));

		rest.stream().flatMap(r -> r.getResource().stream()).map(r -> r.setProfile(null))
				.map(r -> r.setSupportedProfile(null)).map(r -> r.setDocumentation(null))
				.forEach(r -> r.getInteraction().forEach(in -> in.setDocumentation(null)));
		rest.stream().flatMap(r -> r.getResource().stream()).flatMap(r -> r.getSearchParam().stream())
				.forEach(s -> s.setDocumentation(null));
		rest.stream().flatMap(r -> r.getResource().stream()).flatMap(r -> r.getOperation().stream())
				.forEach(o -> o.setDocumentation(null));

		reportEntryCapabilityStatement.setRest(rest);
		reportEntry.setResource(reportEntryCapabilityStatement);
	}

	private String storeResponseBundle(Bundle responseBundle)
	{
		IdType bundleIdType = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
				.updateConditionaly(responseBundle, Map.of("identifier", Collections.singletonList(
						NAMING_SYSTEM_MII_REPORT + "|Report_" + organizationProvider.getLocalIdentifierValue())));

		logger.info("Stored report bundle with id {}", bundleIdType.getValue());

		return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Bundle.name(),
				bundleIdType.getIdPart(), bundleIdType.getVersionIdPart()).getValue();
	}
}