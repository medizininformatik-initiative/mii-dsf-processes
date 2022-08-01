package de.medizininformatik_initiative.process.kds.report.service;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_HRP;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class SelectTargetHrp extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(SelectTargetHrp.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public SelectTargetHrp(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
			EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Organization organization = getHrpOrganization();
		String organizationIdentifier = extractHrpIdentifier(organization);

		Endpoint endpoint = getHrpEndpoint(organizationIdentifier);
		String endpointIdentifier = extractEndpointIdentifier(endpoint);

		Target target = createHrpTarget(organizationIdentifier, endpointIdentifier, endpoint.getAddress());

		logger.info("Using HRP with identifier '{}' and endpoint '{}'", target.getOrganizationIdentifierValue(),
				target.getEndpointUrl());

		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private Organization getHrpOrganization()
	{
		return organizationProvider
				.getOrganizationsByConsortiumAndRole(
						NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
						CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_HRP)
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not find any organization with role HRP in consortium '"
						+ NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM
						+ "'"));
	}

	private Endpoint getHrpEndpoint(String identifier)
	{
		return endpointProvider.getFirstDefaultEndpoint(identifier).orElseThrow(
				() -> new RuntimeException("Could not find any endpoint of HRP with identifier '" + identifier + "'"));
	}

	private String extractHrpIdentifier(Organization organization)
	{
		return organization.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst()
				.orElseThrow(() -> new RuntimeException("organization is missing identifier of type '"
						+ NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER + "'"));
	}

	private String extractEndpointIdentifier(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).map(Identifier::getValue)
				.findFirst().orElseThrow(() -> new RuntimeException(
						"Endpoint is missing identifier of type '" + NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER + "'"));
	}

	private Target createHrpTarget(String organizationIdentifier, String endpointIdentifier, String endpointAddress)
	{
		return Target.createUniDirectionalTarget(organizationIdentifier, endpointIdentifier, endpointAddress);
	}
}
