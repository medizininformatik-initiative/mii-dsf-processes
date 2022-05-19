package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_HEALTH_RESEARCH_PLATFORM_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;

import java.util.Objects;
import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

public class SelectTargetHrp extends AbstractServiceDelegate implements InitializingBean
{
	private final EndpointProvider endpointProvider;

	public SelectTargetHrp(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task task = getLeadingTaskFromExecutionVariables();
		String hrpIdentifier = getHrpOrganizationIdentifier(task);
		Endpoint hrpEndpoint = getHrpEndpoint(hrpIdentifier);
		Target hrpTarget = createTarget(hrpIdentifier, hrpEndpoint);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(hrpTarget));
	}

	private String getHrpOrganizationIdentifier(Task task)
	{
		return getTaskHelper()
				.getFirstInputParameterReferenceValue(task, CODESYSTEM_MII_REPORT,
						CODESYSTEM_MII_REPORT_VALUE_HEALTH_RESEARCH_PLATFORM_IDENTIFIER)
				.filter(Reference::hasIdentifier).map(Reference::getIdentifier).map(Identifier::getValue)
				.orElseThrow(() -> new RuntimeException("HRP identifier missing"));
	}

	private Endpoint getHrpEndpoint(String hrpIndentifier)
	{
		return endpointProvider.getFirstDefaultEndpoint(hrpIndentifier).orElseThrow(
				() -> new RuntimeException("Could not find default endpoint of organization '" + hrpIndentifier + "'"));
	}

	private Target createTarget(String hrpIndentifier, Endpoint hrpEndpoint)
	{
		String endpointIdentifier = getEndpointIdentifierValue(hrpEndpoint);
		return Target.createUniDirectionalTarget(hrpIndentifier, endpointIdentifier, hrpEndpoint.getAddress());
	}

	private String getEndpointIdentifierValue(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).findFirst()
				.map(Identifier::getValue).get();
	}
}
