@echo off
@title %0

@setlocal

@REM checkPreconditions
where mvn > NUL
if not %ERRORLEVEL% == 0 (
	echo It seems you do not have maven installed. Please ensure you have it installed and executable as "mvn".
	exit /B 1
)

where java > NUL
if not %ERRORLEVEL% == 0 (
	echo It seems you do not have java installed. Please ensure you have it installed and executable as "java".
	exit /B 1
)
if not exist target (
	md target
)

:preconditions_ok
if not "%*" == "" goto parse_args

:print_usage
echo usage: call %0 with the following options:
echo 	--test	to run the tests
echo 	--testUi	to run the tests including UI tests
echo 	--installCore to install JMC core
echo 	--packageJmc	to package JMC
echo 	--packageAgent to package Agent
echo 	--runAgentExample to run Agent 'InstrumentMe' example, once it is packaged
echo 	--runAgentConverterExample to run Agent 'InstrumentMeConverter' example, once it is packaged
echo 	--clean	to run maven clean
echo 	--run	to run JMC, once it is packaged
echo 	--help	to show this help dialog
exit /B 0

:parse_args
if "%1" == "--help" goto print_usage
if "%1" == "--test" goto test
if "%1" == "--testUi" goto testUi
if "%1" == "--installCore" goto installCore
if "%1" == "--packageJmc" goto packageJmc
if "%1" == "--packageAgent" goto packageAgent
if "%1" == "--runAgentExample" goto runAgentExample
if "%1" == "--runAgentConverterExample" goto runAgentConverterExample
if "%1" == "--clean" goto clean
if "%1" == "--run" goto run
echo unknown argument %1
goto print_usage

:startJetty
for /f "skip=1" %%A in ('wmic os get localdatetime ^| findstr .') do (set LOCALDATETIME=%%A)
set TIMESTAMP=%LOCALDATETIME:~0,14%
set P2_SITE_LOG=%cd%\build_%TIMESTAMP%.1.p2_site.log
set JETTY_LOG=%cd%\build_%TIMESTAMP%.2.jetty.log
echo %time% building p2:site - logging output to %P2_SITE_LOG%
call mvn -f releng\third-party\pom.xml p2:site --log-file "%P2_SITE_LOG%"
if not %ERRORLEVEL% == 0 (
	echo p2:site build failed!
	exit /B 1
)
echo %time% run jetty - logging output to %JETTY_LOG%
start "%1" cmd /C "mvn -f releng\third-party\pom.xml jetty:run --log-file %JETTY_LOG%"
:wait_jetty
echo Waiting for jetty server to start
timeout /t 1
findstr "[INFO] Started Jetty Server" %JETTY_LOG%
if not %ERRORLEVEL% == 0 goto :wait_jetty
echo %time% jetty server up and running
call :installCore
if not %ERRORLEVEL% == 0 (
	call :killJetty %1
	exit /B 1
)
exit /B 0

:installCore
for /f "skip=1" %%A in ('wmic os get localdatetime ^| findstr .') do (set LOCALDATETIME=%%A)
set TIMESTAMP=%LOCALDATETIME:~0,14%
set INSTALL_LOG=%cd%\build_%TIMESTAMP%.3.install.log
echo %time% installing core artifacts - logging output to %INSTALL_LOG%
call mvn -f core\pom.xml clean install --log-file "%INSTALL_LOG%"
if not %ERRORLEVEL% == 0 (
	echo installing core artifacts failed!
	exit /B 1
)
exit /B 0

@REM Kill the console based on title passed as first arg (%1)
@REM tasklist gives us the pid, and using unique id on window title to filter the list
:killJetty
echo kill jetty
for /F "tokens=2 delims=," %%R in ('tasklist /FI "Windowtitle eq %1" /NH /FO csv') do (
	taskkill /PID %%R
)
exit /B 0

:test
echo %time% running tests
call mvn verify
goto end

:testUi
@REM generate a unique id for window title
@REM allow to filter uniquely to get PID associated later
set JETTY_TITLE=jmc-jetty-%time%
call :startJetty %JETTY_TITLE%
if not %ERRORLEVEL% == 0 (
	exit /B 1
)
echo %time% running UI tests
call mvn verify -P uitests
call :killJetty %JETTY_TITLE%
goto end

:packageJmc
@REM generate a unique id for window title
@REM allow to filter uniquely to get PID associated later
set JETTY_TITLE=jmc-jetty-%time%
call :startJetty %JETTY_TITLE%
if not %ERRORLEVEL% == 0 (
	exit /B 1
)
for /f "skip=1" %%A in ('wmic os get localdatetime ^| findstr .') do (set LOCALDATETIME=%%A)
set TIMESTAMP=%LOCALDATETIME:~0,14%
set PACKAGE_LOG=%cd%\build_%TIMESTAMP%.4.package.log
echo %time% packaging jmc - logging output to %PACKAGE_LOG%
call mvn package --log-file "%PACKAGE_LOG%"
if %ERRORLEVEL% == 0 echo You can now run jmc by calling "%0 --run" or "%cd%\target\products\org.openjdk.jmc\win32\win32\x86_64\JDK Mission Control\jmc.exe"
call :killJetty %JETTY_TITLE%
goto end

:packageAgent
@REM generate a unique id for window title
@REM allow to filter uniquely to get PID associated later
for /f "skip=1" %%A in ('wmic os get localdatetime ^| findstr .') do (set LOCALDATETIME=%%A)
set TIMESTAMP=%LOCALDATETIME:~0,14%
set PACKAGE_LOG=%cd%\build_%TIMESTAMP%.5.package.log
cd agent
call mvn install --log-file "%PACKAGE_LOG%"
cd ..
if %ERRORLEVEL% == 0  (
	echo Agent library build complete. You can now run an example with the agent using --runAgentExample or --runAgentConverterExample
	echo See agent/README.md for more information
) else {
	exit /B 1
}
exit /B 0

:runAgentExample
echo %time% run Agent 'InstrumentMe' example
set PATH_TO_AGENT_TARGET_DIR=%cd%\agent\target
set PATH_TO_AGENT_JAR="%PATH_TO_AGENT_TARGET_DIR%"\org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar
call java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -XX:+FlightRecorder -javaagent:"%PATH_TO_AGENT_JAR%"="%PATH_TO_AGENT_TARGET_DIR%"/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp "%PATH_TO_AGENT_JAR%"="%PATH_TO_AGENT_TARGET_DIR%"/test-classes/ org.openjdk.jmc.agent.test.InstrumentMe
exit /B 0

:runAgentConverterExample
echo %time% run Agent 'InstrumentMeConverter' example
set PATH_TO_AGENT_TARGET_DIR=%cd%\agent\target
set PATH_TO_AGENT_JAR="%PATH_TO_AGENT_TARGET_DIR%"\org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar
call java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -XX:+FlightRecorder -javaagent:"%PATH_TO_AGENT_JAR%"="%PATH_TO_AGENT_TARGET_DIR%"/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp "%PATH_TO_AGENT_JAR%"="%PATH_TO_AGENT_TARGET_DIR%"/test-classes/ org.openjdk.jmc.agent.converters.test.InstrumentMeConverter
exit /B 0

:clean
echo %time% running clean up
call mvn clean
cd core
call mvn clean
cd ..
cd agent
call mvn clean
cd ..
cd releng\third-party
call mvn clean
cd ..\..
goto end

:run
set JMC_EXE=%cd%\target\products\org.openjdk.jmc\win32\win32\x86_64\JDK Mission Control\jmc.exe
if exist "%JMC_EXE%" (
	start /B cmd /c "%JMC_EXE%"
) else (
	echo JMC not found in \"%JMC_EXE%\". Did you call --packageJmc before?
) 
goto end

:end