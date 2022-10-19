package de.medizininformatik_initiative.process.projectathon.data_sharing.message;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.ConstantsBase;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.variables.Researchers;
import de.medizininformatik_initiative.processes.kds.client.KdsClientFactory;

public class SendInitializeNewProjectDataSharing extends AbstractTaskMessageSend implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(SendInitializeNewProjectDataSharing.class);

	private final FhirContext fhirContext;
	private final KdsClientFactory kdsClientFactory;

	public SendInitializeNewProjectDataSharing(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext,
			KdsClientFactory kdsClientFactory)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);

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
	protected void sendTask(DelegateExecution execution, Target target, String instantiatesUri, String messageName,
			String businessKey, String profile, Stream<Task.ParameterComponent> additionalInputParameters)
	{
		try
		{
			Task task = createTask(profile, instantiatesUri, messageName, businessKey);
			additionalInputParameters.forEach(task::addInput);
			MethodOutcome outcome = kdsClientFactory.getKdsClient().createResource(task);

			if (!outcome.getCreated())
			{
				String message = fhirContext.newJsonParser().encodeResourceToString(outcome.getOperationOutcome());
				throw new RuntimeException(message);
			}
			else
				logger.debug("Initializednew DMS project instance with task-id '{}'", outcome.getId());
		}
		catch (Exception exception)
		{
			logger.warn("Could not initialize new DMS project instance {}", exception.getMessage());
		}
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Task.ParameterComponent projectIdentifierInput = getProjectIdentifierInput(projectIdentifier);

		String contractLocation = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_CONTRACT_LOCATION);
		Task.ParameterComponent contractLocationInput = getContractLocationInput(contractLocation);

		Stream<Task.ParameterComponent> otherInputs = Stream.of(projectIdentifierInput, contractLocationInput);

		List<String> researcherIdentifiers = ((Researchers) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_RESEARCHER_IDENTIFIERS)).getEntries();
		Stream<Task.ParameterComponent> researcherIdentifierInputs = getResearcherIdentifierInputs(
				researcherIdentifiers);

		Targets targets = (Targets) execution.getVariable(ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS);
		Stream<Task.ParameterComponent> medicIdentifierInputs = getMedicIdentifierInputs(targets);

		return Stream.of(medicIdentifierInputs, otherInputs, researcherIdentifierInputs).reduce(Stream::concat)
				.orElseThrow(() -> new RuntimeException("Could not concat streams"));
	}

	private Task.ParameterComponent getProjectIdentifierInput(String projectIdentifier)
	{
		Task.ParameterComponent projectIdentifierInput = new Task.ParameterComponent();
		projectIdentifierInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER);
		projectIdentifierInput.setValue(new Identifier().setSystem(ConstantsDataSharing.NAMINGSYSTEM_PROJECT_IDENTIFIER)
				.setValue(projectIdentifier));

		return projectIdentifierInput;
	}

	private Task.ParameterComponent getContractLocationInput(String contractLocation)
	{
		Task.ParameterComponent contractLocationInput = new Task.ParameterComponent();
		contractLocationInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION);
		contractLocationInput.setValue(new UrlType(contractLocation));

		return contractLocationInput;
	}

	private Stream<Task.ParameterComponent> getResearcherIdentifierInputs(List<String> researchers)
	{
		return researchers.stream().map(this::transformToResearcherInput);
	}

	private Task.ParameterComponent transformToResearcherInput(String researcherIdentifier)
	{
		Task.ParameterComponent researcherIdentifierInput = new Task.ParameterComponent();
		researcherIdentifierInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER);
		researcherIdentifierInput.setValue(new Identifier()
				.setSystem(ConstantsDataSharing.NAMINGSYSTEM_RESEARCHER_IDENTIFIER).setValue(researcherIdentifier));

		return researcherIdentifierInput;
	}

	private Stream<Task.ParameterComponent> getMedicIdentifierInputs(Targets targets)
	{
		return targets.getEntries().stream().map(this::transformToInput);
	}

	private Task.ParameterComponent transformToInput(Target target)
	{
		Task.ParameterComponent medicIdentifierInput = new Task.ParameterComponent();
		medicIdentifierInput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER);
		medicIdentifierInput
				.setValue(new Reference()
						.setIdentifier(
								new Identifier().setSystem(ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
										.setValue(target.getOrganizationIdentifierValue()))
						.setType(ResourceType.Organization.name()));

		return medicIdentifierInput;
	}

	private Task createTask(String profile, String instantiatesUri, String messageName, String businessKey)
	{
		Task task = new Task();
		task.setMeta((new Meta()).addProfile(profile));
		task.setStatus(Task.TaskStatus.REQUESTED);
		task.setIntent(Task.TaskIntent.ORDER);
		task.setAuthoredOn(new Date());

		task.setRequester(this.getRequester());
		task.getRestriction().addRecipient(this.getRequester());

		task.setInstantiatesUri(instantiatesUri);

		Task.ParameterComponent messageNameInput = new Task.ParameterComponent(
				new CodeableConcept(new Coding(ConstantsBase.CODESYSTEM_HIGHMED_BPMN,
						ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME, null)),
				new StringType(messageName));
		task.getInput().add(messageNameInput);

		Task.ParameterComponent businessKeyInput = new Task.ParameterComponent(
				new CodeableConcept(new Coding(ConstantsBase.CODESYSTEM_HIGHMED_BPMN,
						ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY, null)),
				new StringType(businessKey));
		task.getInput().add(businessKeyInput);

		return task;
	}
}
