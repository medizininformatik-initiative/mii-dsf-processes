package de.medizininformatik_initiative.processes.projectathon.data_transfer.service;

import static java.util.stream.Collectors.toList;

import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_COORDINATING_SITE_IDENTIFIER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.NAMINGSYSTEM_MII_PROJECT_IDENTIFIER;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.KdsClientFactory;

public class ReadData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(ReadData.class);

	private final FhirContext fhirContext;
	private final KdsClientFactory kdsClientFactory;

	public ReadData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, FhirContext fhirContext, KdsClientFactory kdsClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.fhirContext = fhirContext;
		this.kdsClientFactory = kdsClientFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(kdsClientFactory, "kdsClientFactory");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables();
		String projectIdentifier = getProjectIdentifier(task);
		String coordinatingSiteIdentifier = getCoordinatingSiteIdentifier(task);

		DocumentReference documentReference = readDocumentReference(projectIdentifier, task.getId());
		logger.debug("Read DocumentReference: {}",
				FhirContext.forR4().newXmlParser().encodeResourceToString(documentReference));

		Binary binary = readBinary(documentReference, task.getId());
		logger.debug("Read Binary: {}", FhirContext.forR4().newXmlParser().encodeResourceToString(binary));

		execution.setVariable(BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER, projectIdentifier);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER, coordinatingSiteIdentifier);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE, FhirResourceValues.create(documentReference));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_BINARY, FhirResourceValues.create(binary));
	}

	private String getProjectIdentifier(Task task)
	{
		List<String> identifiers = getTaskHelper()
				.getInputParameterReferenceValues(task, CODESYSTEM_MII_DATA_TRANSFER,
						CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER)
				.filter(Reference::hasIdentifier)
				.filter(i -> NAMINGSYSTEM_MII_PROJECT_IDENTIFIER.equals(i.getIdentifier().getSystem()))
				.map(i -> i.getIdentifier().getValue()).collect(toList());

		if (identifiers.size() < 1)
			throw new IllegalArgumentException("No project identifier present in task with id='" + task.getId() + "'");

		if (identifiers.size() > 1)
			logger.warn("Found > 1 project identifiers ({}) in task with id='{}', using only the first",
					identifiers.size(), task.getId());

		return identifiers.get(0);
	}

	private String getCoordinatingSiteIdentifier(Task task)
	{
		return getTaskHelper()
				.getFirstInputParameterReferenceValue(task, CODESYSTEM_MII_DATA_TRANSFER,
						CODESYSTEM_MII_DATA_TRANSFER_VALUE_COORDINATING_SITE_IDENTIFIER)
				.orElseThrow(() -> new IllegalArgumentException(
						"No coordinating site identifier present in task, this should have been caught by resource validation"))
				.getIdentifier().getValue();
	}

	private DocumentReference readDocumentReference(String projectIdentifier, String taskId)
	{
		List<DocumentReference> documentReferences = kdsClientFactory.getKdsClient().getFhirClient()
				.searchDocumentReferences(NAMINGSYSTEM_MII_PROJECT_IDENTIFIER, projectIdentifier).getEntry().stream()
				.map(Bundle.BundleEntryComponent::getResource).filter(r -> r instanceof DocumentReference)
				.map(r -> ((DocumentReference) r)).collect(toList());

		if (documentReferences.size() < 1)
			throw new IllegalArgumentException("Could not find any DocumentReference for project-identifier='"
					+ projectIdentifier + "' referenced in task with id='" + taskId + "'");

		if (documentReferences.size() > 1)
			logger.warn(
					"Found > 1 DocumentReferences ({}) for project-identifier='{}' referenced in task with id='{}', using only the first",
					documentReferences.size(), projectIdentifier, taskId);

		return documentReferences.get(0);
	}

	private Binary readBinary(DocumentReference documentReference, String taskId)
	{
		List<Binary> binaries = Stream.of(documentReference).filter(DocumentReference::hasContent)
				.flatMap(dr -> dr.getContent().stream()).map(c -> c.getAttachment().getUrl()).map(this::readBinary)
				.collect(toList());

		if (binaries.size() < 1)
			throw new IllegalArgumentException("Could not find any Binary from DocumentReference with id='"
					+ documentReference.getId() + "' belonging to task with id='" + taskId + "'");

		if (binaries.size() > 1)
			logger.warn(
					"Found > 1 Binaries ({}) from DocumentReference with id='{}' belonging to task with id='{}', using only the first",
					binaries.size(), documentReference.getId(), taskId);

		return binaries.get(0);
	}

	private Binary readBinary(String url)
	{
		return kdsClientFactory.getKdsClient().getFhirClient().readBinary(url);
	}
}
