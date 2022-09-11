package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import static java.util.stream.Collectors.toList;

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
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class InsertDataSet extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(InsertDataSet.class);

	private final KdsClientFactory kdsClientFactory;
	private final FhirContext fhirContext;

	public InsertDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		Bundle bundle = (Bundle) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET);

		Bundle stored = kdsClientFactory.getKdsClient().executeTransactionBundle(bundle);

		List<IdType> idsOfCreatedResources = stored.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResponse)
				.map(Bundle.BundleEntryComponent::getResponse).map(Bundle.BundleEntryResponseComponent::getLocation)
				.map(IdType::new).map(this::setIdBase).collect(toList());

		idsOfCreatedResources.stream().filter(i -> ResourceType.DocumentReference.name().equals(i.getResourceType()))
				.forEach(this::addOutputToCurrentTask);

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

	private void addOutputToCurrentTask(IdType id)
	{
		getLeadingTaskFromExecutionVariables().addOutput()
				.setValue(new Reference(id.getValue()).setType(id.getResourceType())).getType().addCoding()
				.setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DOCUMENT_REFERENCE_REFERENCE);
	}
}