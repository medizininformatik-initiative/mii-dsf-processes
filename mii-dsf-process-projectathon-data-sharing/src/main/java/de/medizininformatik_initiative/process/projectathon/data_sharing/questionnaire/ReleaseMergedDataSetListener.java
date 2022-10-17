package de.medizininformatik_initiative.process.projectathon.data_sharing.questionnaire;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.highmed.dsf.bpe.listener.DefaultUserTaskListener;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.questionnaire.QuestionnaireResponseHelper;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class ReleaseMergedDataSetListener extends DefaultUserTaskListener
{
	public ReleaseMergedDataSetListener(FhirWebserviceClientProvider clientProvider,
			OrganizationProvider organizationProvider, QuestionnaireResponseHelper questionnaireResponseHelper,
			TaskHelper taskHelper, ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, organizationProvider, questionnaireResponseHelper, taskHelper, readAccessHelper);
	}

	@Override
	protected void beforeQuestionnaireResponseCreate(DelegateTask userTask, QuestionnaireResponse questionnaireResponse)
	{
		String projectIdentifier = (String) userTask.getExecution()
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		questionnaireResponse.getItem().stream()
				.filter(i -> ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_RELEASE.equals(i.getLinkId())
						|| ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DATA_SET_URL.equals(i.getLinkId()))
				.filter(QuestionnaireResponse.QuestionnaireResponseItemComponent::hasText)
				.forEach(i -> replace(i, projectIdentifier));
	}

	@Override
	protected void afterQuestionnaireResponseCreate(DelegateTask userTask, QuestionnaireResponse questionnaireResponse)
	{

	}

	private void replace(QuestionnaireResponse.QuestionnaireResponseItemComponent item, String projectIdentifier)
	{
		String finalText = replaceText(item.getText(), projectIdentifier);
		item.setText(finalText);

		item.getAnswer().stream().filter(a -> a.getValue() instanceof StringType)
				.forEach(a -> replaceAnswerStringTypePlaceholder(a, projectIdentifier));
	}

	private void replaceAnswerStringTypePlaceholder(
			QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer, String projectIdentifier)
	{
		if (answer.getValue() instanceof StringType)
			answer.setValue(new StringType(projectIdentifier));
	}

	private String replaceText(String toReplace, String projectIdentifier)
	{
		return toReplace.replace(ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_PROJECT_IDENTIFIER,
				"<b>" + projectIdentifier + "</b>");
	}
}
