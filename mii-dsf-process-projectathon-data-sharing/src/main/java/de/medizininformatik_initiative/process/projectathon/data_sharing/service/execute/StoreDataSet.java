package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class StoreDataSet extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreDataSet.class);

	private final DataLogger dataLogger;
	private final OrganizationProvider organizationProvider;

	public StoreDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper, DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dataLogger, "dataLogger");
		Objects.requireNonNull(organizationProvider, "organizationProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);

		String binaryId = storeBinary(bundleEncrypted, cosIdentifier);

		logger.info(
				"Stored Binary with id='{}' provided for COS-identifier='{}' and project-identifier='{}' referenced in Task with id='{}'",
				binaryId, cosIdentifier, projectIdentifier, getLeadingTaskFromExecutionVariables(execution).getId());

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE,
				Variables.stringValue(binaryId));
	}

	private String storeBinary(byte[] content, String cosIdentifier)
	{
		MediaType mediaType = MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM);
		String securityContext = getSecurityContext(cosIdentifier);

		try (InputStream in = new ByteArrayInputStream(content))
		{
			IdType created = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.createBinary(in, mediaType, securityContext);
			return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Binary.name(),
					created.getIdPart(), created.getVersionIdPart()).getValue();
		}
		catch (Exception exception)
		{
			logger.warn("Could not create binary - {}", exception.getMessage());
			throw new RuntimeException(exception);
		}
	}

	private String getSecurityContext(String cosIdentifier)
	{
		return organizationProvider.getOrganization(cosIdentifier)
				.orElseThrow(() -> new RuntimeException("Could not find organization with id '" + cosIdentifier + "'"))
				.getIdElement().toVersionless().getValue();
	}
}
