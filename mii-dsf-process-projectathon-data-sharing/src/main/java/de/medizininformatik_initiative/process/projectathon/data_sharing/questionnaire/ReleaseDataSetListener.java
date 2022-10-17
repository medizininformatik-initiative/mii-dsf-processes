package de.medizininformatik_initiative.process.projectathon.data_sharing.questionnaire;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.highmed.dsf.bpe.listener.DefaultUserTaskListener;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.questionnaire.QuestionnaireResponseHelper;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class ReleaseDataSetListener extends DefaultUserTaskListener implements InitializingBean
{
	private final KdsClientFactory kdsClientFactory;
	private final MailService mailService;

	public ReleaseDataSetListener(FhirWebserviceClientProvider clientProvider,
			OrganizationProvider organizationProvider, QuestionnaireResponseHelper questionnaireResponseHelper,
			TaskHelper taskHelper, ReadAccessHelper readAccessHelper, KdsClientFactory kdsClientFactory,
			MailService mailService)
	{
		super(clientProvider, organizationProvider, questionnaireResponseHelper, taskHelper, readAccessHelper);

		this.kdsClientFactory = kdsClientFactory;
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(kdsClientFactory, "kdsClientFactory");
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void beforeQuestionnaireResponseCreate(DelegateTask userTask, QuestionnaireResponse questionnaireResponse)
	{
		String projectIdentifier = (String) userTask.getExecution()
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String cosIdentifier = (String) userTask.getExecution()
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		String kdsStoreBaseUrl = kdsClientFactory.getKdsClient().getFhirBaseUrl();

		questionnaireResponse.getItem().stream()
				.filter(i -> ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_RELEASE.equals(i.getLinkId()))
				.filter(QuestionnaireResponse.QuestionnaireResponseItemComponent::hasText)
				.forEach(i -> adaptReleaseItem(i, projectIdentifier, cosIdentifier, kdsStoreBaseUrl));
	}

	@Override
	protected void afterQuestionnaireResponseCreate(DelegateTask userTask, QuestionnaireResponse questionnaireResponse)
	{
		IdType id = questionnaireResponse.getIdElement();
		id.setIdBase(getFhirWebserviceClientProvider().getLocalBaseUrl());
		String projectIdentifier = (String) userTask.getExecution()
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		String subject = "New user-task in process '" + ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING
				+ "'";
		String message = "A new user-task 'release-data-set' for data-sharing project '" + projectIdentifier
				+ "' in process '" + ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING
				+ "' is waiting for it's completion. It can be accessed using the following link: " + id.getValue();

		mailService.send(subject, message);
	}

	private void adaptReleaseItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item,
			String projectIdentifier, String cosIdentifier, String kdsStoreBaseUrl)
	{
		String finalText = replaceText(item.getText(), projectIdentifier, cosIdentifier, kdsStoreBaseUrl);
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

	private String replaceText(String toReplace, String projectIdentifier, String cosIdentifier, String kdsStoreBaseUrl)
	{
		return toReplace
				.replace(ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_PROJECT_IDENTIFIER,
						"<b>" + projectIdentifier + "</b>")
				.replace(ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_COS_IDENTIFIER,
						"<b>" + cosIdentifier + "</b>")
				.replace(ConstantsDataSharing.QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_KDS_STORE_BASE_URL,
						"<b>" + kdsStoreBaseUrl + "</b>");
	}
}
