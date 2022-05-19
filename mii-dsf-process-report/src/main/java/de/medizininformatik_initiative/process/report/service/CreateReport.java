package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Collections;
import java.util.Map;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;

public class CreateReport extends AbstractServiceDelegate
{
	public CreateReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution) throws BpmnError, Exception
	{
		// TODO: implement, use dummy value at the moment
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.BATCHRESPONSE);
		bundle.getIdentifier().setSystem(CODESYSTEM_MII_REPORT).setValue("Report|Test_DIC");

		// TODO: end

		Target target = (Target) execution.getVariable(BPMN_EXECUTION_VARIABLE_TARGET);
		getReadAccessHelper().addOrganization(bundle, target.getOrganizationIdentifierValue());

		IdType bundleIdType = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
				.updateConditionaly(bundle, Map.of("identifier", Collections.singletonList("Report|Test_DIC")));
		String bundleId = new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Bundle.name(),
				bundleIdType.getIdPart(), bundleIdType.getVersionIdPart()).getValue();

		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE,
				Variables.stringValue(bundleId));
	}
}
