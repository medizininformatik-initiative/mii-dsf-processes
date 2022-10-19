package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.RsaAesGcmUtil;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class DecryptDataSet extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DecryptDataSet.class);

	private final OrganizationProvider organizationProvider;
	private final KeyProvider keyProvider;
	private final DataLogger dataLogger;

	public DecryptDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, KeyProvider keyProvider,
			DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.keyProvider = keyProvider;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(keyProvider, "keyProvider");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getCurrentTaskFromExecutionVariables(execution);
		String sendingOrganization = task.getRequester().getIdentifier().getValue();
		String localOrganization = organizationProvider.getLocalIdentifierValue();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);

		logger.info("Decrypting data-set from organization '{}' for data-sharing project '{}' in Task with id '{}'",
				sendingOrganization, projectIdentifier, task.getId());

		try
		{
			Bundle bundleDecrypted = decryptBundle(execution, keyProvider.getPrivateKey(), bundleEncrypted,
					sendingOrganization, localOrganization);

			dataLogger.logResource("Decrypted Transfer Bundle", bundleDecrypted);

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET,
					FhirResourceValues.create(bundleDecrypted));
		}
		catch (Exception exception)
		{
			String message = "Could not decrypt data-set from organization '" + sendingOrganization
					+ "' and  data-sharing project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "' - " + exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR, message,
					exception);
		}
	}

	private Bundle decryptBundle(DelegateExecution execution, PrivateKey privateKey, byte[] bundleEncrypted,
			String sendingOrganization, String receivingOrganization)
	{
		try
		{
			byte[] bundleDecrypted = RsaAesGcmUtil.decrypt(privateKey, bundleEncrypted, sendingOrganization,
					receivingOrganization);
			String bundleString = new String(bundleDecrypted, StandardCharsets.UTF_8);
			return (Bundle) FhirContext.forR4().newXmlParser().parseResource(bundleString);
		}
		catch (Exception exception)
		{
			String taskId = getCurrentTaskFromExecutionVariables(execution).getId();
			throw new RuntimeException("Could not decrypt received data-set for task with id '" + taskId + "'");
		}
	}
}
