@REM Maven wrapper script for Windows
@echo off

if "%JAVA_HOME%"=="" (
  echo Please set JAVA_HOME && exit /b 1
)

set MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties

for /f "tokens=2 delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do (
  set DISTRIBUTION_URL=%%a
)

set MAVEN_USER_HOME=%USERPROFILE%\.m2\wrapper\dists
set MAVEN_HOME=%MAVEN_USER_HOME%\apache-maven

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  mkdir "%MAVEN_HOME%"
  echo Downloading Maven...
  powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_HOME%\maven.zip'"
  powershell -Command "Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force"
  del "%MAVEN_HOME%\maven.zip"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
