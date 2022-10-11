package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
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
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		QuestionnaireResponse questionnaireResponse = (QuestionnaireResponse) execution
				.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_QUESTIONNAIRE_RESPONSE_COMPLETED);

		// Validity of the URL to access the merged data-set is checked as part of the default javascript code
		String dataSetUrl = getDataSetUrl(questionnaireResponse);
		storeDataSetUrlAsTaskOutput(dataSetUrl);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_LOCATION,
				Variables.stringValue(dataSetUrl));

		if (projectIdentifierMatch(questionnaireResponse, projectIdentifier))
		{
			logger.info("Released data-set provided for project-identifier='{}' referenced in Task with id='{}'",
					projectIdentifier, getLeadingTaskFromExecutionVariables().getId());
			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_RELEASED, true);
		}
		else
		{
			logger.warn(
					"Could not release data-set project-identifier='{}' referenced in Task with id='{}': expected and provided project identifier do not match ({}/{}), restarting user task",
					projectIdentifier, getLeadingTaskFromExecutionVariables().getId(), projectIdentifier.toLowerCase(),
					getProvidedProjectIdentifierAsLowerCase(questionnaireResponse));
			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_RELEASED, false);
		}
	}

	private String getDataSetUrl(QuestionnaireResponse questionnaireResponse)
	{
		return questionnaireResponse.getItem().stream()
				.filter(i -> ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DATA_SET_URL
						.equals(i.getLinkId()))
				.flatMap(i -> i.getAnswer().stream())
				.map(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent::getValue)
				.filter(a -> UriType.class.isAssignableFrom(a.getClass())).map(a -> (UriType) a)
				.map(PrimitiveType::getValue).findFirst()
				.orElseThrow(() -> new RuntimeException("QuestionnaireResponse with id " + questionnaireResponse.getId()
						+ "is missing answer for linkId "
						+ ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DATA_SET_URL));
	}

	private void storeDataSetUrlAsTaskOutput(String dataSetUrl)
	{
		Task leadingTask = getLeadingTaskFromExecutionVariables();
		Task.TaskOutputComponent dataSetLocationOutput = new Task.TaskOutputComponent();
		dataSetLocationOutput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);
		dataSetLocationOutput.setValue(new UrlType().setValue(dataSetUrl));

		leadingTask.addOutput(dataSetLocationOutput);
		updateLeadingTaskInExecutionVariables(leadingTask);
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
				.map(a -> (StringType) a.getValue()).map(PrimitiveType::getValue).map(String::toLowerCase);
	}
}
