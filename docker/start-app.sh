#!/bin/bash

JAR_NAME="${JAR_NAME:?JAR_NAME variable not defined}"

java -Dconfig.resource=application.conf -Dlogback.configurationFile=logback-klogging.xml -Dlogback.debug=true ${JAVA_OPTS}  -jar "${JAR_NAME}" $@
