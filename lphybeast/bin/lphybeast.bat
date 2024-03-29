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
@rem  lphybeast launcher for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem It is preferable to place lphybeast inside the BEAST2 bin folder
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set BIN_FOLDER=%DIRNAME%

@rem Resolve any "." and ".." in BIN_FOLDER to make it shorter.
for %%i in ("%BIN_FOLDER%") do set BIN_FOLDER=%%~fi

:setBEAST
@rem
set BEAST=%BIN_FOLDER%..
echo set the BEAST root dir to "%BEAST%"

:setBEASTLIB
set BEAST_LIB=%BEAST%\lib
echo set the BEAST_LIB to "%BEAST_LIB%"

if exist "%BEAST%\jre" (
  @rem Use Zulu JRE in the BEAST directory if it exists
  set "JAVA=%BEAST%\jre\bin\java"
) else if defined JAVA_HOME (
  @rem Else use %JAVA_HOME%
  set "JAVA=%JAVA_HOME%\bin\java"
) else (
  @rem Else use java from the PATH
  set "JAVA=java"
)

@rem Print Java version
"%JAVA%" -version

@rem $LPHY_LIB must be provided
if not defined LPHY_LIB (
  @rem If %LPHY_LIB% does not exist, detect the OS
  if /I not x%OS:Windows=%==x%OS% (
    echo Guessing LPHY_LIB on Windows ...
    set "OS_PREFIX=C:\Program Files"

   goto checkx86
  
  ) else (
    echo Cannot guess the LPhy library path LPHY_LIB in unknown OS: %OS% !
    echo Please set it as your environment variable.
    echo Get help from https://linguaphylo.github.io/setup
    exit /b 1
  )
) else ( goto validations )


:checkx86
dir /b "%OS_PREFIX%\lphy*studio-1*" > nul
if errorlevel 1 (
    set "OS_PREFIX=C:\Program Files (x86)"
    echo Cannot find the lphy installation in C:\Program Files, try %"OS_PREFIX%.
)

 @rem If multiple LPhy installations are found, ensure to get the latest version, usually the last line
 for /f "delims=" %%i in ('dir /b /ad "%OS_PREFIX%\lphy*studio-1*"') do set "LPHY_DIR=%OS_PREFIX%\%%i"
  @rem Guess LPHY_LIB
  set "LPHY_LIB=%LPHY_DIR%\lib"

:validations
if not exist "%LPHY_LIB%" (
  echo Error: Invalid LPhy library path: %LPHY_LIB% !
  echo Please set an existing LPhy library path to LPHY_LIB as your environment variable.
  echo Get help from https://linguaphylo.github.io/setup
  exit /b 1
)

echo BEAST_LIB = %BEAST_LIB%
echo BEAST_EXTRA_LIBS = %BEAST_EXTRA_LIBS%
echo LPHY_LIB = %LPHY_LIB%
echo.

@rem Use the "dir" command to check for lphy*.jar files in the %LPHY_LIB% folder
dir /b "%LPHY_LIB%\lphy*.jar" > nul
@rem Check the errorlevel to determine if any lphy*.jar files were found
if "%ERRORLEVEL%"=="0" goto execute

echo Error: No core jar files found in %LPHY_LIB% ! Please install LPhy properly. 
goto fail

:execute
@rem Must set -Dpicocli.disable.closures=true using picocli:4.7.0
@rem Otherwise, it will throw java.lang.NoClassDefFoundError: groovy.lang.Closure
@rem -Dfile.encoding=UTF8 makes the special (e.g., Greek) letters logged properly
set "ARG=-Xms256m -Xmx60g -Dpicocli.disable.closures=true -Dlauncher.wait.for.exit=true -Dfile.encoding=UTF8 -Duser.language=en"

@rem Setup the command line
set CMD="%JAVA%" %ARG% -cp "%BEAST_LIB%\launcher.jar;%LPHY_LIB%\*" beast.pkgmgmt.launcher.AppLauncherLauncher lphybeast %*
@rem Execute
%CMD%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
echo.
echo ERROR: fail to run the command
echo %CMD%
echo.
echo Get help from https://linguaphylo.github.io/setup

:mainEnd
if "%OS%"=="Windows_NT" endlocal


