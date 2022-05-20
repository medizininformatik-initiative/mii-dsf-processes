package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Collections;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

public class SelectTargetHrp extends AbstractServiceDelegate implements InitializingBean
{
	public SelectTargetHrp(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task task = getLeadingTaskFromExecutionVariables();

		IdType searchBundleId = getSearchBundleId(task);
		Bundle endpointOrganizationBundle = getEndpointOrganizationBundle(searchBundleId);

		Organization organization = extractOrganization(endpointOrganizationBundle, searchBundleId);
		Endpoint endpoint = extractEndpoint(endpointOrganizationBundle, searchBundleId);
		Target target = createTarget(organization, endpoint);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE,
				Variables.stringValue(searchBundleId.getValue()));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private IdType getSearchBundleId(Task task)
	{
		Reference reference = getTaskHelper()
				.getFirstInputParameterReferenceValue(task, CODESYSTEM_MII_REPORT,
						CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE)
				.orElseThrow(() -> new RuntimeException("No search bundle reference input parameter found"));

		IdType idType = new IdType(reference.getReference());

		if (!idType.hasBaseUrl())
			throw new RuntimeException(
					"Search Bundle reference (" + idType.getValue() + ") does not contain a base url");

		return idType;
	}

	private Bundle getEndpointOrganizationBundle(IdType searchBundleId)
	{
		Bundle bundle = getFhirWebserviceClientProvider().getLocalWebserviceClient().searchWithStrictHandling(
				Endpoint.class, Map.of("address", Collections.singletonList(searchBundleId.getBaseUrl()), "_include",
						Collections.singletonList("Endpoint:organization")));

		if (bundle.getEntry().size() != 2)
			throw new RuntimeException("Search for organization and endpoint based on url " + searchBundleId.getValue()
					+ " did return " + bundle.getEntry().size() + " results, expected 2");

		return bundle;
	}

	private Organization extractOrganization(Bundle endpointOrganizationBundle, IdType searchBundleId)
	{
		return endpointOrganizationBundle.getEntry().stream().filter(e -> e.getResource() instanceof Organization)
				.map(e -> (Organization) e.getResource()).findFirst()
				.orElseThrow(() -> new RuntimeException("Search for organization and endpoint based on url "
						+ searchBundleId.getValue() + " did not return any organization"));
	}

	private Endpoint extractEndpoint(Bundle endpointOrganizationBundle, IdType searchBundleId)
	{
		return endpointOrganizationBundle.getEntry().stream().filter(e -> e.getResource() instanceof Endpoint)
				.map(e -> (Endpoint) e.getResource()).findFirst()
				.orElseThrow(() -> new RuntimeException("Search for organization and endpoint based on url "
						+ searchBundleId.getValue() + " did not return any endpoint"));
	}

	private Target createTarget(Organization organization, Endpoint endpoint)
	{
		String organizationIdentifier = organization.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst().get();

		String endpointIdentifier = endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).map(Identifier::getValue)
				.findFirst().get();

		return Target.createUniDirectionalTarget(organizationIdentifier, endpointIdentifier, endpoint.getAddress());
	}
}
