package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

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
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class SelectCosTarget extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(SelectCosTarget.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public SelectCosTarget(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		String cos = getCosIdentifier(projectIdentifier);
		Target target = getCosTarget(cos);

		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private String getCosIdentifier(String projectIdentifier)
	{
		Task task = getLeadingTaskFromExecutionVariables();
		return getTaskHelper()
				.getInputParameterReferenceValues(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER)
				.filter(Reference::hasIdentifier).map(Reference::getIdentifier).map(Identifier::getValue)
				.peek(i -> logger.debug("Project '{}' has participating COS '{}'", projectIdentifier, i)).findFirst()
				.orElseThrow(
						() -> new RuntimeException("No COS identifier found in Task with id='" + task.getId() + "'"));
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
