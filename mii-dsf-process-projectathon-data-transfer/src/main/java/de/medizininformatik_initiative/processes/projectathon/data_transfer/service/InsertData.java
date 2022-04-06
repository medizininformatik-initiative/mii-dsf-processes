package de.medizininformatik_initiative.processes.projectathon.data_transfer.service;

import static java.util.stream.Collectors.toList;

import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.CODESYSTEM_MII_DATA_TRANSFER_VALUE_DOCUMENT_REFERENCE_LOCATION;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.client.KdsClientFactory;

public class InsertData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(InsertData.class);

	private final KdsClientFactory kdsClientFactory;
	private final FhirContext fhirContext;

	public InsertData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		Bundle bundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_DATA_SET);

		Bundle stored = kdsClientFactory.getKdsClient().getFhirClient().storeBundle(bundle);

		List<IdType> idsOfCreatedResources = stored.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResponse)
				.map(Bundle.BundleEntryComponent::getResponse).map(Bundle.BundleEntryResponseComponent::getLocation)
				.map(IdType::new).map(this::setIdBase).collect(toList());

		idsOfCreatedResources.stream().filter(i -> ResourceType.DocumentReference.name().equals(i.getResourceType()))
				.forEach(this::addOutputToLeadingTask);

		idsOfCreatedResources.forEach(this::toLogMessage);
	}

	private IdType setIdBase(IdType idType)
	{
		String id = idType.getValue();
		String fhirBaseUrl = kdsClientFactory.getKdsClient().getFhirBaseUrl();
		String deliminator = fhirBaseUrl.endsWith("/") ? "" : "/";
		return new IdType(fhirBaseUrl + deliminator + id);
	}

	private void toLogMessage(IdType idType)
	{
		logger.info("Stored {} with id='{}' on KDS FHIR server with baseUrl='{}'", idType.getResourceType(),
				idType.getIdPart(), idType.getBaseUrl());
	}

	private void addOutputToLeadingTask(IdType id)
	{
		getLeadingTaskFromExecutionVariables().addOutput()
				.setValue(new Reference(id.getValue()).setType(id.getResourceType())).getType().addCoding()
				.setSystem(CODESYSTEM_MII_DATA_TRANSFER)
				.setCode(CODESYSTEM_MII_DATA_TRANSFER_VALUE_DOCUMENT_REFERENCE_LOCATION);
	}
}
