# Testing using Docker Setup

Build the project from the root directory of this repository by executing the following command.

```sh
mvn clean package
```

Add entries to your hosts file

```
127.0.0.1	dic1
127.0.0.1	hrp
```

*A total of five console windows are required. Start docker-compose commands for consoles 1 to 3 from
sub-folder:* `mii-dsf-processes/mii-dsf-processes-docker-test-setup`

Console 1: Start DIC1 HAPI FHIR Server

```sh
docker-compose up dic1-fhir-store-hapi
```

Access at http://localhost:8080/fhir/

Console 2: Start DIC1 DSF FHIR Server and wait till started

```sh
docker-compose up -d dic1-fhir && docker-compose logs -f dic1-fhir
```

Console 2: Disconnect from log output (Ctrl-C) if Server started Console 2: Start DIC1 DSF BPE Server

```sh
docker-compose up -d dic1-bpe && docker-compose logs -f dic1-fhir dic1-bpe
````

Console 3: Start HRP DSF FHIR Server and wait till started

```sh
docker-compose up -d hrp-fhir && docker-compose logs -f hrp-fhir
```

Console 3: Disconnect from log output (Ctrl-C) if Server started Console 3: Start HRP DSF BPE Server

```sh
docker-compose up -d hrp-bpe && docker-compose logs -f hrp-fhir hrp-bpe
````

<!-- EXECUTE PROCESS -->

*Start curl commands in console 4 from root-folder:* `mii-dsf-processes`

Console 4: Add the search Bundle to HRP DSF FHIR Server

```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+xml" \
-d @mii-dsf-process-report/src/test/resources/fhir/Bundle/search-bundle.xml \
--ssl-no-revoke --cacert mii-dsf-processes-test-data-generator/cert/ca/testca_certificate.pem \
--cert mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.pem \
--key mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_private-key.pem \
--pass password \
https://hrp/fhir/Bundle
```

The fullUrl of the returned search bundle (`http://hrp/fhir/Bundle/../_history/..`) needs to pasted into the file 
`mii-dsf-process-report/src/test/resources/fhir/Task/report-send-start-demo.xml` by replacing `<REPLACE-WITH-SEARCH-BUNDLE-URL>`

Console 4: Start Report Send Process at DIC1 using the following command

*Unfortunately this command does not work on Windows. An alternative for starting the process is using WSL or the
example starter class with name* `ReportSendExampleStarter` *in* 
`mii-dsf-process-report/src/test/java/../bpe/start`

```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+xml" \
-d @mii-dsf-process-report/src/test/resources/fhir/Task/report-send-start-demo.xml \
--ssl-no-revoke --cacert mii-dsf-processes-test-data-generator/cert/ca/testca_certificate.pem \
--cert mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.pem \
--key mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_private-key.pem \
--pass password \
https://dic1/fhir/Task
```

Console 4: Check data transferred to HRP

```sh
curl -H "Accept: application/xml+fhir" \
--ssl-no-revoke --cacert mii-dsf-processes-test-data-generator/cert/ca/testca_certificate.pem \
--cert mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.pem \
--key mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_private-key.pem \
--pass password \
https://hrp/fhir/Bundle?identifier=http://medizininformatik-initiative.de/sid/report|Report_Test_DIC1
```

Console 4: Stop everything

```sh
docker-compose down -v
```