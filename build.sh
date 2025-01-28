#!/bin/sh



### EDITABLE SECTION
# java compilers
JAVAC=/run/media/stefano/1f6cfe8b-da05-4d35-9cf6-97e236c81ea1/progetti\ e\ documenti/supervisione\ industriale/jdk-11.0.2/bin/javac
JAVADOC=/run/media/stefano/1f6cfe8b-da05-4d35-9cf6-97e236c81ea1/progetti\ e\ documenti/supervisione\ industriale/jdk-11.0.2/bin/javadoc
JAR=/run/media/stefano/1f6cfe8b-da05-4d35-9cf6-97e236c81ea1/progetti\ e\ documenti/supervisione\ industriale/jdk-11.0.2/bin/jar
JIDL_VERSION=0.8
CLASSPATH="./:../libs/*"


### SECTION YOU SHOULD PROBABLY LEAVE AS IT IS
# change to src directory
cd src

# path to sources
JIDL_PATH=com/github/ilguido

# path to build directory
JIDL_BUILD=../build

# path to java documentation directory
JIDL_DOC=../doc

# source files
COMMON_FILES="$JIDL_PATH/jidl/*.java"
JIDL_FILES="$JIDL_PATH/jidl/variable/*.java"
JIDL_FILES="$JIDL_FILES $JIDL_PATH/jidl/variable/modbus/*.java"
JIDL_FILES="$JIDL_FILES $JIDL_PATH/jidl/variable/opcua/*.java"
JIDL_FILES="$JIDL_FILES $JIDL_PATH/jidl/variable/s7/*.java"
JIDL_FILES="$JIDL_FILES $JIDL_PATH/jidl/variable/json/*.java"
DATA_FILES="$JIDL_PATH/jidl/datalogger/*.java $JIDL_PATH/jidl/datalogger/dataloggerarchiver/*.java $JIDL_PATH/jidl/datalogger/sqlheader/*.java"
CONNECTION_FILES="$JIDL_PATH/jidl/connectionmanager/*.java"
CLIENT_FILES="$JIDL_PATH/jidl/jidlclient/*.java"
UTILS_FILES="$JIDL_PATH/jidl/utils/*.java"
IPC_FILES="$JIDL_PATH/jidl/ipc/*.java"
GUI_FILES="$JIDL_PATH/jidl/gui/*.java $JIDL_PATH/jidl/gui/locale/*.java"
SRC_FILES="$COMMON_FILES $JIDL_FILES $DATA_FILES $CONNECTION_FILES $UTILS_FILES $CLIENT_FILES $IPC_FILES"

# update the version of all files
sed -i "s/@version [0-9]\.[0-9]\{1,2\}/@version $JIDL_VERSION/g" $SRC_FILES $GUI_FILES

# functions
build_jidl() {
"$JAVAC" -Xdiags:verbose -Xlint:deprecation -d $JIDL_BUILD -cp "$CLASSPATH" $SRC_FILES 2>&1 >/dev/null | grep --color 'error:\|$'
}

build_gui() {
"$JAVAC" -Xlint:unchecked -d $JIDL_BUILD -cp "$CLASSPATH" $GUI_FILES 2>&1 >/dev/null | grep --color 'error:\|$'
cp -R $JIDL_PATH/jidl/gui/icons $JIDL_BUILD/$JIDL_PATH/jidl/gui/
}

build_doc() {
"$JAVADOC" -d $JIDL_DOC -cp "$CLASSPATH" $SRC_FILES $GUI_FILES
}

# parse the command line
if [ -z $1 ] || [ $1 = "--help" ]; then
  echo "build.sh [clean|jidl|docs|gui|all|jar]"
elif [ $1 = "clean" ]; then
  rm -R $JIDL_BUILD/com
  rm $JIDL_BUILD/jidl*.jar
elif [ $1 = "jidl" ]; then
  build_jidl
elif [ $1 = "gui" ]; then
  build_gui
elif [ $1 = "docs" ]; then
  build_doc
elif [ $1 = "all" ]; then
  build_jidl
  build_gui
  build_doc
elif [ $1 = "jar" ]; then
  cd $JIDL_BUILD
  "$JAR" cfm jidl-$JIDL_VERSION.jar ../manifest com/
else
  echo "unknown parameter $1"
  echo .
fi

cd ..
