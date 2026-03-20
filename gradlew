#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME=`pwd -P`

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

die () {
    echo "$*"
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )  cygwin=true  ;;
  Darwin*  ) darwin=true  ;;
  MSYS*    ) msys=true    ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi

if [ ! -x "$JAVACMD" ] ; then
    die "ERROR: JAVA_HOME is not set and no 'java' command could be found."
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
