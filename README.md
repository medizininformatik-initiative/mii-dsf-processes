# MII DSF Processes

Business processes for the Medical Informatics Initiative as plugins for the [HiGHmed Data Sharing Framework][1].

The [Projectathon Data-Transfer process](https://github.com/medizininformatik-initiative/mii-process-projectathon/tree/main/mii-process-projectathon-data-transfer)
is greatly influenced by the [NUM-CODEX AP1 Data-Transfer process](https://github.com/num-codex/codex-processes-ap1) and
reuses some of its code.

## Build

Prerequisite: Java 11, Maven >= 3.6

Add the Github Package Registry server to your Maven `.m2/settings.xml`. Instructions on how to generate the `USERNAME`
and `TOKEN` can be found in the HiGHmed DSF Wiki page with the
name [Using the Github Maven Package Registry](https://github.com/highmed/highmed-dsf/wiki/Using-the-Github-Maven-Package-Registry)
. The token need at least the `read:packages` scope.

```xml

<servers>
	<server>
		<id>highmed-dsf</id>
		<username>USERNAME</username>
		<password>TOKEN</password>
	</server>
</servers>
```

Build the project from the root directory of this repository by executing the following command.

```sh
mvn clean package
``` 

## License

Copyright 2022 Medical Informatics Initiative

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

[1]: <https://github.com/highmed/highmed-dsf>
