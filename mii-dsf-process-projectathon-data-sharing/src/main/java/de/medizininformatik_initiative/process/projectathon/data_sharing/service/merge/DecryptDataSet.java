package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

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
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.KeyProvider;
import de.medizininformatik_initiative.process.projectathon.data_sharing.crypto.RsaAesGcmUtil;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class DecryptDataSet extends AbstractServiceDelegate implements InitializingBean
{
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
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);
		String localOrganizationIdentifier = organizationProvider.getLocalIdentifierValue();
		String sendingOrganizationIdentifier = getSendingOrganizationIdentifier();

		Bundle bundleDecrypted = decryptBundle(keyProvider.getPrivateKey(), bundleEncrypted,
				sendingOrganizationIdentifier, localOrganizationIdentifier);

		dataLogger.logResource("Decrypted Transfer Bundle", bundleDecrypted);

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET,
				FhirResourceValues.create(bundleDecrypted));
	}

	private String getSendingOrganizationIdentifier()
	{
		Reference requester = getCurrentTaskFromExecutionVariables().getRequester();

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
			String taskId = getCurrentTaskFromExecutionVariables().getId();
			throw new RuntimeException("Could not decrypt received data-set for task with id='" + taskId + "'");
		}
	}
}