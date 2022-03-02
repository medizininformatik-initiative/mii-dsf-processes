package de.medizininformatik_initiative.processes.projectathon.data_transfer.service;

import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED;

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
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto.KeyProvider;
import de.medizininformatik_initiative.processes.projectathon.data_transfer.crypto.RsaAesGcmUtil;

public class DecryptData extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DecryptData.class);

	private final OrganizationProvider organizationProvider;
	private final KeyProvider keyProvider;

	public DecryptData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, KeyProvider keyProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.keyProvider = keyProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(keyProvider, "keyProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		byte[] bundleEncrypted = (byte[]) execution.getVariable(BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);
		String localOrganizationIdentifier = organizationProvider.getLocalIdentifierValue();
		String sendingOrganizationIdentifier = getSendingOrganizationIdentifier();

		Bundle bundleDecrypted = decryptBundle(keyProvider.getPrivateKey(), bundleEncrypted,
				sendingOrganizationIdentifier, localOrganizationIdentifier);
		logger.debug("Decrypted Bundle: {}",
				FhirContext.forR4().newXmlParser().encodeResourceToString(bundleDecrypted));

		execution.setVariable(BPMN_EXECUTION_VARIABLE_DATA_SET, FhirResourceValues.create(bundleDecrypted));
	}

	private String getSendingOrganizationIdentifier()
	{
		Reference requester = getLeadingTaskFromExecutionVariables().getRequester();

		if (requester.hasIdentifier() && requester.getIdentifier().hasValue())
			return requester.getIdentifier().getValue();
		else
			throw new IllegalArgumentException("Task is missing requester identifier");
	}

	private Bundle decryptBundle(PrivateKey privateKey, byte[] bundleEncrypted, String sendingOrganizationIdentifier,
			String receivingOrganizationIdentifier)
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
			String taskId = getLeadingTaskFromExecutionVariables().getId();
			logger.warn("Could not decrypt received data-set for task with id='{}'", taskId);
			throw new RuntimeException("Could not decrypt received data-set for task with id='" + taskId + "'");
		}
	}
}
