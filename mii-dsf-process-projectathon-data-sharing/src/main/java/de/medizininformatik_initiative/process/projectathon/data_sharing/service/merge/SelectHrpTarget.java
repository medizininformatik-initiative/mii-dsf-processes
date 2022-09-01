package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class SelectHrpTarget extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(SelectHrpTarget.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public SelectHrpTarget(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
	protected void doExecute(DelegateExecution execution)
	{
		logger.info(SelectHrpTarget.class.getName());

		String hrp = getHrpIdentifier();
		Target target = getHrpTarget(hrp);

		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private String getHrpIdentifier()
	{
		return organizationProvider.getOrganizationsByConsortiumAndRole(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_HRP).map(Organization::getIdentifierFirstRep)
				.filter(i -> ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst()
				.orElseThrow(() -> new RuntimeException("No organization with role HRP found"));
	}

	private Target getHrpTarget(String identifier)
	{
		return endpointProvider.getFirstConsortiumEndpoint(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_HRP, identifier)
				.map(e -> Target.createUniDirectionalTarget(identifier, e.getIdentifierFirstRep().getValue(),
						e.getAddress()))
				.orElseThrow(() -> new RuntimeException("No endpoint of organization with role HRP found"));
	}
}
