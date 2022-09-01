package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
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
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class SelectCosTarget extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(SelectCosTarget.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public SelectCosTarget(OrganizationProvider organizationProvider, EndpointProvider endpointProvider,
			FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper, ReadAccessHelper readAccessHelper)
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
		logger.info(SelectCosTarget.class.getName());

		String cos = getCosIdentifier();
		Target target = getCosTarget(cos);

		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private String getCosIdentifier()
	{
		return organizationProvider.getOrganizationsByConsortiumAndRole(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS).map(Organization::getIdentifierFirstRep)
				.filter(i -> ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst().get();
	}


	private Target getCosTarget(String identifier)
	{
		return endpointProvider.getFirstConsortiumEndpoint(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS, identifier)
				.map(e -> Target.createUniDirectionalTarget(identifier, e.getIdentifierFirstRep().getValue(),
						e.getAddress()))
				.get();
	}
}
