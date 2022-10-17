package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class SelectDicTargets extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(SelectDicTargets.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public SelectDicTargets(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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

		Stream<String> medics = getMedicIdentifiers(execution, projectIdentifier);
		List<Target> targets = getMedicTargets(medics);

		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS,
				TargetsValues.create(new Targets(targets)));
	}

	private Stream<String> getMedicIdentifiers(DelegateExecution execution, String projectIdentifier)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		return getTaskHelper()
				.getInputParameterReferenceValues(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER)
				.filter(Reference::hasIdentifier).map(Reference::getIdentifier).map(Identifier::getValue)
				.peek(i -> logger.debug("Project '{}' has participating MeDIC '{}'", projectIdentifier, i));
	}

	private List<Target> getMedicTargets(Stream<String> identifiers)
	{
		return identifiers.map(this::createTarget).filter(Optional::isPresent).map(Optional::get)
				.collect(Collectors.toList());
	}

	private Optional<Target> createTarget(String identifier)
	{
		return endpointProvider.getFirstConsortiumEndpoint(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_MEDIC, identifier)
				.map(e -> Target.createBiDirectionalTarget(identifier, e.getIdentifierFirstRep().getValue(),
						e.getAddress(), UUID.randomUUID().toString()));
	}
}
