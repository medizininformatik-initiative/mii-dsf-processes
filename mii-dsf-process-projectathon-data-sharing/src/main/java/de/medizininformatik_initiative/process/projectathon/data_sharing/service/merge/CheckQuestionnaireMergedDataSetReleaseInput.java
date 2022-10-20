package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class CheckQuestionnaireMergedDataSetReleaseInput extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(CheckQuestionnaireMergedDataSetReleaseInput.class);

	public CheckQuestionnaireMergedDataSetReleaseInput(FhirWebserviceClientProvider clientProvider,
			TaskHelper taskHelper, ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) execution
				.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_QUESTIONNAIRE_RESPONSE_COMPLETED);

		Optional<String> dataSetUrl = getDataSetUrl(questionnaireResponse);

		if (projectIdentifierMatch(questionnaireResponse, projectIdentifier) && dataSetUrl.isPresent())
		{
			storeDataSetUrlAsTaskOutput(task, dataSetUrl.get());
			updateLeadingTaskInExecutionVariables(execution, task);
			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_LOCATION,
					Variables.stringValue(dataSetUrl.get()));

			logger.info(
					"Released merged data-set for HRP and data-sharing project '{}' referenced in Task with id '{}'",
					projectIdentifier, task.getId());
		}
		else
		{
			String message = "Could not release merged data-set for HRP and data-sharing project '" + projectIdentifier
					+ "' referenced in Task with id '" + task.getId()
					+ "': expected and provided project identifier do not match (" + projectIdentifier.toLowerCase()
					+ "/" + getProvidedProjectIdentifierAsLowerCase(questionnaireResponse)
					+ ") or QuestionnaireResponse with id '"
					+ getDsfFhirServerAbsoluteId(questionnaireResponse.getIdElement())
					+ "' is missing item with linkId '"
					+ ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DATA_SET_URL + "'";

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR, message);
		}
	}

	private Optional<String> getDataSetUrl(QuestionnaireResponse questionnaireResponse)
	{
		return questionnaireResponse.getItem().stream()
				.filter(i -> ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DATA_SET_URL
						.equals(i.getLinkId()))
				.filter(QuestionnaireResponse.QuestionnaireResponseItemComponent::hasAnswer)
				.flatMap(i -> i.getAnswer().stream())
				.filter(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent::hasValue)
				.map(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent::getValue)
				.filter(a -> UriType.class.isAssignableFrom(a.getClass())).map(a -> (UriType) a)
				.filter(PrimitiveType::hasValue).map(PrimitiveType::getValue).findFirst();
	}

	private void storeDataSetUrlAsTaskOutput(Task leadingTask, String dataSetUrl)
	{
		Task.TaskOutputComponent dataSetLocationOutput = new Task.TaskOutputComponent();
		dataSetLocationOutput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);
		dataSetLocationOutput.setValue(new UrlType().setValue(dataSetUrl));

		leadingTask.addOutput(dataSetLocationOutput);
	}

	private boolean projectIdentifierMatch(QuestionnaireResponse questionnaireResponse,
			String expectedProjectIdentifier)
	{
		return getProjectIdentifiersAsLowerCase(questionnaireResponse).anyMatch(
				foundProjectIdentifier -> expectedProjectIdentifier.toLowerCase().equals(foundProjectIdentifier));
	}

	private String getProvidedProjectIdentifierAsLowerCase(QuestionnaireResponse questionnaireResponse)
	{
		return getProjectIdentifiersAsLowerCase(questionnaireResponse).findFirst().orElse("unknown");
	}

	private Stream<String> getProjectIdentifiersAsLowerCase(QuestionnaireResponse questionnaireResponse)
	{
		return questionnaireResponse.getItem().stream()
				.filter(i -> ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_RELEASE.equals(i.getLinkId()))
				.flatMap(i -> i.getAnswer().stream()).filter(a -> a.getValue() instanceof StringType)
				.map(a -> (StringType) a.getValue()).map(PrimitiveType::getValue).map(String::toLowerCase)
				.map(String::trim);
	}

	private String getDsfFhirServerAbsoluteId(IdType idType)
	{
		return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), idType.getResourceType(),
				idType.getIdPart(), null).getValue();
	}
}
