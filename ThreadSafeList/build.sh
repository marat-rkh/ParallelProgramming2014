SOURCE_FILES_PATHS=app/*
JAR_NAME=lock-free.jar
ENTRY_POINT=app.Main

cd src
javac $SOURCE_FILES_PATHS
jar cfe $JAR_NAME  $ENTRY_POINT $SOURCE_FILES_PATHS.class
rm -r $SOURCE_FILES_PATHS.class
cd ..
mv -t . src/$JAR_NAME 
