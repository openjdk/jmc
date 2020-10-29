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
echo 	--packageJmc	to package JMC
echo 	--clean	to run maven clean
echo 	--run	to run JMC once it was packaged
echo 	--help	to show this help dialog
exit /B 0

:parse_args
if "%1" == "--help" goto print_usage
if "%1" == "--test" goto test
if "%1" == "--testUi" goto testUi
if "%1" == "--packageJmc" goto packageJmc
if "%1" == "--clean" goto clean
if "%1" == "--run" goto run
echo unknown argument %1
goto print_usage

:startJetty
@REM based on US format of %date%: Fri 10/30/2020
SET DATE_STR=%DATE:~10,4%%DATE:~4,2%%DATE:~7,2%
SET TIME_STR=%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%
SET P2_SITE_LOG=%cd%\build_%DATE_STR%_%TIME_STR%.1.p2_site.log
SET JETTY_LOG=%cd%\build_%DATE_STR%_%TIME_STR%.2.jetty.log
SET INSTALL_LOG=%cd%\build_%DATE_STR%_%TIME_STR%.3.install.log
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
for /F "tokens=2 delims=," %%R IN ('tasklist /FI "Windowtitle eq %1" /NH /FO csv') do (
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
SET JETTY_TITLE=jmc-jetty-%time%
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
SET JETTY_TITLE=jmc-jetty-%time%
call :startJetty %JETTY_TITLE%
if not %ERRORLEVEL% == 0 (
	exit /B 1
)
@REM based on US format of %date%: Fri 10/30/2020
SET DATE_STR=%DATE:~10,4%%DATE:~4,2%%DATE:~7,2%
SET TIME_STR=%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%
SET PACKAGE_LOG=%cd%\build_%DATE_STR%_%TIME_STR%.4.package.log
echo %time% packaging jmc - logging output to %PACKAGE_LOG%
call mvn package --log-file "%PACKAGE_LOG%"
echo You can now run jmc by calling "%0 --run" or "%cd%\target\products\org.openjdk.jmc\win32\win32\x86_64\JDK Mission Control\jmc.exe"
call :killJetty %JETTY_TITLE%
goto end

:clean
echo %time% running clean up
call mvn clean
cd core
call mvn clean
cd ..
cd releng\third-party
call mvn clean
cd ..\..
goto end

:run
SET JMC_EXE=%cd%\target\products\org.openjdk.jmc\win32\win32\x86_64\JDK Mission Control\jmc.exe
if exist "%JMC_EXE%" (
	start /B cmd /c "%JMC_EXE%"
) else (
	echo JMC not found in \"%JMC_EXE%\". Did you call --packageJmc before?
) 
goto end

:end