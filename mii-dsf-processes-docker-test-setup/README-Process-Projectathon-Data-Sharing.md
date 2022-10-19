# Process Projectathon Data Sharing: Testing using Docker Setup

Build the project from the root directory of this repository by executing the following command.

```sh
mvn clean package
```

Add entries to your hosts file

```
127.0.0.1	dic1
127.0.0.1	dic2
127.0.0.1	cos
127.0.0.1	hrp
```

*A total of eight console windows are required. Start docker-compose commands for consoles 1 to 7 from
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

Console 2: Disconnect from log output (Ctrl-C) if Server started
Console 2: Start DIC1 DSF BPE Server

```sh
docker-compose up -d dic1-bpe && docker-compose logs -f dic1-fhir dic1-bpe
```

Console 3: Start DIC2 HAPI FHIR Server

```sh
docker-compose up dic2-fhir-store-hapi
```

Access at http://localhost:8081/fhir/

Console 4: Start DIC2 DSF FHIR Server and wait till started

```sh
docker-compose up -d dic2-fhir && docker-compose logs -f dic2-fhir
```

Console 4: Disconnect from log output (Ctrl-C) if Server started
Console 4: Start DIC2 DSF BPE Server

```sh
docker-compose up -d dic2-bpe && docker-compose logs -f dic2-fhir dic2-bpe
```

Console 5: Start COS HAPI FHIR Server

```sh
docker-compose up cos-fhir-store-hapi
```

Access at http://localhost:8082/fhir/

Console 6: Start COS DSF FHIR Server and wait till started

```sh
docker-compose up -d cos-fhir && docker-compose logs -f cos-fhir
```

Console 6: Disconnect from log output (Ctrl-C) if Server started 
Console 6: Start COS DSF BPE Server

```sh
docker-compose up -d cos-bpe && docker-compose logs -f cos-fhir cos-bpe
```

Console 7: Start HRP DSF FHIR Server and wait till started

```sh
docker-compose up -d hrp-fhir && docker-compose logs -f hrp-fhir
```

Console 7: Disconnect from log output (Ctrl-C) if Server started
Console 7: Start HRP DSF BPE Server

```sh
docker-compose up -d hrp-bpe && docker-compose logs -f hrp-fhir hrp-bpe
````

<!-- EXECUTE PROCESS -->

*Start curl commands in console 8 from root-folder:* `mii-dsf-processes`

Console 8: Execute Demo Transaction-Bundle for DIC1 HAPI FHIR Server

```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+xml" \
-d @mii-dsf-process-projectathon-data-sharing/src/test/resources/fhir/Bundle/Dic1FhirStore_Demo_Bundle.xml \
http://localhost:8080/fhir
```

Console 8: Execute Demo Transaction-Bundle for DIC2 HAPI FHIR Server

```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+xml" \
-d @mii-dsf-process-projectathon-data-sharing/src/test/resources/fhir/Bundle/Dic2FhirStore_Demo_Bundle.xml \
http://localhost:8081/fhir
```

Console 8: Start Data Send Process at HRP using the following command

*Unfortunately this command does not work on Windows. An alternative for starting the process is using WSL or the
example starter class with name* `CoordinateDataSharingExampleStarter` *in* 
`mii-dsf-process-projectathon-data-sharing/src/test/java/../bpe/start`

```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+xml" \
-d @mii-dsf-process-projectathon-data-sharing/src/test/resources/fhir/Task/TaskCoordinateDataSharing_Demo_Bundle.xml \
--ssl-no-revoke --cacert mii-dsf-processes-test-data-generator/cert/ca/testca_certificate.pem \
--cert mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.pem \
--key mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_private-key.pem \
--pass password \
https://hrp/fhir/Task
```

Console 2: Execute DIC1 user-task to release data-set for COS based on the URL in the log output from console 2
Console 4: Execute DIC2 user-task to release data-set for COS based on the URL in the log output from console 4

Console 8: Check data transferred to COS (2 DocumentReferences expected)

```sh
curl http://localhost:8082/fhir/DocumentReference
```

Console 6: Execute COS user-task to release merged data-set for HRP based on the URL in the log output from console 6

console 8 : Check if the Task starting the coordination process at the HRP contains a Task.output with 
            code `data-set-location` containing the URL inserted as part of the user-task at the COS 
```sh
curl -H "Accept: application/xml+fhir" \
--ssl-no-revoke --cacert mii-dsf-processes-test-data-generator/cert/ca/testca_certificate.pem \
--cert mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.pem \
--key mii-dsf-processes-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_private-key.pem \
--pass password \
https://hrp/fhir/Task?_sort=-_lastUpdated
```

Console 8: Stop everything

```sh
cd mii-dsf-processes-docker-test-setup
docker-compose down -v
```