#!/bin/bash

SOURCE_FILES_PATHS=pool/*
JAR_NAME=pool.jar
ENTRY_POINT=pool.Main

cd src
javac $SOURCE_FILES_PATHS
jar cfe $JAR_NAME  $ENTRY_POINT $SOURCE_FILES_PATHS.class
rm -r $SOURCE_FILES_PATHS.class
cd ..
mv -t . src/$JAR_NAME