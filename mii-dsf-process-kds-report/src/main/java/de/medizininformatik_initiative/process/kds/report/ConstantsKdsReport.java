package de.medizininformatik_initiative.process.kds.report;

import static de.medizininformatik_initiative.process.kds.report.KdsReportProcessPluginDefinition.VERSION;

public interface ConstantsKdsReport
{
	String BPMN_EXECUTION_VARIABLE_KDS_REPORT_TIMER_INTERVAL = "kdsReportTimerInterval";
	String BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE = "kdsReportSearchBundle";
	String BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE_RESPONSE_REFERENCE = "kdsReportSearchBundleResponseReference";
	String BPMN_EXECUTION_VARIABLE_KDS_REPORT_RECEIVE_ERROR = "kdsReportReceiveError";

	String KDS_REPORT_TIMER_INTERVAL_DEFAULT_VALUE = "P7D";
	String FHIR_STORE_TYPE_BLAZE = "blaze";

	String PROFILE_KDS_REPORT_SEARCH_BUNDLE = "http://medizininformatik-initiative.de/fhir/Bundle/mii-kds-report-search-bundle"
			+ "|" + VERSION;
	String PROFILE_KDS_REPORT_SEARCH_BUNDLE_RESPONSE = "http://medizininformatik-initiative.de/fhir/Bundle/mii-kds-report-search-bundle-response"
			+ "|" + VERSION;
	String EXTENSION_KDS_REPORT_STATUS_ERROR_URL = "http://medizininformatik-initiative.de/fhir/StructureDefinition/extension-mii-kds-report-status-error";

	String CODESYSTEM_MII_KDS_REPORT = "http://medizininformatik-initiative.de/fhir/CodeSystem/kds-report";
	String CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE = "search-bundle";
	String CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE = "search-bundle-response-reference";
	String CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS = "kds-report-status";
	String CODESYSTEM_MII_KDS_REPORT_VALUE_TIMER_INTERVAL = "timer-interval";

	String CODESYSTEM_MII_KDS_REPORT_STATUS = "http://medizininformatik-initiative.de/fhir/CodeSystem/kds-report-status";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_ALLOWED = "not-allowed";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_REACHABLE = "not-reachable";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_MISSING = "receipt-missing";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_OK = "receipt-ok";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_ERROR = "receipt-error";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_OK = "receive-ok";
	String CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR = "receive-error";

	int DSF_CLIENT_RETRY_TIMES = 6;
	long DSF_CLIENT_RETRY_INTERVAL_5MIN = 300000;

	String PROCESS_NAME_KDS_REPORT_AUTOSTART = "kdsReportAutostart";
	String PROCESS_NAME_KDS_REPORT_RECEIVE = "kdsReportReceive";
	String PROCESS_NAME_KDS_REPORT_SEND = "kdsReportSend";

	String PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART = "medizininformatik-initiativede_"
			+ PROCESS_NAME_KDS_REPORT_AUTOSTART;
	String PROCESS_NAME_FULL_KDS_REPORT_RECEIVE = "medizininformatik-initiativede_" + PROCESS_NAME_KDS_REPORT_RECEIVE;
	String PROCESS_NAME_FULL_KDS_REPORT_SEND = "medizininformatik-initiativede_" + PROCESS_NAME_KDS_REPORT_SEND;

	String PROCESS_MII_URI_BASE = "http://medizininformatik-initiative.de/bpe/Process/";

	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-kds-report-task-autostart-start";
	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START
			+ "|" + VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_PROCESS_URI = PROCESS_MII_URI_BASE
			+ PROCESS_NAME_KDS_REPORT_AUTOSTART + "/";
	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_START_MESSAGE_NAME = "kdsReportAutostartStart";

	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_STOP = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-kds-report-task-autostart-stop";
	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_STOP_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_STOP
			+ "|" + VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_AUTOSTART_STOP_MESSAGE_NAME = "kdsReportAutostartStop";

	String PROFILE_MII_KDS_REPORT_TASK_SEND_START = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-kds-report-task-send-start";
	String PROFILE_MII_KDS_REPORT_TASK_SEND_START_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_SEND_START + "|"
			+ VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_SEND_START_PROCESS_URI = PROCESS_MII_URI_BASE + PROCESS_NAME_KDS_REPORT_SEND
			+ "/";
	String PROFILE_KDS_MII_REPORT_TASK_SEND_START_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_SEND_START_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_SEND_START_MESSAGE_NAME = "kdsReportSendStart";

	String PROFILE_MII_KDS_REPORT_TASK_SEND = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-kds-report-task-send";
	String PROFILE_MII_KDS_REPORT_TASK_SEND_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_SEND + "|" + VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_SEND_PROCESS_URI = PROCESS_MII_URI_BASE + PROCESS_NAME_KDS_REPORT_RECEIVE + "/";
	String PROFILE_MII_KDS_REPORT_TASK_SEND_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_SEND_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_SEND_MESSAGE_NAME = "kdsReportSend";

	String PROFILE_MII_KDS_REPORT_TASK_RECEIVE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-kds-report-task-receive";
	String PROFILE_MII_KDS_REPORT_TASK_RECEIVE_AND_LATEST_VERSION = PROFILE_MII_KDS_REPORT_TASK_RECEIVE + "|" + VERSION;
	String PROFILE_MII_KDS_REPORT_TASK_RECEIVE_MESSAGE_NAME = "kdsReportReceive";
}
