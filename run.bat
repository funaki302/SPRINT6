@echo off

set "TOMCAT_HOME=D:\XAMPP\tomcat"
set "SERVLET_JAR=%TOMCAT_HOME%\lib\servlet-api.jar"
set "GSON_JAR=framework\lib\gson-2.10.1.jar"
set "TARGET_DIR=%TOMCAT_HOME%\webapps\sprint6"
set "BIN_DIR=bin"

rem Nettoyage
if exist "%BIN_DIR%" rmdir /s /q "%BIN_DIR%"
if exist framework.jar del /f /q framework.jar
if exist test\WEB-INF\classes rmdir /s /q test\WEB-INF\classes
if exist "%TARGET_DIR%" rmdir /s /q "%TARGET_DIR%"

mkdir "%BIN_DIR%"
mkdir test\WEB-INF\classes

rem Compilation Framework & JAR
javac -d "%BIN_DIR%" -cp "%SERVLET_JAR%;%GSON_JAR%" ^
    framework\src\itu\webdynamique\framework\*.java ^
    framework\src\itu\webdynamique\framework\annotation\*.java ^
    framework\src\itu\webdynamique\framework\util\*.java
jar -cvf framework.jar -C "%BIN_DIR%" .

rem Compilation Application Test
javac -parameters -d test\WEB-INF\classes -cp "framework.jar;%SERVLET_JAR%;%GSON_JAR%" test\src\itu\webdynamique\app\controller\*.java

rem Deploiement Tomcat
mkdir "%TARGET_DIR%\WEB-INF\lib"
mkdir "%TARGET_DIR%\WEB-INF\classes"

copy test\WEB-INF\web.xml "%TARGET_DIR%\WEB-INF\"
xcopy /e /y test\WEB-INF\classes\* "%TARGET_DIR%\WEB-INF\classes\"
xcopy /e /y test\WEB-INF\views "%TARGET_DIR%\WEB-INF\views\"
copy framework.jar "%TARGET_DIR%\WEB-INF\lib\"
copy "%GSON_JAR%" "%TARGET_DIR%\WEB-INF\lib\"

echo Deploiement Sprint 6 termine.
