package de.medizininformatik_initiative.process.projectathon.data_transfer.client.fhir;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTIONRESPONSE;
import static org.hl7.fhir.r4.model.DocumentReference.ReferredDocumentStatus.FINAL;
import static org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus.CURRENT;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;

import de.medizininformatik_initiative.process.projectathon.data_transfer.client.KdsClient;

public final class KdsFhirClientStub implements KdsFhirClient
{
	private final KdsClient kdsClient;

	public KdsFhirClientStub(KdsClient kdsClient)
	{
		this.kdsClient = kdsClient;
	}

	@Override
	public Bundle searchDocumentReferences(String system, String code)
	{
		DocumentReference documentReference = new DocumentReference().setStatus(CURRENT).setDocStatus(FINAL);
		documentReference.getMasterIdentifier().setSystem(system).setValue(code);
		documentReference.addAuthor().setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(kdsClient.getLocalIdentifierValue());
		documentReference.setDate(new Date());
		documentReference.addContent().getAttachment().setContentType("text/csv")
				.setUrl(ResourceType.Binary.name() + "/" + UUID.randomUUID().toString());
		documentReference.setId(UUID.randomUUID().toString());

		Bundle bundle = new Bundle().setType(Bundle.BundleType.SEARCHSET);
		bundle.addEntry().setResource(documentReference);

		return bundle;
	}

	@Override
	public Binary readBinary(String url)
	{
		Binary binary = new Binary().setContentType("text/csv").setData(getData());
		binary.setId(new IdType(url).getIdPart());

		return binary;
	}

	@Override
	public Bundle storeBundle(Bundle toStore)
	{
		Bundle bundle = new Bundle().setType(TRANSACTIONRESPONSE);
		bundle.setId(UUID.randomUUID().toString());

		bundle.addEntry().getResponse().setStatus("201 Created")
				.setLocation(getIdType(ResourceType.DocumentReference).getValue()).setEtag("1")
				.setLastModified(new Date());
		bundle.addEntry().getResponse().setStatus("201 Created").setLocation(getIdType(ResourceType.Binary).getValue())
				.setEtag("1").setLastModified(new Date());

		return bundle;
	}

	private IdType getIdType(ResourceType resourceType)
	{
		return new IdType(resourceType.name(), UUID.randomUUID().toString(), "1");
	}

	private byte[] getData()
	{
		return ("77u/ImdyYXBoIiwiZ2VuZXJhbGx5IiwidHlwZSIsIm1pZGRsZSIsIndoZXRoZXIiLCJiZSIsImhheSIsImdyb3c"
				+ "iLCJnYW1lIiwiZmllcmNlIiwid2VsbCIsImFtb3VudCIsInVudGlsIiwic2FmZSIsImJyb3RoZXIiLCJsb29rIi"
				+ "wiYnJvdWdodCIsInByb3BlciIsImVuZCIKImJsYWNrIiwic2NpZW50aXN0IiwiZmxhbWUiLCJlcXVhbGx5IiwiZ"
				+ "GlzY292ZXIiLCJ0aGljayIsImRpZSIsImlkZW50aXR5IiwicmVzdWx0Iiwic29tZWhvdyIsImxldCIsInZlcnRp"
				+ "Y2FsIiwiaGltIiwiZXhwZXJpZW5jZSIsInRvcCIsImx1bmdzIiwic29tZXRoaW5nIiwiaGFkIiwibW9zdCIKIm5"
				+ "lZWRsZSIsInJ1bGVyIiwiaGFkIiwid2lsbCIsImhvdCIsInRlcnJpYmxlIiwicmVtb3ZlIiwicmVjb2duaXplIi"
				+ "wiY3V0IiwiaGFsZiIsIm1pZGRsZSIsImluc3RlYWQiLCJkZXRhaWwiLCJjb3JuZXIiLCJsYWNrIiwiZnJlZSIsI"
				+ "mtpbmQiLCJjb2xkIiwiY2FrZSIKInRoZWUiLCJncm91bmQiLCJ3aGF0IiwibGFyZ2VyIiwiY29hdCIsImludGVy"
				+ "aW9yIiwib3Bwb3J0dW5pdHkiLCJwcmFjdGljYWwiLCJ0b29sIiwiZmVhdGhlcnMiLCJsZXNzb24iLCJzYXkiLCJ"
				+ "leHByZXNzaW9uIiwicHJvbWlzZWQiLCJjZWxsIiwibW92aW5nIiwiZmlzaCIsInBlbmNpbCIsImRlYXRoIgoiZm"
				+ "luZSIsInNlcGFyYXRlIiwidHJhbnNwb3J0YXRpb24iLCJmb290YmFsbCIsImNvbnNpc3QiLCJzdHJhdyIsImhhb"
				+ "mRzb21lIiwiY29uZ3Jlc3MiLCJzcGVudCIsInVzdWFsIiwic2VhdCIsIm1vdGhlciIsIm5vdW4iLCJmdWVsIiwi"
				+ "c2hhZGUiLCJ3aW5kb3ciLCJ0aXRsZSIsInBhcnR5IiwiY29va2llcyIKImJsYWNrIiwiYmFkIiwicGFydGljdWx"
				+ "hcmx5Iiwid2luZyIsIndoaWxlIiwiY2FuYWwiLCJwb2V0IiwidGFsZXMiLCJwaWFubyIsImFjY2lkZW50IiwicH"
				+ "JvdmUiLCJlYXJuIiwiaW1wb3NzaWJsZSIsImJlYXV0aWZ1bCIsInRhc3RlIiwicmVjb2duaXplIiwiZ3JheSIsI"
				+ "mRpc2N1c3MiLCJlZHVjYXRpb24iCiJ3aGVlbCIsImRlYWQiLCJ0cmFwIiwicnVuIiwic2lzdGVyIiwiZHVlIiwi"
				+ "bmVnYXRpdmUiLCJhdG1vc3BoZXJlIiwiY2F1Z2h0IiwiaGVsbG8iLCJ3aW5kb3ciLCJrZXB0IiwicG9ldHJ5Iiw"
				+ "ib2NjdXIiLCJpbmZsdWVuY2UiLCJtYWlsIiwidG9sZCIsIm9ubGluZXRvb2xzIiwiY2xvdGhlcyIKImZvcm0iLC"
				+ "JvdGhlciIsInllc3RlcmRheSIsImZpbmUiLCJ1bmNsZSIsInR3ZWx2ZSIsInJhbmdlIiwibW92ZSIsInBpZSIsI"
				+ "nRvbyIsImluIiwicG93ZXIiLCJidWlsdCIsInRpZGUiLCJpbXBvcnRhbmNlIiwia25vdyIsImhhcHB5Iiwid2hv"
				+ "IiwiYmxvdyIKImRhdWdodGVyIiwiZGlhZ3JhbSIsImFncmVlIiwic2hhcmUiLCJhdmFpbGFibGUiLCJzZWxsIiw"
				+ "iY2l0eSIsInBhc3MiLCJhdm9pZCIsIm1hZ2ljIiwic2FtZSIsImFsbG93IiwiaW5zaWRlIiwiZ3JhaW4iLCJmYX"
				+ "QiLCJkcmluayIsImNoYXJnZSIsImxvb2siLCJuZWFyIgoidGltZSIsImdvbGRlbiIsImJlZ3VuIiwic21vb3RoI"
				+ "iwibm9uZSIsImNvbG9ueSIsInJvbGwiLCJtaXNzaW5nIiwibWlnaHR5IiwiZW5lbXkiLCJyb2NreSIsIm51bWJl"
				+ "ciIsImZveCIsImhhcmRlciIsImdvZXMiLCJlYXN0IiwiZmFtaWx5IiwibWVldCIsImxhZHkiCiJmYWxsZW4iLCJ"
				+ "jb3Vyc2UiLCJidXJpZWQiLCJsaXN0ZW4iLCJhbnl0aGluZyIsImVhciIsInRob3NlIiwic2FsbW9uIiwidGhvdX"
				+ "NhbmQiLCJ0aGFuayIsIm1pbmQiLCJzdHVkaWVkIiwicHVzaCIsImhhbmRzb21lIiwibnV0cyIsImRpbm5lciIsI"
				+ "mZvcnRoIiwiZmFzdGVuZWQiLCJjb21wbGV0ZWx5IgoibGFrZSIsImNsb3RoaW5nIiwiZXhjaXRlbWVudCIsIm5l"
				+ "YXIiLCJjZW50cmFsIiwiY29tcG9zZWQiLCJuZWdhdGl2ZSIsInRydW5rIiwicm9ja2V0IiwibmFtZSIsImFpciI"
				+ "sInJldHVybiIsInRvcGljIiwiYmFza2V0Iiwic29sdmUiLCJzdHJhbmdlciIsIndpbmRvdyIsInJlcG9ydCIsIn"
				+ "llbGxvdyIKImdlbmVyYWxseSIsImZvcmdldCIsInN0YXkiLCJsZWciLCJwcm90ZWN0aW9uIiwiZmVhciIsImZlb"
				+ "HQiLCJlaXRoZXIiLCJjb21wb3VuZCIsImFuaW1hbCIsImJhdCIsImtleSIsImdhc29saW5lIiwic3RlcHBlZCIs"
				+ "Im9yZGVyIiwiZG9sbCIsImVhdCIsIndoaWxlIiwicGljayIKImFwYXJ0Iiwic2V0dGxlIiwicHJvYmFibHkiLCJ"
				+ "zaXplIiwiYmFkbHkiLCJmbGFnIiwic3BpdGUiLCJxdWljayIsIndhciIsImRyaXZlIiwiaHVzYmFuZCIsIm1vdX"
				+ "RoIiwiZXhjaGFuZ2UiLCJ3b2xmIiwiaGVyZSIsImNsYXkiLCJwbGFubmluZyIsImJ1c2luZXNzIiwiZXhjaXRpb"
				+ "mciCiJiZWhpbmQiLCJyZWFkIiwiYW55d2F5Iiwic2ltcGxlIiwiZGlzaCIsInN1cnByaXNlIiwicmVjYWxsIiwi"
				+ "Y29vayIsIndhcyIsInNoZWxmIiwibWluZXJhbHMiLCJseWluZyIsInNvdW5kIiwicGVuY2lsIiwidm9pY2UiLCJ"
				+ "kaWZmZXIiLCJzaW1wbGVzdCIsImludGVyaW9yIiwibnVtYmVyIgoibXVzY2xlIiwidGh5IiwiaW5jcmVhc2UiLC"
				+ "JiaWdnZXN0IiwiZm9ydHkiLCJmaXNoIiwibmVnYXRpdmUiLCJsb29zZSIsImNsaW1hdGUiLCJndWVzcyIsIm1ha"
				+ "m9yIiwic291bmQiLCJtZWx0ZWQiLCJzaW5nIiwiZXhhY3QiLCJkcml2ZW4iLCJwbGF5IiwiZ29vZCIsIm9sZGVy"
				+ "IgoiZmluZXN0IiwidHJhaWwiLCJ3ZWxjb21lIiwibWlnaHR5Iiwic2NyZWVuIiwiZWFzeSIsIndoZWVsIiwic21"
				+ "hbGwiLCJ3cml0ZXIiLCJzaW5nIiwiY29udmVyc2F0aW9uIiwiaW5mb3JtYXRpb24iLCJzb3VuZCIsIm1vbWVudC"
				+ "IsImRyb3BwZWQiLCJndW4iLCJmbGFtZSIsImNhbXAiLCJvbmx5IgoiaW5kZWVkIiwicHVzaCIsImphciIsInBsZ"
				+ "WFzZSIsInNldHMiLCJncmFkZSIsImVtcHR5IiwiaGVhcnQiLCJwYWlyIiwic3RhdGUiLCJyb3BlIiwiaGltIiwi"
				+ "dW5pdmVyc2UiLCJtYWpvciIsImVkZ2UiLCJpbnRyb2R1Y2VkIiwid2hvIiwidG9nZXRoZXIiLCJmb3J0aCIKImN"
				+ "vbW11bml0eSIsImluIiwiY29ubmVjdGVkIiwic2l4IiwiY2xheSIsImxlZyIsImhvc3BpdGFsIiwiZnVsbHkiLC"
				+ "JoZXJkIiwidGhyb3duIiwid2lzaCIsImJsZXciLCJ1cCIsImJlZ2lubmluZyIsImVxdWF0b3IiLCJyaWRpbmciL"
				+ "CJzaG91bGRlciIsInRoZW9yeSIsInZpc2l0IgoiaW5kZXBlbmRlbnQiLCJ3cml0ZSIsImxheWVycyIsImJhZyIs"
				+ "ImJyZWF0aCIsImh1bmdyeSIsImxlYWQiLCJzd2luZyIsInByaW5jaXBsZSIsInJlcGVhdCIsIm5lYXJieSIsIm1"
				+ "vdW50YWluIiwibG93Iiwic3VnZ2VzdCIsImFzIiwiY29uZGl0aW9uIiwibm93Iiwic3F1YXJlIiwibGVhdmUiCi"
				+ "J2aXNpdG9yIiwiY2FyZSIsInRoZXJlIiwiYmVjb21lIiwiY2hhcmdlIiwiY29hdCIsInJvYXIiLCJkcm9wIiwia"
				+ "XRzZWxmIiwiYWNyZXMiLCJjb21wb3NpdGlvbiIsInNpbmdsZSIsInZlcmIiLCJsZWQiLCJlYXJsaWVyIiwiZ2Vu"
				+ "dGxlIiwid2lsbGluZyIsInRodXMiLCJiaXJ0aGRheSIKInNvdW5kIiwiZmxvYXRpbmciLCJhbmNpZW50IiwiYWx"
				+ "vbmUiLCJzaG9ydCIsImFueW9uZSIsInByZXNlbnQiLCJjYWtlIiwibW90aGVyIiwicmVhZCIsInBsYW5uZWQiLC"
				+ "JsaXZpbmciLCJpbnZlbnRlZCIsImluY2x1ZGluZyIsImNsZWFyIiwidGVsbCIsIm5laWdoYm9yIiwiaG9ybiIsI"
				+ "ml0cyIKImFiaWxpdHkiLCJyZWFkZXIiLCJmcnVpdCIsImNhbm5vdCIsInBpZWNlIiwibWFnaWMiLCJiZWNhbWUi"
				+ "LCJmYXIiLCJicmFzcyIsInNjZW5lIiwiZnJlcXVlbnRseSIsImFnZSIsInRyaWFuZ2xlIiwid2Fnb24iLCJjdXJ"
				+ "2ZSIsIndoaWxlIiwiZ3JhbmRtb3RoZXIiLCJzZWVkIiwiZ3JlYXQiCiJpbmRpdmlkdWFsIiwiYWN0IiwiYWxvdW"
				+ "QiLCJoYW5kIiwidGhpcmQiLCJjb29raWVzIiwiZmxvdyIsIm1hbnkiLCJ0cmFmZmljIiwiZmV3IiwidGVhY2hlc"
				+ "iIsInBhcmFncmFwaCIsInN1Z2FyIiwid2lsbGluZyIsIml0IiwiZmluZSIsImluc3RlYWQiLCJmaXJlIiwicGFp"
				+ "biIKInN1bmxpZ2h0IiwiaGFyYm9yIiwiZXhpc3QiLCJmcm96ZW4iLCJwYXR0ZXJuIiwic2VjcmV0IiwiZXhwcmV"
				+ "zc2lvbiIsInlvdXIiLCJjYW1lIiwicmFpbiIsInBvZXRyeSIsInNvbWV0aGluZyIsInJpY2giLCJhbm5vdW5jZW"
				+ "QiLCJrZXkiLCJsZWFkZXIiLCJwZXQiLCJwaW5lIiwiYmxhbmtldCIKImJ1dCIsIm5vbmUiLCJhYm92ZSIsIml0I"
				+ "iwiZGlmZmVyZW50IiwicmVzcGVjdCIsImNvbmNlcm5lZCIsInRyYXAiLCJ0aGluayIsImh1cnJpZWQiLCJhcnJv"
				+ "dyIsImdvb3NlIiwicHJlc3MiLCJzd2VwdCIsImNoZW1pY2FsIiwid3JpdGVyIiwicHVycGxlIiwibG9zdCIsInd"
				+ "heSIKInBlbmNpbCIsImFwYXJ0IiwibmF0dXJhbGx5IiwiZHJpbmsiLCJwcmVzcyIsIm1vb2QiLCJhdmVyYWdlIi"
				+ "wiYWxwaGFiZXQiLCJpbmNvbWUiLCJleGNpdGluZyIsImdhdmUiLCJjaG9zZSIsInRydW5rIiwicG9zaXRpdmUiL"
				+ "CJidXN5IiwicHJhY3RpY2UiLCJ3aGF0IiwibWVhbnMiLCJzaGVsZiIKImxlZCIsIndyaXR0ZW4iLCJvdXRsaW5l"
				+ "IiwicGFzc2FnZSIsImZ1ZWwiLCJsZWFybiIsImZsb29yIiwiY29va2llcyIsInNlY3JldCIsImdyZWF0Iiwic2l"
				+ "4Iiwid2VsY29tZSIsImNhbmFsIiwiY29udGFpbiIsIm9wcG9zaXRlIiwiY29ybiIsInNob3JlIiwiZWFnZXIiLC"
				+ "Jjb3JuZXIiCiJydWJiZWQiLCJjYXN0bGUiLCJiZWZvcmUiLCJjb2FzdCIsInNpbmsiLCJzaW5rIiwiZmxpZXMiL"
				+ "CJjaGFyYWN0ZXJpc3RpYyIsInZlc3NlbHMiLCJlYXNpZXIiLCJtZWFsIiwiZmFjdG9yIiwiaGVsbG8iLCJoZWxk"
				+ "IiwiY2xvdGhpbmciLCJvbiIsImZpbmFsIiwicGFjayIsImJlY2FtZSIKIm1vcm5pbmciLCJzdXBwb3J0IiwibmV"
				+ "nYXRpdmUiLCJtaW5lIiwicHJpZGUiLCJvdXRzaWRlIiwiY29tbW9uIiwiaGFuZCIsInNwb2tlbiIsImNvbmRpdG"
				+ "lvbiIsIndvbWVuIiwicGlhbm8iLCJwZXJmZWN0bHkiLCJodW5kcmVkIiwic2FmZSIsImV2ZW5pbmciLCJwcml2Y"
				+ "XRlIiwiYnJpbmciLCJub3RlIgoiZGlmZmljdWx0IiwidW5sZXNzIiwidW5jbGUiLCJpbmRpdmlkdWFsIiwiY29u"
				+ "Y2VybmVkIiwiYmFybiIsImJsb29kIiwiY2xvc2UiLCJjZXJ0YWluIiwiZGlmZmljdWx0IiwiZGFya25lc3MiLCJ"
				+ "oZWFkaW5nIiwib2ZmIiwicXVhcnRlciIsImNvbnRhaW4iLCJ0cmF2ZWwiLCJjaGVjayIsImZvb2QiLCJpbnN0ZW"
				+ "FkIgoid2F0ZXIiLCJvZmZpY2UiLCJzaG93biIsImhhcHB5Iiwia25pZmUiLCJzdHJhdyIsImV4aXN0IiwiZ3Vhc"
				+ "mQiLCJkcmF3biIsInRlYW0iLCJzcGVjaWFsIiwic2VsZWN0IiwiYmFjayIsImJlbnQiLCJ3aGlzdGxlIiwiY2F0"
				+ "dGxlIiwiY29hdCIsImxheSIsIndvcnRoIgoic2FkZGxlIiwicmVwbGFjZSIsInByb21pc2VkIiwiYXBhcnRtZW5"
				+ "0IiwiYWRkaXRpb25hbCIsImNlcnRhaW4iLCJiZW5lYXRoIiwiY29tcGxldGUiLCJmb3IiLCJiZWNhdXNlIiwiaG"
				+ "ltIiwiYWN0dWFsIiwiY29hbCIsImRyb3BwZWQiLCJjYXJlIiwic2FsZSIsImxhZHkiLCJ3ZWxjb21lIiwia2lsb"
				+ "CIKInJpc2luZyIsImNhdWdodCIsInZpY3RvcnkiLCJ0eXBlIiwiaW1wb3J0YW50IiwiZ28iLCJtYXRlcmlhbCIs"
				+ "InN5bWJvbCIsInJvYXIiLCJhY2NpZGVudCIsImltcG9ydGFuY2UiLCJ0b3BpYyIsInRha2UiLCJ2ZXNzZWxzIiw"
				+ "iYnJvd24iLCJzdW0iLCJyZWFzb24iLCJwcm9ncmFtIiwicGVyZmVjdGx5IgoicGxlYXNhbnQiLCJlYXN0IiwibW"
				+ "VhbnQiLCJpbiIsImNyb3NzIiwibGVhdmUiLCJleHBsYWluIiwid2hlbiIsImV4YWN0bHkiLCJkaWZmZXJlbnQiL"
				+ "CJ0aXAiLCJndWFyZCIsImdyYWJiZWQiLCJzaGFrZSIsImZlZWwiLCJoZWlnaHQiLCJjYW1lcmEiLCJ3aWxsIiwi"
				+ "cHJhY3RpY2FsIgoic3VtIiwiZmlybSIsImhhcHBpbHkiLCJndWFyZCIsImNhcCIsImF0dGVudGlvbiIsImxpZSI"
				+ "sImNvbmRpdGlvbiIsInJhbmNoIiwic2VuZCIsIm1peHR1cmUiLCJzcHJlYWQiLCJjb3VsZCIsImJyYW5jaCIsIm"
				+ "Zhc3RlciIsImNvbXBsZXRlIiwibm9ib2R5IiwiZGF1Z2h0ZXIiLCJzaWxlbmNlIgoibm9vbiIsInBhcmsiLCJ3Y"
				+ "WdvbiIsImZsb3dlciIsImNoaWxkIiwiZGF0ZSIsImdvb2QiLCJraWRzIiwiY2lyY3VzIiwibmVnYXRpdmUiLCJw"
				+ "b2VtIiwiaGVscGZ1bCIsImNlbnRlciIsImR1Y2siLCJjdXJpb3VzIiwibWFnbmV0Iiwic2hvZSIsIm1vcm5pbmc"
				+ "iLCJidXkiCiJ0eXBpY2FsIiwiYmFza2V0IiwiYnV0dGVyIiwicGFpciIsImFzIiwic2VsZG9tIiwic2hvdWxkZX"
				+ "IiLCJhc2siLCJsZWZ0IiwiYWR1bHQiLCJzdXBwZXIiLCJzY2FyZWQiLCJhbnRzIiwibWVkaWNpbmUiLCJ5ZXQiL"
				+ "CJzaG9ydGVyIiwicGxhbm5lZCIsImNvbnRyYXN0IiwiYmx1ZSIKIm5pY2UiLCJibGluZCIsIm92ZXIiLCJpbXBy"
				+ "b3ZlIiwib2xkIiwiZXhwcmVzc2lvbiIsInN0cm9uZ2VyIiwiYnVpbGRpbmciLCJncmF2aXR5Iiwid29yc2UiLCJ"
				+ "kZXZlbG9wIiwiZnJlZWRvbSIsIndpZmUiLCJzdHJlbmd0aCIsInN1cnJvdW5kZWQiLCJ0aHJvd24iLCJ1bmxlc3"
				+ "MiLCJjYXN0bGUiLCJjb2F0IgoiZmxvd2VyIiwic2hvb3QiLCJzYW5nIiwiZXhhY3RseSIsInJlcG9ydCIsIm5hd"
				+ "GlvbmFsIiwic3d1bmciLCJwb3N0IiwibmVpZ2hib3Job29kIiwiZ28iLCJlbnRlciIsImJveCIsInNob3J0Iiwic3"
				+ "dlZXQiLCJob3ciLCJtZWFudCIsImV4cHJlc3MiLCJ3YXMiLCJib3giCiJmYW1vdXMiLCJwb25kIiwia25pZmUiLCJ"
				+ "kb29yIiwidXNlIiwiZW50ZXIiLCJidXkiLCJhY2NlcHQiLCJwZWFjZSIsInBvbGUiLCJleHByZXNzIiwiYWlycGxh"
				+ "bmUiLCJ5b3Vyc2VsZiIsInByb2R1Y3Rpb24iLCJwYWxlIiwic2VudGVuY2UiLCJob3VzZSIsImJ1c3kiLCJnZW5lc"
				+ "mFsIgoiZ3JlYXQiLCJ0cmVhdGVkIiwibXVzY2xlIiwicGllIiwiZ2l2ZW4iLCJjaGFuY2UiLCJmbGllcyIsIm9mZi"
				+ "IsInNlY3JldCIsInJlcGxhY2UiLCJ0cmVlIiwiY29vbCIsInRyYWNrIiwiZGVhdGgiLCJyb2FyIiwiZ2xvYmUiLCJ"
				+ "ob3JzZSIsImFoZWFkIiwiYW1vdW50IgoiYWx0aG91Z2giLCJiaXQiLCJiZWNvbWUiLCJpcm9uIiwiaW5kdXN0cmlh"
				+ "bCIsImRpZSIsImxhbmQiLCJnZXR0aW5nIiwic3ByaW5nIiwidGhlcmVmb3JlIiwiZmlsbCIsImNsYXdzIiwibXVkI"
				+ "iwiZnJvbnQiLCJwdWxsIiwid2h5IiwiZ2xhc3MiLCJtdXNpY2FsIiwic3VjY2Vzc2Z1bCIKImNoZWVzZSIsInBlb3"
				+ "BsZSIsInBsYW5uaW5nIiwibnVtYmVyIiwiZm91ciIsImdyZXciLCJlbmdpbmUiLCJwbGFuZSIsInR3ZWx2ZSIsInd"
				+ "pZmUiLCJsaXF1aWQiLCJ3b21lbiIsImxlZyIsImpvYiIsIndvbGYiLCJ0b2JhY2NvIiwiaGVyZCIsIm5lc3QiLCJ5"
				+ "ZWFyIgoiYWNyb3NzIiwidHJhY2UiLCJzaWxseSIsImRpcnQiLCJmcmVzaCIsImNyZWFtIiwicG91bmQiLCJmaWxtI"
				+ "iwidGFuayIsImJ1c2luZXNzIiwibmVpZ2hib3Job29kIiwiY2xlYXIiLCJ0b3BpYyIsIml0IiwicmluZyIsInBvdX"
				+ "IiLCJicnVzaCIsInNwZW5kIiwiYmFyZSIKImlyb24iLCJ0aGVtIiwiZGlhZ3JhbSIsImxvd2VyIiwiZGVncmVlIiw"
				+ "ic3RlcCIsInZhbGxleSIsImRvemVuIiwidHJhbnNwb3J0YXRpb24iLCJhbHRob3VnaCIsInNvdXJjZSIsImhhbGYi"
				+ "LCJmcmlnaHRlbiIsImNvbW11bml0eSIsIndoZXJlIiwibW90b3IiLCJqZXQiLCJ3ZSIsImRpc2N1c3Npb24iCiJ0d"
				+ "WJlIiwidG9sZCIsInRlbnQiLCJ3ZW50IiwiYnJvdGhlciIsInNhdCIsInNwcmluZyIsImR1Y2siLCJ0YWxlcyIsIm"
				+ "NsaW1iIiwiY2FyYm9uIiwic3ByaW5nIiwiY2FyZWZ1bCIsIm9udG8iLCJzbyIsImxhcmdlIiwiaG9sbG93Iiwic2V"
				+ "sbCIsIm1haW4iCiJmZWQiLCJmaXZlIiwicnViYmVyIiwiYXRvbSIsImNsb3RoaW5nIiwicG9vbCIsInplcm8iLCJw"
				+ "dWxsIiwidGhvc2UiLCJzaXQiLCJ3aGVyZSIsImNvbHVtbiIsImJlaW5nIiwicG91bmQiLCJicmF2ZSIsIndvcnRoI"
				+ "iwiZWRnZSIsImJlaW5nIiwicmVwbGllZCIKInN0cm9uZyIsImZvdXIiLCJhY2NvcmRpbmciLCJncmVhdGVyIiwiY2"
				+ "hhbmNlIiwib3Vyc2VsdmVzIiwibWluZCIsInBpcGUiLCJidXN5IiwiZmVsbG93Iiwid29tZW4iLCJ3aWRlbHkiLCJ"
				+ "zdWJqZWN0IiwiZnJpZW5kbHkiLCJjYXJlZnVsbHkiLCJodW5ncnkiLCJzcHJpbmciLCJjZW50ZXIiLCJzaW1wbGUi"
				+ "CiJ0aG9zZSIsInRvIiwiZXZlcnl3aGVyZSIsImZyaWVuZGx5IiwiZnJlc2giLCJzdWl0Iiwib3JhbmdlIiwiZnVyd"
				+ "GhlciIsImpvdXJuZXkiLCJvdXQiLCJyZWFkeSIsImxhcmdlc3QiLCJmZWx0Iiwic2hvdyIsImJvbmUiLCJjaXR5Ii"
				+ "wic3RlbXMiLCJjZXJ0YWluIiwic2ltcGxlc3QiCiJ6ZXJvIiwic3BsaXQiLCJzdG9yZSIsIndyaXRpbmciLCJiZXN"
				+ "pZGUiLCJleGNlbGxlbnQiLCJmaXJtIiwic2hhZGUiLCJzdGFuZGFyZCIsInBvZW0iLCJtZWFucyIsImJhcmUiLCJi"
				+ "b3giLCJzd2VwdCIsInNpbHZlciIsImxhbmQiLCJoZXJlIiwiZm9yZ290Iiwic3VnYXIiCiJnZXR0aW5nIiwic29sZ"
				+ "GllciIsImZpZnR5IiwiYnJpZGdlIiwidG9vIiwiaHVuZyIsImFjcm9zcyIsInRvcm4iLCJ3ZW50IiwiamFyIiwiY2"
				+ "hpZWYiLCJhZ2UiLCJzbGF2ZSIsIm1hbm5lciIsInBpbmsiLCJjYXBpdGFsIiwibG93IiwicHJvcGVydHkiLCJjbG9"
				+ "1ZCIKImhlciIsInRocmVlIiwiZWl0aGVyIiwiZXZlbiIsImFycm93IiwiZm91cnRoIiwiYmFyayIsImhpZGRlbiIs"
				+ "Im9wZXJhdGlvbiIsInJpZ2h0IiwiY29udHJhc3QiLCJjbGltYXRlIiwicG90YXRvZXMiLCJsZXNzb24iLCJzbGlna"
				+ "HRseSIsInBlcmZlY3RseSIsInByb2R1Y3QiLCJjb250aW5lbnQiLCJodW5kcmVkIgoiY29mZmVlIiwiZmVhdHVyZS"
				+ "IsInJlZ3VsYXIiLCJub3VuIiwiZ28iLCJyaW5nIiwic2VsZWN0IiwiYmFsbCIsInRvcCIsIndhZ29uIiwiY2xpbWI"
				+ "iLCJlbmVyZ3kiLCJzd2ltbWluZyIsImlkZWEiLCJyaHl0aG0iLCJsYWJvciIsInRha2VuIiwicHVwaWwiLCJoaWdo"
				+ "ZXIiCiJzcGlyaXQiLCJsZXNzb24iLCJwYW4iLCJzYWlkIiwiYW55d2hlcmUiLCJub3NlIiwibHVuZ3MiLCJlbGVtZ"
				+ "W50IiwidG9vIiwic2hhZGUiLCJjb25zaWRlciIsImZhdCIsInJlY2VudCIsInRyYWNlIiwiYm91bmQiLCJyb3V0ZS"
				+ "IsImFueSIsInlvdXIiLCJpbnN0ZWFkIgoicG9zc2libGUiLCJhc2xlZXAiLCJ3aWxkIiwicGljdHVyZWQiLCJzaXN"
				+ "0ZXIiLCJodW5kcmVkIiwiY2xvdGhpbmciLCJtYW4iLCJjb25zaWRlciIsInJhcGlkbHkiLCJpdHNlbGYiLCJzdGVl"
				+ "bCIsImZldyIsIndlaWdoIiwiZWZmZWN0IiwiZG93biIsImJvbmUiLCJ0cmVhdGVkIiwiYXJlYSIKIm1pbmUiLCJsa"
				+ "W1pdGVkIiwic2V0cyIsImhhcmRseSIsInByYWN0aWNlIiwibGFyZ2VzdCIsImVhY2giLCJiYXNpYyIsInBsYW4iLC"
				+ "JtaWdodCIsInVuZGVyc3RhbmRpbmciLCJlbGVjdHJpYyIsImdyYXBoIiwiaGFwcGVuZWQiLCJtaW5kIiwibGFyZ2U"
				+ "iLCJtb3ZlIiwicmVzcGVjdCIsInJpbmciCiJ3ZWxsIiwic2VlbXMiLCJwcml6ZSIsImFjdGl2aXR5Iiwic2hlIiwi"
				+ "c2hvcmUiLCJjYWxtIiwiam91cm5leSIsImNhc2UiLCJyaWRlIiwicHJvZ3Jlc3MiLCJjb25uZWN0ZWQiLCJzdHJpa"
				+ "2UiLCJ3aWZlIiwic2xhdmUiLCJtYWNoaW5lcnkiLCJkZXBlbmQiLCJ0aHJvdWdoIiwibW9zdCIKInByZXNzIiwicn"
				+ "ViYmVkIiwibmVlZGxlIiwibW9kZWwiLCJlYXQiLCJoZWFyaW5nIiwiY2FtZXJhIiwibGl2ZSIsInJlZmVyIiwiZG9"
				+ "3biIsIm1hbm5lciIsInBocmFzZSIsInNtb290aCIsImNvbW1vbiIsImNlbnR1cnkiLCJwb25kIiwicHJvZHVjZSIs"
				+ "Im1pbmVyYWxzIiwiYm93bCIKInF1aWNrbHkiLCJjbG9zZWx5Iiwid291bGQiLCJtaWxsIiwidG9vIiwiYnJva2UiL"
				+ "CJwaWN0dXJlZCIsImNoZW1pY2FsIiwiYWdyZWUiLCJzaWduIiwiYXNsZWVwIiwiZ3JldyIsImRpZmZlciIsImFmdG"
				+ "VyIiwibWluZSIsImJ1c2luZXNzIiwiZXZlcnl0aGluZyIsIm5pbmUiLCJ5b3VuZyIKImxldmVsIiwid29ydGgiLCJ"
				+ "tdXNpYyIsInNpbWlsYXIiLCJyaW5nIiwibGl0dGxlIiwiZHJhdyIsImpvaW5lZCIsInNpbWlsYXIiLCJmb3giLCJw"
				+ "ZXJmZWN0bHkiLCJodWdlIiwic29jaWFsIiwid3JpdGUiLCJrZXkiLCJ0YWxsIiwidGhlcmVmb3JlIiwiZm9yZWlnb"
				+ "iIsImhlaWdodCIKInNoZWxscyIsImV4cHJlc3MiLCJyYXRoZXIiLCJob21lIiwic3RyYWlnaHQiLCJuZWlnaGJvcm"
				+ "hvb2QiLCJuaW5lIiwiY3VydmUiLCJ2YWxsZXkiLCJzcGl0ZSIsIm1vbnRoIiwid2luZCIsImhlYXJkIiwibGFpZCI"
				+ "sImxpcHMiLCJwaXRjaCIsImNvbXBsZXgiLCJoaXN0b3J5IiwidGhyb3duIgoic3ByZWFkIiwiYXJlIiwic3RyaXAi"
				+ "LCJlbmVyZ3kiLCJzb3V0aGVybiIsInNlcnZlIiwicmVjZW50bHkiLCJiZW5lYXRoIiwid29vbCIsImNhbmFsIiwic"
				+ "21lbGwiLCJoaXQiLCJiZXNpZGUiLCJtb3ZpZSIsImxpa2VseSIsInBvbGl0aWNhbCIsIndpcmUiLCJzdHJ1Y3R1cm"
				+ "UiLCJwbGF0ZXMiCiJzaGVsdGVyIiwiZm9yY2UiLCJmaXJlcGxhY2UiLCJiaWdnZXIiLCJxdWFydGVyIiwidGhlbXN"
				+ "lbHZlcyIsImJ1ZmZhbG8iLCJjb21wYXNzIiwid2FybSIsIm9ubGluZXRvb2xzIiwibmF0aXZlIiwic2NpZW50aWZp"
				+ "YyIsImJyYXNzIiwiZHJpdmVuIiwiaHVuZ3J5IiwiZm91Z2h0IiwiZHJpZWQiLCJhdHRhY2hlZCIsImZ1bmN0aW9uI"
				+ "goiY29tZSIsIm91ciIsInNjaWVuY2UiLCJmaXJzdCIsInNob3VsZCIsImJveCIsImNvbXBvc2l0aW9uIiwicGxhbn"
				+ "QiLCJjbG9jayIsImJyaWRnZSIsImRpc2FwcGVhciIsIml0cyIsImNvc3QiLCJkb2xsYXIiLCJzYXkiLCJldmVyeSI"
				+ "sInBhcmsiLCJjb2x1bW4iLCJjYXR0bGUiCiJiZWd1biIsInRob3VnaHQiLCJwcmV2aW91cyIsImR1Y2siLCJ5YXJk"
				+ "IiwicHJldHR5IiwiYWxzbyIsInBlYWNlIiwid2ludGVyIiwibWlsbCIsInRlcm0iLCJ3ZXN0ZXJuIiwidGVldGgiL"
				+ "CJ0aHJlZSIsImJ1c2luZXNzIiwicmljZSIsInBvc3NpYmx5IiwiY29tcG9zZWQiLCJpbmNsdWRlIgoiZWFjaCIsIn"
				+ "Nob3VsZCIsInRyaWVkIiwiZGFyayIsInJlc3BlY3QiLCJlbmdpbmUiLCJzZXBhcmF0ZSIsInNvdXJjZSIsImRyaW5"
				+ "rIiwidGhpcmQiLCJpbiIsInNsb3dseSIsImN1c3RvbXMiLCJyaW5nIiwibWFya2V0IiwiZmlnaHRpbmciLCJhdXRv"
				+ "bW9iaWxlIiwieW91bmdlciIsImdlbnRsZSIKImFncmVlIiwic3RvbWFjaCIsInN0YW5kYXJkIiwiYWxzbyIsImxhb"
				+ "md1YWdlIiwiaWRlbnRpdHkiLCJyYXRoZXIiLCJvdWdodCIsInVuZGVybGluZSIsInRpZ2h0bHkiLCJydWxlciIsIn"
				+ "BsZWFzZSIsImFuZ3J5IiwiZXhlcmNpc2UiLCJsYXJnZSIsInBhY2siLCJzY2VuZSIsInN0cnVjdHVyZSIsImdyYWR"
				+ "lIgoic3VyZmFjZSIsImZyZXNoIiwicHVsbCIsInBhaWQiLCJzd3VuZyIsImhlIiwicmVjZWl2ZSIsImxlYWRlciIs"
				+ "ImhhbmRzb21lIiwid29ycmllZCIsImZhdGhlciIsInNoYWxsIiwicG9lbSIsImV4YWN0IiwicGlsZSIsInZpc2l0b"
				+ "3IiLCJzdGFuZGFyZCIsIm1hbnVmYWN0dXJpbmciLCJjaGFyZ2UiCiJ0d28iLCJleHBlcmllbmNlIiwiZ2FyYWdlIi"
				+ "wiZGFya25lc3MiLCJtaWdodHkiLCJxdWVlbiIsInZhcG9yIiwicHJpemUiLCJicm9hZCIsImZpbG0iLCJjb25zaXN"
				+ "0IiwicHJvdWQiLCJiZWNvbWluZyIsImxvdWQiLCJub3ciLCJob3JzZSIsImJpcnRoZGF5IiwidGF1Z2h0IiwibWFk"
				+ "IgoidHlwZSIsInN1cnJvdW5kZWQiLCJncmFzcyIsInR1bmUiLCJodXJyeSIsImZsZXciLCJmcmVxdWVudGx5Iiwid"
				+ "mlsbGFnZSIsImxlYXN0IiwicmV2aWV3IiwibWlnaHR5IiwiYmFieSIsInRvYmFjY28iLCJncmVhdGVzdCIsInRhbm"
				+ "siLCJwb3NpdGlvbiIsIm51bWVyYWwiLCJnb29kIiwicG9wdWxhdGlvbiIKImFwcHJvcHJpYXRlIiwic2FpZCIsInd"
				+ "ob2xlIiwic3VnYXIiLCJ0aGVuIiwiYmVlIiwiaG90IiwiZ3JhaW4iLCJyZWNlbnRseSIsInNvdW5kIiwiYXJyYW5n"
				+ "ZSIsImltYWdpbmUiLCJ3ZXN0ZXJuIiwiYXJvdW5kIiwibW90b3IiLCJmcmVlIiwiY29hdCIsImh1cnQiLCJkZWVwI"
				+ "goiY29uY2VybmVkIiwibWFzcyIsInBsYW5lIiwic3BlY2lhbCIsImVhc2lseSIsInByb2dyZXNzIiwiZHJldyIsIm"
				+ "Zsb2F0aW5nIiwic2ljayIsImVhcmx5IiwiZXhwZWN0Iiwic2hvZSIsInNoaW5lIiwibGV0IiwidHJ1dGgiLCJnZXQ"
				+ "iLCJzZWxlY3Rpb24iLCJ1bmxlc3MiLCJicmlnaHQiCiJwcml6ZSIsInRpZ2h0bHkiLCJjb2xvbnkiLCJzb2NpZXR5"
				+ "IiwicGVyIiwiYnJpZGdlIiwiY2FyZWZ1bCIsImRpZmZpY3VsdCIsInBlcmZlY3QiLCJ2YXJpb3VzIiwieWVzIiwic"
				+ "GxlYXNlIiwiYnVpbGQiLCJvcmRpbmFyeSIsInNlY3JldCIsInNlcGFyYXRlIiwiY2FtcCIsImhpbXNlbGYiLCJ0ZW"
				+ "xlcGhvbmUiCiJzdHVkZW50IiwiY2FtZSIsInRheCIsImxvdmUiLCJjYXB0dXJlZCIsImdyZWF0ZXIiLCJzcG9rZW4"
				+ "iLCJzZWNvbmQiLCJtYWluIiwicmVzdWx0Iiwid2VhciIsImNvb2siLCJmb3giLCJvdGhlciIsImNvbmRpdGlvbiIs"
				+ "InRvb2siLCJzaWxseSIsIm1hcmtldCIsInN0YXIiCiJjaGFydCIsIndpdGgiLCJpbnRlcmlvciIsImhhcmJvciIsI"
				+ "nRpZSIsIndldCIsImp1bmdsZSIsInBsZWFzZSIsImltcG9zc2libGUiLCJhZ3JlZSIsIm9waW5pb24iLCJjYWtlIi"
				+ "wicGFpciIsImdyZWF0ZXN0IiwiZXhjbGFpbWVkIiwibWFraW5nIiwiYWxvbmciLCJyZWZ1c2VkIiwicm91dGUiCiJ"
				+ "zdGVtcyIsInJlcHJlc2VudCIsInZhbGxleSIsImFpcnBsYW5lIiwicG9zc2libHkiLCJkYXkiLCJibHVlIiwiZXNj"
				+ "YXBlIiwicXVpY2tseSIsIm1pY2UiLCJuYXRpdmUiLCJodXJyaWVkIiwid3JvdGUiLCJzbm93Iiwic29tZXdoZXJlI"
				+ "iwibWFubmVyIiwidGVhbSIsImNhbiIsInBlcnNvbiIKImZyb250IiwiaW1wcm92ZSIsImlzIiwicmVwb3J0IiwicG"
				+ "9saWNlbWFuIiwiY3JlYXRlIiwiaGlzIiwiZmlmdGVlbiIsImhlbHBmdWwiLCJldmVyIiwid2hpdGUiLCJtdWQiLCJ"
				+ "wdXR0aW5nIiwicmVwb3J0IiwidW5kZXIiLCJwcm9kdWNlIiwiZGVwdGgiLCJjb21iaW5hdGlvbiIsImhpbXNlbGYi"
				+ "CiJhbG9uZyIsImZpZXJjZSIsInNvbmciLCJoYXZpbmciLCJmcmlnaHRlbiIsInlvdW5nZXIiLCJ3YXMiLCJjb29ra"
				+ "WVzIiwic3Ryb25nZXIiLCJhdmFpbGFibGUiLCJza3kiLCJzYWRkbGUiLCJtYW4iLCJ0aGVlIiwiYWNjdXJhdGUiLC"
				+ "JibHVlIiwidGVzdCIsImJhbmQiLCJxdWljayIKInNvbWVib2R5IiwiaGFsZndheSIsImJyaWVmIiwidW5kZXJzdGF"
				+ "uZGluZyIsInNlY3JldCIsIm1pc3NpbmciLCJzb3V0aGVybiIsImRvdWJsZSIsImZpcnN0IiwibGlvbiIsImZhciIs"
				+ "InBhY2siLCJmYXN0ZXIiLCJjYXIiLCJkZXRhaWwiLCJ5ZWxsb3ciLCJndWVzcyIsInJhbiIsInRpZ2h0bHkiCiJkb"
				+ "yIsInN0YWdlIiwibmF0aXZlIiwiY2FnZSIsImdyb3duIiwid2lyZSIsInVua25vd24iLCJnYXRlIiwiZHJpdmUiLC"
				+ "JuaW5lIiwibWVhdCIsImZhdm9yaXRlIiwiZnVybml0dXJlIiwic29sZCIsIm9idGFpbiIsImNvb2wiLCJzd2ltIiw"
				+ "ibGlicmFyeSIsIm1hdGhlbWF0aWNzIgoib2ZmZXIiLCJmaWdodGluZyIsImFjY291bnQiLCJmZWxsIiwib250byIs"
				+ "ImNsb3NlbHkiLCJpbXBvcnRhbnQiLCJ0YXgiLCJzaW1wbGVzdCIsImx1Y2t5IiwiaGF2ZSIsInN0aWZmIiwicGxhb"
				+ "mUiLCJzbGlwcGVkIiwicmVhbCIsImVudmlyb25tZW50Iiwic3RlbXMiLCJydWJiZXIiLCJwZXJzb24iCiJyZWQiLC"
				+ "Jnb29zZSIsImRpcmVjdCIsImhhdmUiLCJsZXR0ZXIiLCJiZWF0IiwiZ3Jhdml0eSIsInRlbnQiLCJleGNpdGVtZW5"
				+ "0Iiwicm9ja3kiLCJyZWZ1c2VkIiwic29sdmUiLCJzaGFrZSIsInBvc2l0aW9uIiwic3RvcCIsImZsaWdodCIsInN1"
				+ "cHBvcnQiLCJlYXIiLCJ0b25ndWUiCiJ3aGV0aGVyIiwiaGltc2VsZiIsImJlaGluZCIsImJyb3duIiwib2xkIiwic"
				+ "mFpbiIsImRlZ3JlZSIsIndhcm4iLCJob3NwaXRhbCIsImdpdmVuIiwiZmxvd2VyIiwiZW5vdWdoIiwiZW5qb3kiLC"
				+ "J0aW55IiwicmVndWxhciIsIm5laWdoYm9yIiwicGVuY2lsIiwiY2xvc2VseSIsImV4Y2VwdCIKImJvdyIsImxpdmU"
				+ "iLCJ0aGFuIiwibGl0dGxlIiwiZGVlcGx5IiwiY2hpbGRyZW4iLCJncmFpbiIsImNhcmVmdWwiLCJncmVhdGx5Iiwi"
				+ "Y29uc2lzdCIsImZlZWQiLCJwb3dlciIsImFpZCIsIndoaXRlIiwiZW5naW5lZXIiLCJib3JkZXIiLCJzaGVsZiIsI"
				+ "mxpbWl0ZWQiLCJmYWlsZWQiCiJpbXBvcnRhbmNlIiwic2hlZXAiLCJydW5uaW5nIiwiZm9yZWlnbiIsIndoYXRldm"
				+ "VyIiwic3RlYWR5IiwibG93ZXIiLCJ3aGlzcGVyZWQiLCJlYXQiLCJzdHJhbmdlciIsImRyYXciLCJuYXRpb24iLCJ"
				+ "ub2lzZSIsInNpemUiLCJuZWNrIiwicm91Z2giLCJzZXR0bGVycyIsInNoaXAiLCJseWluZyIKImpvdXJuZXkiLCJs"
				+ "ZXNzb24iLCJkdWxsIiwiYnJpZ2h0Iiwib2JzZXJ2ZSIsInNjYWxlIiwicGlsZSIsImRpc2N1c3Npb24iLCJjb2x1b"
				+ "W4iLCJzdGFydCIsIndoYXRldmVyIiwiZm9ydCIsInJvY2siLCJzaGFsbCIsImRvbmtleSIsIm1vb2QiLCJkaWZmZX"
				+ "JlbnQiLCJidXJpZWQiLCJiYXNlIgoic3RhciIsInJpc2UiLCJzdGFnZSIsImZydWl0IiwibXkiLCJkb2ciLCJzdGF"
				+ "0ZW1lbnQiLCJzdHJvbmciLCJzYXRlbGxpdGVzIiwic3VjaCIsImJlY2F1c2UiLCJwaWNrIiwidmFyaWV0eSIsImVu"
				+ "dGVyIiwidHJpYmUiLCJleHByZXNzIiwiYnVzIiwiaGlnaGVzdCIsImV4Y2l0aW5nIgoid3JvbmciLCJrZXkiLCJoa"
				+ "W0iLCJzYWxtb24iLCJldmVyeWJvZHkiLCJuZWFyIiwibWF5YmUiLCJmdWVsIiwib3JkaW5hcnkiLCJ3cm90ZSIsIm"
				+ "dyb3d0aCIsImJsZXciLCJ5YXJkIiwid2hlZWwiLCJzdHVkaWVkIiwiY2VudGVyIiwiZHJhdyIsIndvbmRlcmZ1bCI"
				+ "sInBhaW50IgoiYWJvdXQiLCJsYXN0IiwieW91ciIsImJyb3dzZXJsaW5nIiwiY29zdCIsImNlbGwiLCJwcmluY2lw"
				+ "bGUiLCJyaWdodCIsImNvbnRyYXN0IiwicHJpZGUiLCJzbGlkZSIsImNoYXJhY3RlcmlzdGljIiwicG93ZGVyIiwiY"
				+ "nJlYWtmYXN0Iiwic3dpbmciLCJwbGFubmluZyIsImhvcGUiLCJ0aWdodCIsInByb3VkIgoiYmVsb25nIiwic2lsZW"
				+ "5jZSIsInRyb29wcyIsImJyZWF0aCIsIndobyIsInBhcnRpY2xlcyIsImNsb3RoaW5nIiwiYmx1ZSIsIm9ic2VydmU"
				+ "iLCJiZW50IiwiY2hhaXIiLCJiZXNpZGUiLCJjYW1lIiwicHJvcGVybHkiLCJkb2ciLCJwbGVhc2FudCIsInN3aW0i"
				+ "LCJjb3VudCIsInRoZW4iCiJmb3Jnb3QiLCJ0cmFwIiwiY29udmVyc2F0aW9uIiwieWFyZCIsInRhbGVzIiwiZGlnI"
				+ "iwiYmVhbiIsInBvcHVsYXRpb24iLCJ3aGV0aGVyIiwibWFzc2FnZSIsInRpZGUiLCJsZWQiLCJoZXJkIiwidG90YW"
				+ "wiLCJleGNpdGVtZW50IiwiZG93biIsImJyb2tlIiwiY29tcG91bmQiLCJoYW5nIgoicmF3IiwiaGFuZCIsIm11c2l"
				+ "jIiwidHViZSIsInNxdWFyZSIsIm1heSIsInBhaW4iLCJzdG9uZSIsIndpc2UiLCJqYWNrIiwidW5pdmVyc2UiLCJj"
				+ "b21tYW5kIiwiY291bnQiLCJwcmFjdGljZSIsImNvbmRpdGlvbiIsIm5lZWRlZCIsInllcyIsImFncmVlIiwidGFsb"
				+ "CIKIndvdWxkIiwiY2F2ZSIsInN0cm9uZyIsIndpc2UiLCJtb29uIiwiYW5jaWVudCIsInRvb2siLCJjdXAiLCJzdG"
				+ "F0ZW1lbnQiLCJzaW1wbGUiLCJ3ZWlnaCIsImhlcmQiLCJyZWd1bGFyIiwiZG9sbCIsImNhcmVmdWxseSIsInNoaW5"
				+ "uaW5nIiwiY3V0IiwiY29uZGl0aW9uIiwic3Rvcm0iCiJ0YWtlbiIsIm5laWdoYm9yaG9vZCIsImJyZWFkIiwic2No"
				+ "b29sIiwiZHVsbCIsInJhdyIsImZvdW5kIiwicmVtYXJrYWJsZSIsImluZGljYXRlIiwic2hvcmUiLCJyZWFkeSIsI"
				+ "mRpdmlzaW9uIiwidG9kYXkiLCJ0aG9zZSIsImFnbyIsImNoYW5naW5nIiwiY293IiwiaGlkZGVuIiwid29sZiIKIm"
				+ "Rlc2lnbiIsInN0YXkiLCJjbG90aGluZyIsImJlbHQiLCJzdW5saWdodCIsInVzZSIsInByaW50ZWQiLCJzb2Z0bHk"
				+ "iLCJiYWNrIiwibWluZXJhbHMiLCJjdXN0b21zIiwidG9vIiwidGFzdGUiLCJmb3JnZXQiLCJwcmFjdGljYWwiLCJn"
				+ "cmFwaCIsImF2b2lkIiwibGllIiwiZ3JheSIKInRyYWNrIiwiYm94IiwibWFpbiIsImpvYiIsImdsb2JlIiwieW91b"
				+ "mdlciIsImZpZXJjZSIsImJvdW5kIiwibWFraW5nIiwiYW1vdW50IiwiZ3JhcGgiLCJtaXNzaW5nIiwidGhhbiIsIm"
				+ "RhdWdodGVyIiwicGluayIsImdvdmVybm1lbnQiLCJtYXkiLCJpbXByb3ZlIiwicXVpY2siCiJzcGVjaWVzIiwiYmV"
				+ "oYXZpb3IiLCJmcmVxdWVudGx5IiwiY2hpbGQiLCJwcm9ibGVtIiwiYm93IiwidHJvdWJsZSIsInN5bGxhYmxlIiwi"
				+ "bmlnaHQiLCJhY3Rpdml0eSIsImNoYXJnZSIsImVhcm4iLCJvY2VhbiIsImxpcHMiLCJzdGVlcCIsInNob3VsZGVyI"
				+ "iwiYWRkaXRpb24iLCJzcGVjaWVzIiwiYWxpdmUiCiJkYXJrbmVzcyIsIm5vbmUiLCJkb2luZyIsImVudmlyb25tZW"
				+ "50IiwiY29uc3RhbnRseSIsImdhdGhlciIsImZpbmFsbHkiLCJsZW5ndGgiLCJ3aW5kIiwiYmxhbmtldCIsImJpdCI"
				+ "sImJhbGFuY2UiLCJmdXJ0aGVyIiwiZGFya25lc3MiLCJzb21ld2hlcmUiLCJzdGFpcnMiLCJzcGVlY2giLCJkaXJl"
				+ "Y3QiLCJjYXIiCiJ3aGVlbCIsImdyYW5kbW90aGVyIiwiZ2FtZSIsImV4YWN0IiwibWl4dHVyZSIsInRyYW5zcG9yd"
				+ "GF0aW9uIiwiZ2V0IiwiZXZpZGVuY2UiLCJzdWJqZWN0Iiwic2NvcmUiLCJyYWJiaXQiLCJzcGlyaXQiLCJ0ZWxsIi"
				+ "wic29tZXRoaW5nIiwib3JnYW5pemF0aW9uIiwiZnJvemVuIiwiYmV0dGVyIiwib250byIsInBpZSIKInN1Z2FyIiw"
				+ "iYmxhbmtldCIsImF2ZXJhZ2UiLCJvZmZpY2UiLCJjcmVhdHVyZSIsImZhaXJseSIsImJlbmQiLCJ3YXJtIiwiY2ly"
				+ "Y3VzIiwiYWlyIiwiZGlubmVyIiwicmFkaW8iLCJwb3B1bGF0aW9uIiwiY291cnNlIiwic2xpcHBlZCIsIm1ldGFsI"
				+ "iwic3RpbGwiLCJwb2xpY2VtYW4iLCJzdGVlcCI=").getBytes(StandardCharsets.UTF_8);
	}
}