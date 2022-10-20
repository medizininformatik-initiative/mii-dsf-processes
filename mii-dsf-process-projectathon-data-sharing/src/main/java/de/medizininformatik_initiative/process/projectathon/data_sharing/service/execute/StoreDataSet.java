package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.bpe.service.MailService;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class StoreDataSet extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreDataSet.class);

	private final OrganizationProvider organizationProvider;
	private final DataLogger dataLogger;
	private final MailService mailService;

	public StoreDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper, DataLogger dataLogger,
			MailService mailService)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.dataLogger = dataLogger;
		this.mailService = mailService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(dataLogger, "dataLogger");
		Objects.requireNonNull(mailService, "mailService");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);

		logger.info(
				"Storing encrypted transferable data-set for COS '{}' and data-sharing project '{}' referenced in Task with id '{}'",
				cosIdentifier, projectIdentifier, task.getId());

		try
		{
			String binaryId = storeBinary(bundleEncrypted, cosIdentifier);
			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE,
					Variables.stringValue(binaryId));

			log(projectIdentifier, cosIdentifier, binaryId, getLeadingTaskFromExecutionVariables(execution).getId());
			sendMail(projectIdentifier, cosIdentifier, binaryId);
		}
		catch (Exception exception)
		{
			String message = "Could not store encrypt transferable data-set for COS '" + cosIdentifier
					+ "' and data-sharing project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "' - " + exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR, message,
					exception);
		}
	}

	private String storeBinary(byte[] content, String cosIdentifier)
	{
		MediaType mediaType = MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM);
		String securityContext = getSecurityContext(cosIdentifier);

		try (InputStream in = new ByteArrayInputStream(content))
		{
			IdType created = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.withRetry(ConstantsDataSharing.DSF_CLIENT_RETRY_TIMES,
							ConstantsDataSharing.DSF_CLIENT_RETRY_INTERVAL_5MIN)
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

	private String getSecurityContext(String cosIdentifier)
	{
		return organizationProvider.getOrganization(cosIdentifier)
				.orElseThrow(() -> new RuntimeException("Could not find organization with id '" + cosIdentifier + "'"))
				.getIdElement().toVersionless().getValue();
	}

	private void log(String projectIdentifier, String cosIdentifier, String binaryId, String taskId)
	{
		logger.info(
				"Stored Binary with id '{}' provided for COS '{}' and data-sharing project '{}' referenced in Task with id '{}'",
				binaryId, cosIdentifier, projectIdentifier, taskId);
	}

	private void sendMail(String projectIdentifier, String cosIdentifier, String binaryId)
	{
		String subject = "Data-set provided in process '" + ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING
				+ "'";
		String message = "The data-set for data-sharing project '" + projectIdentifier + "' in process '"
				+ ConstantsDataSharing.PROCESS_NAME_FULL_EXECUTE_DATA_SHARING
				+ "' has been successfully provided for COS '" + cosIdentifier + "' at the following location:\n" + "- "
				+ binaryId;

		mailService.send(subject, message);
	}
}
