package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_transfer.crypto.RsaAesGcmUtil;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class DecryptData extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DecryptData.class);

	private final OrganizationProvider organizationProvider;
	private final KeyProvider keyProvider;
	private final DataLogger dataLogger;

	public DecryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);
		String localOrganizationIdentifier = organizationProvider.getLocalIdentifierValue();
		String sendingOrganizationIdentifier = getSendingOrganizationIdentifier(execution);

		Bundle bundleDecrypted = decryptBundle(execution, keyProvider.getPrivateKey(), bundleEncrypted,
				sendingOrganizationIdentifier, localOrganizationIdentifier);

		dataLogger.logResource("Decrypted Transfer Bundle", bundleDecrypted);

		execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET,
				FhirResourceValues.create(bundleDecrypted));
	}

	private String getSendingOrganizationIdentifier(DelegateExecution execution)
	{
		return getLeadingTaskFromExecutionVariables(execution).getRequester().getIdentifier().getValue();
	}

	private Bundle decryptBundle(DelegateExecution execution, PrivateKey privateKey, byte[] bundleEncrypted,
			String sendingOrganizationIdentifier, String receivingOrganizationIdentifier)
	{
		try
		{
			byte[] bundleDecrypted = RsaAesGcmUtil.decrypt(privateKey, bundleEncrypted, sendingOrganizationIdentifier,
					receivingOrganizationIdentifier);
			String bundleString = new String(bundleDecrypted, StandardCharsets.UTF_8);
			return (Bundle) FhirContext.forR4().newXmlParser().parseResource(bundleString);
		}
		catch (Exception exception)
		{
			String taskId = getLeadingTaskFromExecutionVariables(execution).getId();
			logger.warn("Could not decrypt received data-set for Task with id '{}' - {}", taskId,
					exception.getMessage());
			throw new RuntimeException("Could not decrypt received data-set for Task with id '" + taskId + "'",
					exception);
		}
	}
}
