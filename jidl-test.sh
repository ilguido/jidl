#!/bin/sh

### EDITABLE SECTION
# java command
JAVA=java

# dependencies
LIBS='../libs/plc4j-api-0.10.0.jar:'\
'../libs/plc4j-driver-modbus-0.10.0.jar:'\
'../libs/plc4j-driver-opcua-0.10.0.jar:'\
'../libs/plc4j-driver-s7-0.10.0.jar:'\
'../libs/plc4j-spi-0.10.0.jar:'\
'../libs/jackson-annotations-2.13.3.jar:'\
'../libs/slf4j-api-2.0.3.jar:'\
'../libs/slf4j-simple-2.0.3.jar:'\
'../libs/plc4j-transport-tcp-0.10.0.jar:'\
'../libs/commons-codec-1.15.jar:'\
'../libs/commons-lang3-3.12.0.jar:'\
'../libs/bcprov-jdk15on-1.70.jar:'\
'../libs/netty-transport-4.1.82.Final.jar:'\
'../libs/netty-common-4.1.82.Final.jar:'\
'../libs/netty-resolver-4.1.82.Final.jar:'\
'../libs/netty-buffer-4.1.82.Final.jar:'\
'../libs/netty-handler-4.1.82.Final.jar:'\
'../libs/netty-codec-4.1.82.Final.jar:'\
'../libs/vavr-0.10.4.jar:'\
'../libs/bit-io-1.4.3.jar:'\
'../libs/json-simple-4.0.1.jar:'\
'../libs/sqlite-jdbc-3.36.0.3.jar:'\
'../libs/monetdb-jdbc-3.2.jre8.jar:'\
'../libs/mariadb-java-client-3.5.1.jar'


### SECTION YOU SHOULD PROBABLY LEAVE AS IT IS
cd build

# parse the command line
if [ -z $1 ] || [ $1 = "--help" ]; then
  echo "jidl-test.sh [shell|gui|jar-shell|jar-gui]"
elif [ $1 = "shell" ]; then
  "$JAVA" -cp "./:${LIBS}" com.github.ilguido.jidl.jidlcl -a -c test/dummy.ini
elif [ $1 = "gui" ]; then
  "$JAVA" -cp "./:${LIBS}" com.github.ilguido.jidl.gui.jidlss
elif [ $1 = "jar-shell" ]; then
  "$JAVA" -cp "jidl-0.8.jar:../${LIBS}" com.github.ilguido.jidl.jidlcl -a -c test/dummy.ini
elif [ $1 = "jar-gui" ]; then
  "$JAVA" -cp "jidl-0.8.jar:${LIBS}" com.github.ilguido.jidl.gui.jidlss
else
  echo "unknown parameter $1"
  echo .
fi

cd ..
