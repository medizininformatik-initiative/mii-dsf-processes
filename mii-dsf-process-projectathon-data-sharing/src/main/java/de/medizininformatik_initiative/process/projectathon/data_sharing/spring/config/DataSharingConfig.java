package de.medizininformatik_initiative.process.projectathon.data_sharing.spring.config;

import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendExecuteDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendMergeDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.message.SendMergedDataSet;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.PrepareCoordination;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.SelectCosTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate.SelectDicTargets;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.PrepareExecution;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute.SelectDataSetTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.SelectHrpTarget;
import de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge.StoreCorrelationKeys;

@Configuration
@ComponentScan(basePackages = "de.medizininformatik_initiative")
public class DataSharingConfig
{
	@Autowired
	private FhirWebserviceClientProvider clientProvider;

	@Autowired
	private TaskHelper taskHelper;

	@Autowired
	private ReadAccessHelper readAccessHelper;

	@Autowired
	private OrganizationProvider organizationProvider;

	@Autowired
	private EndpointProvider endpointProvider;

	@Autowired
	private FhirContext fhirContext;

	// COORDINATE DATA SHARING PROCESS

	@Bean
	public PrepareCoordination prepareCoordination()
	{
		return new PrepareCoordination(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SelectDicTargets selectDicTargets()
	{
		return new SelectDicTargets(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SelectCosTarget selectCosTarget()
	{
		return new SelectCosTarget(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SendMergeDataSharing sendMergeDataSharing()
	{
		return new SendMergeDataSharing(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	@Bean
	public SendExecuteDataSharing sendExecuteDataSharing()
	{
		return new SendExecuteDataSharing(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				fhirContext);
	}

	// EXECUTE DATA SHARING PROCESS

	@Bean
	public PrepareExecution prepareExecution()
	{
		return new PrepareExecution(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SelectDataSetTarget selectDataSetTarget()
	{
		return new SelectDataSetTarget(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SendDataSet sendDataSet()
	{
		return new SendDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	// MERGE DATA SHARING PROCESS

	@Bean
	public StoreCorrelationKeys storeCorrelationKeys()
	{
		return new StoreCorrelationKeys(clientProvider, taskHelper, readAccessHelper);
	}

	@Bean
	public SelectHrpTarget selectHrpTarget()
	{
		return new SelectHrpTarget(clientProvider, taskHelper, readAccessHelper, organizationProvider,
				endpointProvider);
	}

	@Bean
	public SendMergedDataSet sendMergedDataSet()
	{
		return new SendMergedDataSet(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}
}
