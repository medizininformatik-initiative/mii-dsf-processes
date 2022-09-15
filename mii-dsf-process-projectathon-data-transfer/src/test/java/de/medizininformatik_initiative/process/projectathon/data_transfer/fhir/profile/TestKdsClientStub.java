package de.medizininformatik_initiative.process.projectathon.data_transfer.fhir.profile;

import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.processes.kds.client.KdsClient;
import de.medizininformatik_initiative.processes.kds.client.KdsClientStub;

public class TestKdsClientStub
{
	@Test
	public void testKdsClientStubBinary()
	{
		KdsClient kdsClient = createKdsClientStub();
		Resource binary = kdsClient.readByIdType(new IdType().withResourceType("Binary"));

		assertTrue(binary instanceof Binary);
	}

	@Test
	public void testKdsClientStubBundle()
	{
		KdsClient kdsClient = createKdsClientStub();
		Resource binary = kdsClient.readByIdType(new IdType().withResourceType("Bundle"));

		assertTrue(binary instanceof Bundle);
	}

	private KdsClient createKdsClientStub()
	{
		return new KdsClientStub(FhirContext.forR4(), "Test_DIC");
	}
}
