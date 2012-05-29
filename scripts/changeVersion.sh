#!/bin/sh
export MAVEN_OPTS="-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"
export KEVOREE_RELEASE=$1
BASE_RELEASE_DIR=`pwd`

echo "Release version"
echo $KEVOREE_RELEASE

cd $BASE_RELEASE_DIR

#CHANGE TOP VERSION
mvn versions:set -DnewVersion=$KEVOREE_RELEASE -DgenerateBackupPoms=false
