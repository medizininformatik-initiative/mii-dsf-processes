package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.util.MimeTypeHelper;

public class ValidateDataSetMerge extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidateDataSetMerge.class);

	private final MimeTypeHelper mimeTypeHelper;

	public ValidateDataSetMerge(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, MimeTypeHelper mimeTypeHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.mimeTypeHelper = mimeTypeHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(mimeTypeHelper, "mimeTypeHelper");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getCurrentTaskFromExecutionVariables(execution);
		String sendingOrganization = task.getRequester().getIdentifier().getValue();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Bundle bundle = (Bundle) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET);

		logger.info(
				"Validating decrypted data-set from organization '{}' for data-sharing project '{}' in Task with id '{}'",
				sendingOrganization, projectIdentifier, task.getId());

		try
		{
			validate(execution, bundle);
		}
		catch (Exception exception)
		{
			String message = "Could not validate decrypted data-set from organization '" + sendingOrganization
					+ "' and  data-sharing project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "' - " + exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR, message,
					exception);
		}
	}

	private void validate(DelegateExecution execution, Bundle bundle)
	{
		Bundle.BundleType type = bundle.getType();
		if (!TRANSACTION.equals(type))
		{
			throw new RuntimeException("Bundle is not of type Transaction (" + type + ")");
		}

		List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

		int countE = entries.size();
		if (countE != 2)
		{
			throw new RuntimeException("Bundle contains " + countE + " entries (expected 2)");
		}

		List<DocumentReference> documentReferences = entries.stream().map(Bundle.BundleEntryComponent::getResource)
				.filter(r -> r instanceof DocumentReference).map(r -> (DocumentReference) r)
				.collect(Collectors.toList());

		long countDr = documentReferences.size();
		if (countDr != 1)
		{
			throw new RuntimeException("Bundle contains " + countDr + " DocumentReferences (expected 1)");
		}

		String identifierRequester = getCurrentTaskFromExecutionVariables(execution).getRequester().getIdentifier()
				.getValue();
		String identifierAuthor = documentReferences.stream().filter(DocumentReference::hasAuthor)
				.flatMap(dr -> dr.getAuthor().stream()).filter(Reference::hasIdentifier).map(Reference::getIdentifier)
				.filter(Identifier::hasValue).map(Identifier::getValue).findFirst().orElse("no-author");
		if (!identifierAuthor.equals(identifierRequester))
		{
			throw new RuntimeException("Requester in Task does not match author in DocumentReference ("
					+ identifierRequester + " != " + identifierAuthor + ")");
		}

		List<String> projectIdentifiersDocuementReference = documentReferences.stream()
				.filter(DocumentReference::hasMasterIdentifier).map(DocumentReference::getMasterIdentifier)
				.filter(mi -> ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER.equals(mi.getSystem()))
				.map(Identifier::getValue).filter(Objects::nonNull).collect(Collectors.toList());
		long countMi = projectIdentifiersDocuementReference.size();
		if (countMi != 1)
		{
			throw new RuntimeException("DocumentReference contains " + countMi + " projectIdentifiers (expected 1)");
		}

		String projectIdentifierTask = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		if (!projectIdentifiersDocuementReference.get(0).equals(projectIdentifierTask))
		{
			throw new RuntimeException("DocumentReference and Task projectIdentifier do not match  ("
					+ projectIdentifiersDocuementReference.get(0) + " != " + projectIdentifierTask + ")");
		}

		List<Resource> resources = entries.stream().map(Bundle.BundleEntryComponent::getResource)
				.filter(r -> r != documentReferences.get(0)).collect(Collectors.toList());

		long countR = resources.size();
		if (countR != 1)
		{
			throw new RuntimeException("Bundle contains " + countR + " Resources (expected 1)");
		}

		Resource resource = resources.get(0);
		String mimeTypeR = mimeTypeHelper.getMimeType(resource);
		byte[] dataR = mimeTypeHelper.getData(resource);
		mimeTypeHelper.validate(dataR, mimeTypeR);
	}
}
