@echo off
setlocal
set GRADLE_VERSION=8.14.3
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set BASE=%USERPROFILE%\.gradle\wrapper\dists\gamopanel-gradle-%GRADLE_VERSION%
set BIN=%BASE%\gradle-%GRADLE_VERSION%\bin\gradle.bat
if not exist "%BIN%" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%BASE%\gradle.zip'; Expand-Archive -Path '%BASE%\gradle.zip' -DestinationPath '%BASE%' -Force"
  if errorlevel 1 exit /b 1
)
call "%BIN%" %*
exit /b %ERRORLEVEL%
