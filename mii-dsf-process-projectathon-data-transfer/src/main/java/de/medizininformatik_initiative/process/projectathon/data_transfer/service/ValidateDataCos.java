package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.util.MimeTypeHelper;

public class ValidateDataCos extends AbstractServiceDelegate implements InitializingBean
{
	private final MimeTypeHelper mimeTypeHelper;

	public ValidateDataCos(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		Bundle bundle = (Bundle) execution.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET);

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

		String identifierRequester = getLeadingTaskFromExecutionVariables().getRequester().getIdentifier().getValue();
		String identifierAuthor = documentReferences.stream().filter(DocumentReference::hasAuthor)
				.flatMap(dr -> dr.getAuthor().stream()).filter(Reference::hasIdentifier).map(Reference::getIdentifier)
				.filter(Identifier::hasValue).map(Identifier::getValue).findFirst().orElse("no-author");
		if (!identifierAuthor.equals(identifierRequester))
		{
			throw new RuntimeException("Requester in Task does not match author in DocumentReference ("
					+ identifierRequester + " != " + identifierAuthor + ")");
		}

		long countMi = documentReferences.stream().filter(DocumentReference::hasMasterIdentifier)
				.map(DocumentReference::getMasterIdentifier)
				.filter(mi -> ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER.equals(mi.getSystem()))
				.map(Identifier::getValue).filter(Objects::nonNull).count();
		if (countMi != 1)
		{
			throw new RuntimeException("DocumentReference contains " + countMi + " project-identifiers (expected 1)");
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
