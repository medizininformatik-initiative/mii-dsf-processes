package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.variables.Researchers;
import de.medizininformatik_initiative.process.projectathon.data_sharing.variables.ResearchersValues;

public class PrepareMerging extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(PrepareMerging.class);

	private final EndpointProvider endpointProvider;

	public PrepareMerging(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);

		String projectIdentifier = getProjectIdentifier(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER,
				Variables.stringValue(projectIdentifier));

		String contractLocation = getContractLocation(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_CONTRACT_LOCATION,
				Variables.stringValue(contractLocation));

		String extractionInterval = getExtractionInterval(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_EXTRACTION_INTERVAL,
				Variables.stringValue(extractionInterval));

		List<String> researcherIdentifiers = getResearcherIdentifiers(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_RESEARCHER_IDENTIFIERS,
				ResearchersValues.create(new Researchers(researcherIdentifiers)));

		List<Target> targets = getTargets(task);
		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS,
				TargetsValues.create(new Targets(targets)));

		logger.info(
				"Starting data-set reception and merging of approved data sharing project [project-identifier: {} ; contract-location: {} ; extraction-interval: {} ; researchers: {} ; medics: {}]",
				projectIdentifier, contractLocation, extractionInterval, String.join(",", researcherIdentifiers),
				targets.stream().map(Target::getOrganizationIdentifierValue).collect(Collectors.joining(",")));
	}

	private String getProjectIdentifier(Task task)
	{
		return task.getInput().stream().filter(i -> i.getType().getCoding().stream()
				.anyMatch(c -> ConstantsDataSharing.CODESYSTEM_DATA_SHARING.equals(c.getSystem())
						&& ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER.equals(c.getCode())))
				.filter(i -> i.getValue() instanceof Identifier).map(i -> (Identifier) i.getValue())
				.filter(i -> ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).findFirst().orElseThrow(() -> new RuntimeException(
						"No project-identifier present in task with id='" + task.getId() + "'"));
	}

	private String getContractLocation(Task task)
	{
		return getTaskHelper()
				.getFirstInputParameterUrlValue(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION)
				.map(UrlType::getValue).orElseThrow(() -> new RuntimeException(
						"No contract-location present in task with id='" + task.getId() + "'"));
	}

	private String getExtractionInterval(Task task)
	{
		return getTaskHelper()
				.getFirstInputParameterStringValue(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_EXTRACTION_INTERVAL)
				.orElseThrow(() -> new RuntimeException("No extraction interval provided by HRP"));
	}

	private List<String> getResearcherIdentifiers(Task task)
	{
		return task.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> ConstantsDataSharing.CODESYSTEM_DATA_SHARING.equals(c.getSystem())
								&& ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER
										.equals(c.getCode())))
				.filter(i -> i.getValue() instanceof Identifier).map(i -> (Identifier) i.getValue())
				.filter(i -> ConstantsDataSharing.NAMINGSYSTEM_RESEARCHER_IDENTIFIER.equals(i.getSystem()))
				.map(Identifier::getValue).collect(Collectors.toList());
	}

	private List<Target> getTargets(Task task)
	{
		return getTaskHelper()
				.getInputParameterWithExtension(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY,
						ConstantsDataSharing.EXTENSION_URL_MEDIC_IDENTIFIER)
				.map(this::transformMedicCorrelationKeyInputToTarget).collect(Collectors.toList());
	}

	private Target transformMedicCorrelationKeyInputToTarget(Task.ParameterComponent input)
	{
		String organizationIdentifier = ((Reference) input
				.getExtensionByUrl(ConstantsDataSharing.EXTENSION_URL_MEDIC_IDENTIFIER).getValue()).getIdentifier()
						.getValue();
		String correlationKey = ((StringType) input.getValue()).asStringValue();

		return endpointProvider.getFirstConsortiumEndpoint(
				ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE,
				ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_MEDIC, organizationIdentifier)
				.map(e -> Target.createBiDirectionalTarget(organizationIdentifier, e.getIdentifierFirstRep().getValue(),
						e.getAddress(), correlationKey))
				.orElseThrow(() -> new RuntimeException(
						"No endpoint of found for organization='" + organizationIdentifier + "'"));
	}
}
