package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.KdsClient;
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
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		String organizationIdentifier = getCurrentTaskFromExecutionVariables(execution).getRequester().getIdentifier()
				.getValue();

		try
		{
			KdsClient kdsClient = kdsClientFactory.getKdsClient();

			logger.info(
					"Inserting data-set on FHIR server with baseUrl='{}' received from organization='{}' for project-identifier='{}'",
					kdsClient.getFhirBaseUrl(), organizationIdentifier, projectIdentifier);

			Bundle bundle = (Bundle) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET);
			Bundle stored = kdsClient.executeTransactionBundle(bundle);

			List<IdType> idsOfCreatedResources = stored.getEntry().stream()
					.filter(Bundle.BundleEntryComponent::hasResponse).map(Bundle.BundleEntryComponent::getResponse)
					.map(Bundle.BundleEntryResponseComponent::getLocation).map(IdType::new).map(this::setIdBase)
					.collect(toList());

			idsOfCreatedResources.stream()
					.filter(i -> ResourceType.DocumentReference.name().equals(i.getResourceType()))
					.forEach(i -> addOutputToCurrentTask(execution, i));

			idsOfCreatedResources.forEach(id -> toLogMessage(id, organizationIdentifier, projectIdentifier));

			Targets targets = (Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS);
			removeOrganizationFromTargets(execution, targets, organizationIdentifier);
		}
		catch (Exception exception)
		{
			logger.error(
					"Could not insert data-set received from organization='{}' for project-identifier='{}', error-message='{}'",
					organizationIdentifier, projectIdentifier, exception.getMessage());

			// TODO stop current subprocess execution
		}

	}

	private IdType setIdBase(IdType idType)
	{
		String id = idType.getValue();
		String fhirBaseUrl = kdsClientFactory.getKdsClient().getFhirBaseUrl();
		String deliminator = fhirBaseUrl.endsWith("/") ? "" : "/";
		return new IdType(fhirBaseUrl + deliminator + id);
	}

	private void addOutputToCurrentTask(DelegateExecution execution, IdType id)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);

		task.addOutput().setValue(new Reference(id.getValue()).setType(id.getResourceType())).getType().addCoding()
				.setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DOCUMENT_REFERENCE_REFERENCE);

		updateLeadingTaskInExecutionVariables(execution, task);
	}

	private void toLogMessage(IdType idType, String sendingOrganization, String projectIdentifier)
	{
		logger.info(
				"Stored {} with id='{}' on FHIR server with baseUrl='{}' received from organization='{}' for project-identifier='{}'",
				idType.getResourceType(), idType.getIdPart(), idType.getBaseUrl(), sendingOrganization,
				projectIdentifier);
	}

	private void removeOrganizationFromTargets(DelegateExecution execution, Targets targets,
			String organizationIdentifier)
	{
		List<Target> targetsWithoutReceivedIdentifier = targets.getEntries().stream()
				.filter(t -> !organizationIdentifier.equals(t.getOrganizationIdentifierValue()))
				.collect(Collectors.toList());
		execution.setVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS,
				TargetsValues.create(new Targets(targetsWithoutReceivedIdentifier)));
	}
}
