package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM;

import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class StoreData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreData.class);

	private final EndpointProvider endpointProvider;
	private final DataLogger dataLogger;

	public StoreData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, EndpointProvider endpointProvider, DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.endpointProvider = endpointProvider;
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);
		String coordinatingSiteIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		Binary binary = createBinary(bundleEncrypted, coordinatingSiteIdentifier);
		String binaryId = storeBinary(binary);

		logger.info(
				"Stored Binary with id='{}' provided for COS-identifier='{}' and project-identifier='{}' referenced in Task with id='{}'",
				binaryId, coordinatingSiteIdentifier, projectIdentifier,
				getLeadingTaskFromExecutionVariables().getId());

		Target target = createTarget(coordinatingSiteIdentifier);

		execution.setVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE,
				Variables.stringValue(binaryId));
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private Binary createBinary(byte[] content, String coordinatingSiteIdentifier)
	{
		Reference securityContext = new Reference();
		securityContext.setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(coordinatingSiteIdentifier);
		Binary binary = new Binary().setContentType(MediaType.APPLICATION_OCTET_STREAM)
				.setSecurityContext(securityContext).setData(content);

		dataLogger.logResource("Encrypted Binary", binary);

		return binary;
	}

	private String storeBinary(Binary binary)
	{
		IdType created = createBinaryResource(binary);
		return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Binary.name(),
				created.getIdPart(), created.getVersionIdPart()).getValue();
	}

	private IdType createBinaryResource(Binary binary)
	{
		return getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().create(binary);
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
				CODESYSTEM_HIGHMED_ORGANIZATION_ROLE, CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS, identifier).get();
	}

	private String getEndpointIdentifierValue(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).findFirst()
				.map(Identifier::getValue).get();
	}
}
