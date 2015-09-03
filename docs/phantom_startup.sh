#!/bin/bash

FK_USER=fk-w3-agent
FK_GROUP=fk-w3
FK_UID=$(id --user $FK_USER)
FK_GID=$(id --group $FK_USER)

LOG_DIR=/var/log/flipkart/w3/agent
PAC=fk-w3-phantom
FQHOSTNAME=`hostname -f`

# Jave home constants
ORACLE_JAVA8_HOME=/usr/lib/jvm/j2sdk1.8-oracle
ORACLE_JAVA7_HOME=/usr/lib/jvm/j2sdk1.7-oracle
ORACLE_JAVA6_HOME=/usr/lib/jvm/j2sdk1.6-oracle
SUN_JAVA6_HOME=/usr/lib/jvm/java-6-sun

# Set java home (in order of priority if multiple java installations present)
if [ -d "$ORACLE_JAVA8_HOME" ]; then
    export JAVA_HOME="$ORACLE_JAVA8_HOME"
elif [ -d "$ORACLE_JAVA7_HOME" ]; then
    export JAVA_HOME="$ORACLE_JAVA7_HOME"
elif [ -d "$ORACLE_JAVA6_HOME" ]; then
    export JAVA_HOME="$ORACLE_JAVA6_HOME"
elif [ -d "$SUN_JAVA6_HOME" ]; then
    export JAVA_HOME="$SUN_JAVA6_HOME"
fi

# Set the java command if java home was set
# else let the system pick default java command
if [ -z $JAVA_HOME ]; then
    JAVA_CMD=java
else
   JAVA_CMD="${JAVA_HOME}/bin/java"
fi

JAVA_OPTS="${JAVA_OPTS} -Xms8g"
JAVA_OPTS="${JAVA_OPTS} -Xmx8g"

# Set Server JVM
JAVA_OPTS="${JAVA_OPTS} -server"

# Set to headless, just in case
JAVA_OPTS="${JAVA_OPTS} -Djava.awt.headless=true"

#Set encoding to UTF-8
JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8"

# Force the JVM to use IPv4 stack
JAVA_OPTS="${JAVA_OPTS} -Djava.net.preferIPv4Stack=true"

# Set language and region
JAVA_OPTS="${JAVA_OPTS} -Duser.language=en -Duser.region=CA"

# GC Options
if [ "$JAVA_HOME" == "$ORACLE_JAVA8_HOME" ]; then
    JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
else
    JAVA_OPTS="${JAVA_OPTS} -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled"
    JAVA_OPTS="${JAVA_OPTS} -XX:NewSize=300m -XX:MaxNewSize=600m -XX:PermSize=128m -XX:MaxPermSize=128m"
fi
JAVA_OPTS="${JAVA_OPTS} -verbose:gc  -XX:+PrintTenuringDistribution"

# Flight Recorder
if [ "$JAVA_HOME" == "$ORACLE_JAVA8_HOME" -o "$JAVA_HOME" == "$ORACLE_JAVA7_HOME" ]; then
    JAVA_OPTS="${JAVA_OPTS} -XX:+UnlockCommercialFeatures"
    JAVA_OPTS="${JAVA_OPTS} -XX:+FlightRecorder"
fi

#Setup remote JMX monitoring
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote"
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.authenticate=false"
JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.ssl=false"
JAVA_OPTS="${JAVA_OPTS} -Djava.rmi.server.hostname=$FQHOSTNAME"
JAVA_OPTS="${JAVA_OPTS} -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintGCDetails"

JMX_LOGAGENT_PORT=25021
LOGAGENT_JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.port=$JMX_LOGAGENT_PORT"
LOGAGENT_JAVA_OPTS="${LOGAGENT_JAVA_OPTS} -Xloggc:/var/log/flipkart/w3/$PAC/gc.$(date +%Y-%m-%d).log  "

LOGAGENT_JAR="/usr/share/$PAC/log-agent.jar"
LOGAGENT_CMD="$JAVA_CMD  $LOGAGENT_JAVA_OPTS  -classpath "$LOGAGENT_JAR:/usr/share/$PAC/log-agent/:/usr/share/$PAC/log-agent/lib/logback-classic-1.0.5.jar:/usr/share/$PAC/log-agent/lib/logback-core-1.0.5.jar:/usr/share/$PAC/log-agent/lib/slf4j-api-1.6.4.jar:/usr/share/$PAC/log-agent/lib/slf4j-log4j12-1.6.4.jar:/usr/share/$PAC/log-agent/lib/*" org.trpr.platform.runtime.impl.bootstrap.BootstrapLauncher /usr/share/$PAC/log-agent/resources/external/bootstrap.xml"

JMX_SVCPROXY_PORT=25020
SVCPROXY_JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote.port=$JMX_SVCPROXY_PORT"
SVCPROXY_JAVA_OPTS="${SVCPROXY_JAVA_OPTS} -Xloggc:/var/log/flipkart/w3/$PAC/gc-service-proxy.$(date +%Y-%m-%d).log"
SVCPROXY_JAR="/usr/share/$PAC/service-proxy.jar"
SVCPROXY_CMD="$JAVA_CMD $SVCPROXY_JAVA_OPTS -classpath "$SVCPROXY_JAR:/usr/share/$PAC/service-proxy/:/usr/share/$PAC/service-proxy/lib/logback-classic-1.0.5.jar:/usr/share/$PAC/service-proxy/lib/logback-core-1.0.5.jar:/usr/share/$PAC/service-proxy/lib/slf4j-api-1.6.4.jar:/usr/share/$PAC/service-proxy/lib/slf4j-log4j12-1.6.4.jar:/usr/share/$PAC/service-proxy/lib/*" org.trpr.platform.runtime.impl.bootstrap.BootstrapLauncher /usr/share/$PAC/service-proxy/resources/external/bootstrap.xml"