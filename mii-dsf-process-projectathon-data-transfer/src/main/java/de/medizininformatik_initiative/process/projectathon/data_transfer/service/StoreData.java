package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class StoreData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreData.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;
	private final DataLogger dataLogger;
	private final MailService mailService;

	public StoreData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
			EndpointProvider endpointProvider, DataLogger dataLogger, MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.endpointProvider = endpointProvider;
		this.dataLogger = dataLogger;
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(dataLogger, "dataLogger");
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);
		String coordinatingSiteIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		try
		{
			String binaryId = storeBinary(bundleEncrypted, coordinatingSiteIdentifier);
			execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE,
					Variables.stringValue(binaryId));

			log(projectIdentifier, coordinatingSiteIdentifier, binaryId, task.getId());
			sendMail(projectIdentifier, coordinatingSiteIdentifier, binaryId);

			Target target = createTarget(coordinatingSiteIdentifier);
			execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
		}
		catch (Exception exception)
		{
			logger.warn(
					"Could not store data-set for COS '{}' and data-transfer project '{}' referenced in Task with id '{}' - {}",
					coordinatingSiteIdentifier, projectIdentifier, task.getId(), exception.getMessage());
			throw new RuntimeException(
					"Could not store data-set for COS '" + coordinatingSiteIdentifier + "' and data-transfer project '"
							+ projectIdentifier + "' referenced in Task with id '" + task.getId() + "'",
					exception);
		}
	}

	private String storeBinary(byte[] content, String coordinatingSiteIdentifier)
	{
		MediaType mediaType = MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM);
		String securityContext = getSecurityContext(coordinatingSiteIdentifier);

		try (InputStream in = new ByteArrayInputStream(content))
		{
			IdType created = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.withRetry(ConstantsDataTransfer.DSF_CLIENT_RETRY_TIMES,
							ConstantsDataTransfer.DSF_CLIENT_RETRY_INTERVAL_5MIN)
					.createBinary(in, mediaType, securityContext);
			return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Binary.name(),
					created.getIdPart(), created.getVersionIdPart()).getValue();
		}
		catch (Exception exception)
		{
			logger.warn("Could not create binary - {}", exception.getMessage());
			throw new RuntimeException("Could not create binary", exception);
		}
	}

	private String getSecurityContext(String coordinatingSiteIdentifier)
	{
		return organizationProvider.getOrganization(coordinatingSiteIdentifier).orElseThrow(
				() -> new RuntimeException("Could not find organization with id '" + coordinatingSiteIdentifier + "'"))
				.getIdElement().toVersionless().getValue();
	}

	private void log(String projectIdentifier, String cosIdentifier, String binaryId, String taskid)
	{
		logger.info(
				"Stored Binary with id '{}' provided for COS '{}' and data-transfer project '{}' referenced in Task with id '{}'",
				binaryId, cosIdentifier, projectIdentifier, taskid);
	}

	private void sendMail(String projectIdentifier, String cosIdentifier, String binaryId)
	{
		String subject = "Data-set provided in process '" + ConstantsDataTransfer.PROCESS_NAME_FULL_DATA_SEND + "'";
		String message = "The data-set for data-transfer project '" + projectIdentifier + "' in process '"
				+ ConstantsDataTransfer.PROCESS_NAME_FULL_DATA_SEND + "' has been successfully provided for COS '"
				+ cosIdentifier + "' at the following location:\n" + "- " + binaryId;

		mailService.send(subject, message);
	}

	private Target createTarget(String coordinatingSiteIdentifier)
	{
		Endpoint endpoint = getEndpoint(coordinatingSiteIdentifier);
		return Target.createUniDirectionalTarget(coordinatingSiteIdentifier, getEndpointIdentifierValue(endpoint),
				endpoint.getAddress());
	}

	private Endpoint getEndpoint(String identifier)
	{
		return endpointProvider.getFirstConsortiumEndpoint(
				NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				CODESYSTEM_HIGHMED_ORGANIZATION_ROLE, CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS, identifier)
				.orElseThrow(() -> new RuntimeException(
						"Could not find Endpoint of organization with identifier '" + identifier + "'"));
	}

	private String getEndpointIdentifierValue(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).findFirst()
				.map(Identifier::getValue).orElseThrow(() -> new RuntimeException(
						"Endpoint with id '" + endpoint.getId() + "' does not contain any identifier"));
	}
}
