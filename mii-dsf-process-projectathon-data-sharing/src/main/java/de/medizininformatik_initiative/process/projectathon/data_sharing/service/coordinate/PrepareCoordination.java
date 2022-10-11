package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import java.util.List;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.variables.Researchers;
import de.medizininformatik_initiative.process.projectathon.data_sharing.variables.ResearchersValues;

public class PrepareCoordination extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(PrepareCoordination.class);

	public PrepareCoordination(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables();

		String projectIdentifier = getProjectIdentifier(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER,
				Variables.stringValue(projectIdentifier));

		String contractLocation = getContractLocation(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_CONTRACT_LOCATION,
				Variables.stringValue(contractLocation));

		List<String> researcherIdentifiers = getResearcherIdentifiers(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_RESEARCHER_IDENTIFIERS,
				ResearchersValues.create(new Researchers(researcherIdentifiers)));

		String cosIdentifier = getCosIdentifier(task);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER,
				Variables.stringValue(cosIdentifier));

		logger.info(
				"Starting coordination of approved data sharing project [project-identifier: {}, contract-location: {}, researchers: {}, cos-identifier: {}]",
				projectIdentifier, contractLocation, String.join(",", researcherIdentifiers), cosIdentifier);
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

	private String getCosIdentifier(Task task)
	{
		return getTaskHelper()
				.getInputParameterReferenceValues(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER)
				.filter(Reference::hasIdentifier).map(Reference::getIdentifier).map(Identifier::getValue).findFirst()
				.orElseThrow(
						() -> new RuntimeException("No COS-identifier found in Task with id='" + task.getId() + "'"));
	}
}
