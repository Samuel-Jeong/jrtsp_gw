#!/bin/bash

SERVICE_NAME=jrtspgw
PACKAGE_NAME=jrtsp_gw
SERVICE_HOME=/home/${SERVICE_NAME}/${PACKAGE_NAME}

PATH_TO_JAR="${SERVICE_HOME}/lib/jrtsp_gw-0.0.1.jar"
echo ${PATH_TO_JAR}
if [ ! -f ${PATH_TO_JAR} ]
then
  echo "JAR is not exist. Fail to start [${PATH_TO_JAR}]."
  exit
fi

JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1RSetUpdatingPauseTimePercent=5 -XX:MaxGCPauseMillis=500 -XX:+UseLargePages -verbosegc -verbose:gc -Xlog:gc=debug:file=$SERVICE_HOME/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=100m"
#JAVA_OPT="${JAVA_OPT} -Xms4G -Xmx4G"
JAVA_OPT="${JAVA_OPT} -Dspring.profiles.active=server"
JAVA_OPT="${JAVA_OPT} \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=5884 \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Djava.rmi.server.hostname=0.0.0.0"
JAVA_OPT="${JAVA_OPT} -Dspring.config.location=file:${SERVICE_HOME}/config/application.yml"
JAVA_CONF="--spring.config.name=application"

function exec_start() {
  PID=`ps -ef | grep java | grep ${SERVICE_NAME} | grep -v "grep" | awk '{print $2}'`
  if [ -n "$PID" ]
  then
    echo "[${SERVICE_NAME}] is already running"
  else
    java -jar ${JAVA_OPT} ${PATH_TO_JAR} ${JAVA_CONF} > /dev/null 2>&1 &
    echo "[${SERVICE_NAME}] started ..."
  fi
}

function exec_stop() {
  PID=`ps -ef | grep java | grep ${SERVICE_NAME} | grep -v "grep" | awk '{print $2}'`
  if [ -z "$PID" ]
  then
    echo "[${SERVICE_NAME}] is not running"
  else
    echo "stopping [${SERVICE_NAME}]"

    oldIFS="$IFS"
    IFS='
    '
    IFS=${IFS:0:1}
    ids=($PID)
    IFS="$oldIFS"

    for id in "${ids[@]}"
    do
      echo "--> $id"
      kill "${id}"
    done

    sleep 1
    echo "[${SERVICE_NAME}] stopped"
  fi
}

function exec_status() {
  PID=`ps -ef | grep java | grep ${SERVICE_NAME} | grep -v "grep" | awk '{print $2}'`
	if [ -z "$PID" ]
	then
		echo "[${SERVICE_NAME}] is not running"
	else
		echo "[${SERVICE_NAME}] is running"
	  ps -ef | grep java | grep ${SERVICE_NAME} | grep -v "grep" | awk '{print $2}'
	fi
}

case $1 in
  restart)
    exec_stop
		exec_start
		;;
    start)
		exec_start
    ;;
    stop)
		exec_stop
    ;;
    status)
    exec_status
    ;;
esac
