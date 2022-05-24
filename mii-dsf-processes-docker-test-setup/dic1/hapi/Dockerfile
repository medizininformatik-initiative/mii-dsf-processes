FROM hapiproject/hapi:v5.6.0
WORKDIR /usr/local/tomcat/webapps
RUN apt update && apt install unzip && unzip ROOT.war -d ROOT
COPY ./sample-logo.jpg /usr/local/tomcat/webapps/ROOT/img/sample-logo.jpg