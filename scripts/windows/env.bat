@echo off
chcp 65001 >nul
set "PROJECT_ROOT=%~dp0..\.."
cd /d "%PROJECT_ROOT%"
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Users\hero\tools\gradle-9.5.1\bin;%PATH%"
