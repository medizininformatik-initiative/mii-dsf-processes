package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class SelectDataSetTarget extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(SelectDataSetTarget.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public SelectDataSetTarget(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		String cosIdentifier = getCosIdentifier(execution);
		String correlationKey = getCorrelationKey(execution);
		Target target = getCosTarget(cosIdentifier, correlationKey);

		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private String getCosIdentifier(DelegateExecution execution)
	{
		return (String) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);

	}

	private String getCorrelationKey(DelegateExecution execution)
	{
		return getTaskHelper().getFirstInputParameterStringValue(getLeadingTaskFromExecutionVariables(execution),
				ConstantsBase.CODESYSTEM_HIGHMED_BPMN, ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY)
				.orElseThrow(() -> new RuntimeException("No correlation key found"));
	}

	private Target getCosTarget(String identifier, String correlationKey)
	{
		return endpointProvider.getFirstConsortiumEndpoint(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS, identifier)
				.map(e -> Target.createBiDirectionalTarget(identifier, e.getIdentifierFirstRep().getValue(),
						e.getAddress(), correlationKey))
				.orElseThrow(() -> new RuntimeException("No endpoint of COS '" + identifier + "' found"));
	}
}
