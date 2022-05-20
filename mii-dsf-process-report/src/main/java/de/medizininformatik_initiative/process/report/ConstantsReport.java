package de.medizininformatik_initiative.process.report;

import static de.medizininformatik_initiative.process.report.ReportProcessPluginDefinition.VERSION;

public interface ConstantsReport
{
	String PROCESS_NAME_REPORT_AUTOSTART = "reportAutostart";
	String PROCESS_NAME_REPORT_RECEIVE = "reportReceive";
	String PROCESS_NAME_REPORT_SEND = "reportSend";

	String PROCESS_NAME_FULL_REPORT_AUTOSTART = "medizininformatik-initiativede_" + PROCESS_NAME_REPORT_AUTOSTART;
	String PROCESS_NAME_FULL_REPORT_RECEIVE = "medizininformatik-initiativede_" + PROCESS_NAME_REPORT_RECEIVE;
	String PROCESS_NAME_FULL_REPORT_SEND = "medizininformatik-initiativede_" + PROCESS_NAME_REPORT_SEND;

	String BPMN_EXECUTION_VARIABLE_REPORT_TIMER_INTERVAL = "reportTimerInterval";
	String BPMN_EXECUTION_VARIABLE_REPORT_STOP_TIMER = "reportStopTimer";
	String BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE = "searchBundleReference";
	String BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE = "searchBundle";
	String BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE = "searchBundleResponseReference";

	String REPORT_TIMER_INTERVAL_DEFAULT_VALUE = "P1M";

	String EXTENSION_REPORT_STATUS_ERROR_URL = "http://medizininformatik-initiative.de/fhir/StructureDefinition/extension-mii-report-status-error";

	String NAMING_SYSTEM_MII_REPORT = "http://medizininformatik-initiative.de//sid/report";

	String CODESYSTEM_MII_REPORT = "http://medizininformatik-initiative.de/fhir/CodeSystem/report";
	String CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE = "search-bundle-reference";
	String CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE = "search-bundle-response-reference";
	String CODESYSTEM_MII_REPORT_VALUE_REPORT_STATUS = "report-status";
	String CODESYSTEM_MII_REPORT_VALUE_TIMER_INTERVAL = "timer-interval";

	String CODESYSTEM_MII_REPORT_STATUS = "http://medizininformatik-initiative.de/fhir/CodeSystem/report-status";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_NOT_ALLOWED = "not-allowed";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_NOT_REACHABLE = "not-reachable";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_MISSING = "receipt-missing";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_OK = "receipt-ok";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIPT_ERROR = "receipt-error";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIVE_OK = "receive-ok";
	String CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIVE_ERROR = "receive-error";

	String PROCESS_MII_URI_BASE = "http://medizininformatik-initiative.de/bpe/Process/";

	String PROFILE_MII_REPORT_TASK_AUTOSTART_START = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-report-task-autostart-start";
	String PROFILE_MII_REPORT_TASK_AUTOSTART_START_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_AUTOSTART_START + "|"
			+ VERSION;
	String PROFILE_MII_REPORT_TASK_AUTOSTART_START_PROCESS_URI = PROCESS_MII_URI_BASE + PROCESS_NAME_REPORT_AUTOSTART
			+ "/";
	String PROFILE_MII_REPORT_TASK_AUTOSTART_START_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_AUTOSTART_START_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_REPORT_TASK_AUTOSTART_START_MESSAGE_NAME = "reportAutostartStart";

	String PROFILE_MII_REPORT_TASK_AUTOSTART_STOP = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-report-task-autostart-stop";
	String PROFILE_MII_REPORT_TASK_AUTOSTART_STOP_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_AUTOSTART_STOP + "|"
			+ VERSION;
	String PROFILE_MII_REPORT_TASK_AUTOSTART_STOP_MESSAGE_NAME = "reportAutostartStop";

	String PROFILE_MII_REPORT_TASK_SEND_START = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-report-task-send-start";
	String PROFILE_MII_REPORT_TASK_SEND_START_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_SEND_START + "|" + VERSION;
	String PROFILE_MII_REPORT_TASK_SEND_START_PROCESS_URI = PROCESS_MII_URI_BASE + PROCESS_NAME_REPORT_SEND + "/";
	String PROFILE_MII_REPORT_TASK_SEND_START_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_SEND_START_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_REPORT_TASK_SEND_START_MESSAGE_NAME = "reportSendStart";

	String PROFILE_MII_REPORT_TASK_SEND = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-report-task-send";
	String PROFILE_MII_REPORT_TASK_SEND_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_SEND + "|" + VERSION;
	String PROFILE_MII_REPORT_TASK_SEND_PROCESS_URI = PROCESS_MII_URI_BASE + PROCESS_NAME_REPORT_RECEIVE + "/";
	String PROFILE_MII_REPORT_TASK_SEND_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_SEND_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_REPORT_TASK_SEND_MESSAGE_NAME = "reportSend";

	String PROFILE_MII_REPORT_TASK_RECEIVE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/mii-report-task-receive";
	String PROFILE_MII_REPORT_TASK_RECEIVE_AND_LATEST_VERSION = PROFILE_MII_REPORT_TASK_RECEIVE + "|" + VERSION;
	String PROFILE_MII_REPORT_TASK_RECEIVE_MESSAGE_NAME = "reportReceive";
}
