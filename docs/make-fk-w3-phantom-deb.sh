#!/bin/bash -ex

function die()
{
	echo "Error: $1" >&2
	exit 1
}

function logmsg()
{
	echo "[`date`] $*"
}

[ -z "$LOCAL_DIR" ] && die "No base dir specified"
[ -z "$TARGET" ] && die "No package target specified"
[ -z "$PACKAGE" ] && die "No package name specified"
[ ! -d "$LOCAL_DIR" ] && die "$LOCAL_DIR does not exist"
[ -z "$TARGET" ] && die "Invalid target: $TARGET"

logmsg "Setting Java Home to Java 8"
export JAVA_HOME=/usr/lib/jvm/j2sdk1.8-oracle

pushd $LOCAL_DIR
MVN=mvn
echo `pwd`;
logmsg "Building with $MVN"
$MVN clean package -DskipTests -U
LOCAL_DIR=$LOCAL_DIR/fk-w3-phantom-base
DEB_DIR="$LOCAL_DIR/deb"
mkdir -p $DEB_DIR
cp -r $LOCAL_DIR/DEBIAN $DEB_DIR
cp -r $LOCAL_DIR/usr $DEB_DIR
cp -r $LOCAL_DIR/etc $DEB_DIR


# copy log-agent jar
logmsg "Copying log-agent jars"
mkdir -p $DEB_DIR/usr/share/$PACKAGE/log-agent
jar=$(basename $(ls log-agent-service-proxy/target/log-agent-service-proxy*.jar | tail -n1))
cp log-agent-service-proxy/target/$jar $DEB_DIR/usr/share/$PACKAGE/log-agent
pushd $DEB_DIR/usr/share/$PACKAGE/log-agent && ln -s $jar "log-agent.jar" && popd

# copy log-agent libs
logmsg "Copying log-agent dependent libraries"
LIB_DIR="$DEB_DIR/usr/share/$PACKAGE/log-agent/lib"
mkdir -p "$LIB_DIR" && cp log-agent-service-proxy/target/lib/*jar $LIB_DIR

logmsg "Copying log-agent uds related libraries"
UDS_LIB_DIR="$DEB_DIR/usr/share/$PACKAGE/log-agent/uds-lib"
mkdir -p "$UDS_LIB_DIR" &&  cp log-agent-service-proxy/target/classes/external/uds-lib/* $UDS_LIB_DIR

# copy service-proxy jar
logmsg "Copying service-proxy jars"
mkdir -p $DEB_DIR/usr/share/$PACKAGE/service-proxy
jar=$(basename $(ls service-proxy/target/service-proxy*.jar | tail -n1))
cp service-proxy/target/$jar $DEB_DIR/usr/share/$PACKAGE/service-proxy
pushd $DEB_DIR/usr/share/$PACKAGE/service-proxy && ln -s $jar "service-proxy.jar" && popd

# copy service-proxy libs
logmsg "Copying service-proxy dependent libraries"
LIB_DIR="$DEB_DIR/usr/share/$PACKAGE/service-proxy/lib"
mkdir -p "$LIB_DIR" && cp service-proxy/target/lib/*jar $LIB_DIR

logmsg "Copying service-proxy uds related libraries"
UDS_LIB_DIR="$DEB_DIR/usr/share/$PACKAGE/service-proxy/uds-lib"
mkdir -p "$UDS_LIB_DIR" &&  cp service-proxy/target/classes/external/uds-lib/* $UDS_LIB_DIR

logmsg "Replacing renew-session-ticket-scripts for env : $TARGET"
if [ -f $DEB_DIR/usr/share/$PACKAGE/scripts/renew_session_ticket_$TARGET ]; then
	logmsg "copying $DEB_DIR/usr/share/$PACKAGE/scripts/renew_session_ticket_$TARGET $DEB_DIR/usr/share/$PACKAGE/scripts/renew_session_ticket"
	cp $DEB_DIR/usr/share/$PACKAGE/scripts/renew_session_ticket_$TARGET $DEB_DIR/usr/share/$PACKAGE/scripts/renew_session_ticket
fi

pushd $LOCAL_DIR/..
mv $LOCAL_DIR/deb .

