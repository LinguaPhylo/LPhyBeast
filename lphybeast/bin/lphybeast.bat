@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  The command line program for running a LPhy script to create simulated data for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. 
set DEFAULT_JVM_OPTS="-Xmx60g" "-Xms256m" ""-Duser.language=en" "-Dpicocli.disable.closures=true"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto setLPHYLIB

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto setLPHYLIB

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:setBEAST
set BEAST=%APP_HOME%\..\
echo set the BEAST root dir to "%BEAST%"

:setBEASTLIB
set BEAST_LIB=%BEAST%\lib
echo set the BEAST_LIB to "%BEAST_LIB%"

for /d %%G in ("%BEAST%\lphy*studio-1*") do (
  set "LPHY_DIR=%%G"
  goto FoundLphyDir
)

set "LPHY_LIB=%BEAST%\lphystudio-1.4.3\lib"
goto Done

:FoundLphyDir
set "LPHY_LIB=%LPHY_DIR%\lib"

:Done
echo set the LPHY_LIB to ""%LPHY_LIB%""



:execute
@rem Setup the command line
@rem Execute 
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -cp "%BEAST_LIB%"\launcher.jar:%LPHY_LIB%\*" beast.pkgmgmt.launcher.AppLauncherLauncher lphybeast %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
echo ERROR: fail to run 
echo "%JAVA_EXE%" %DEFAULT_JVM_OPTS% -cp %BEAST_LIB%"\launcher.jar:%LPHY_LIB%\*" beast.pkgmgmt.launcher.AppLauncherLauncher lphybeast %*


:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
