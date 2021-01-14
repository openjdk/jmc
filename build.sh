#!/bin/bash
set -e # immediately exit if any command has a non-zero exit status
set -u # a reference to any variable you have not previously defined causes the program to immediately exit
set -o pipefail # If any command in a pipeline fails, that return code will be used as the return code of the whole pipeline

PROGNAME=$(basename "$0")

JETTY_PID=""
BASEDIR=""
JMC_DIR=""

function err_report() {
    err_log "$(date +%T) ${PROGNAME}: Error on line $1"
    err_log "$(date +%T) current working directory: $PWD"
}

function exitTrap() {
    if [ -n "${JETTY_PID}" ]; then
        echo "$(date +%T) terminating jetty server"
        kill "${JETTY_PID}"
    fi
}

function installCore() {
    local timestamp="$1"
    local installLog="${BASEDIR}/build_${timestamp}.3.install.log"
    pushd core 1> /dev/null || {
        err_log "directory core not found"
        exit 1
    }

    echo "$(date +%T) installing core artifacts - logging output to ${installLog}"
    mvn clean install --log-file "${installLog}"

    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }
}

function startJetty() {
    local timestamp=$1
    local p2SiteLog="${BASEDIR}/build_${timestamp}.1.p2_site.log"
    local jettyLog="${BASEDIR}/build_${timestamp}.2.jetty.log"
    

    pushd releng/third-party 1> /dev/null || {
        err_log "directory releng/third-party not found"
        exit 1
    }
    echo "$(date +%T) building p2:site - logging output to ${p2SiteLog}"
    mvn p2:site --log-file "${p2SiteLog}"

    echo "$(date +%T) run jetty - logging output to ${jettyLog}"
    touch "${jettyLog}" # create file so that it exists already for tail below
    mvn jetty:run --log-file "${jettyLog}" &
    JETTY_PID=$!

    while ! grep -q "^\[INFO\] Started Jetty Server$" "${jettyLog}"; do
        echo "$(date +%T) waiting for jetty server to start"
        sleep 1
    done
    echo "$(date +%T) jetty server up and running on pid ${JETTY_PID}"

    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }
}

function err_log() {
    echo "$@" >&2
}

trap 'err_report $LINENO' ERR
trap 'exitTrap' EXIT

function printHelp() {
    echo "usage: call ./$(basename "$0") with the following options:"
    {
        printf " \t%s\t%s\n" "--test" "to run the tests"
        printf " \t%s\t%s\n" "--testUi" "to run the tests including UI tests"
        printf " \t%s\t%s\n" "--installCore" "to install JMC core"
        printf " \t%s\t%s\n" "--packageJmc" "to package JMC"
        printf " \t%s\t%s\n" "--packageAgent" "to package Agent"
        printf " \t%s\t%s\n" "--clean" "to run maven clean"
        printf " \t%s\t%s\n" "--run" "to run JMC, once it is packaged"
        printf " \t%s\t%s\n" "--runAgentExample" "to run Agent 'InstrumentMe' example, once it is packaged"
        printf " \t%s\t%s\n" "--runAgentConverterExample" "to run Agent 'InstrumentMeConverter' example, once it is packaged"
        printf " \t%s\t%s\n" "--help" "to show this help dialog"
    } | column -ts $'\t'
}

if [ $# -eq 0 ]; then
    printHelp
    exit 0
fi

function runTests() {
    local timestamp=$1
    startJetty $timestamp
    echo "${timestamp} running tests"
    mvn verify
}

function runUiTests() {
    local timestamp=$1
    startJetty $timestamp
    installCore $timestamp
    echo "$(date +%T) running UI tests"
    mvn verify -P uitests
}

function packageJmc() {
    local timestamp=$1    
    startJetty $timestamp
    installCore $timestamp
    local packageLog="${BASEDIR}/build_${timestamp}.4.package.log"

    echo "$(date +%T) packaging jmc - logging output to ${packageLog}"
    mvn package --log-file "${packageLog}"

    if [[ "${OSTYPE}" =~ "linux"* ]]; then
        echo "You can now run jmc by calling \"${PROGNAME} --run\" or \"${BASEDIR}/products/org.openjdk.jmc/linux/gtk/x86_64/JDK\ Mission\ Control/jmc\""
    elif [[ "${OSTYPE}" =~ "darwin"* ]]; then
        echo "You can now run jmc by calling \"${PROGNAME} --run\" or \"${BASEDIR}/products/org.openjdk.jmc/macosx/cocoa/x86_64/JDK\ Mission\ Control.app/Contents/MacOS/jmc\""
    else
        err_log "unknown OS type: \"${OSTYPE}\". Please check your package in \"${BASEDIR}/products/org.openjdk.jmc/\""
    fi
}

function packageAgent() {
    local timestamp=$1
    local packageLog="${BASEDIR}/build_${timestamp}.5.package.log"

    pushd agent 1> /dev/null || {
        err_log "directory agent not found"
        exit 1
    }
    
    echo "$(date +%T) packaging jmc agent - logging output to ${packageLog}"
    mvn package --log-file "${packageLog}"

    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }

    if [[ "${OSTYPE}" =~ "linux"* ]] || [[ "${OSTYPE}" =~ "darwin"* ]]; then
       printf "%s\n" "Agent library build complete. You can now run an example with the agent using \"${PROGNAME} --runAgentExample or --runAgentConverterExample\""
       printf "%s\n" "See agent/README.md for more information"
    else
        err_log "unknown OS type: \"${OSTYPE}\". Please check package in \"${JMC_DIR}/agent/target\""
        exit 1
    fi
}

function clean() {
    echo "$(date +%T) running clean up"
    mvn clean

    pushd core 1> /dev/null || {
        err_log "directory core not found"
        exit 1
    }
    mvn clean
    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }

    pushd agent 1> /dev/null || {
        err_log "directory agent not found"
        exit 1
    }
    mvn clean
    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }

    pushd releng/third-party 1> /dev/null || {
        err_log "directory releng/third-party not found"
        exit 1
    }
    mvn clean
    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }
}

function run() {
    local path
    if [[ "${OSTYPE}" =~ "linux"* ]]; then
        path="${BASEDIR}/products/org.openjdk.jmc/linux/gtk/x86_64/JDK Mission Control/jmc"
    elif [[ "${OSTYPE}" =~ "darwin"* ]]; then
        path="${BASEDIR}/products/org.openjdk.jmc/macosx/cocoa/x86_64/JDK Mission Control.app/Contents/MacOS/jmc"
    else
        err_log "unknown OS type: ${OSTYPE}"
        exit 1
    fi

    if [ -f "${path}" ]; then
        exec "${path}"
    else
        err_log "JMC not found in \"${path}\". Did you call --packageJmc before?"
        exit 1
    fi
}

function runAgentByClass() {
    local agentExampleClass=$1
    if [[ -z "${agentExampleClass}" ]]; then
        err_log "error: try to run undefined agent class, empty class"
        printHelp
        exit 1
    else
        printf "example agent class: %s\n" "${agentExampleClass}"
    fi

    checkJava
    if [[ "${OSTYPE}" =~ "linux"* ]] || [[ "${OSTYPE}" =~ "darwin"* ]]; then
       printf "%s\n" "try to execute an agent example"
    else
        err_log "unknown OS type: \"${OSTYPE}\". Please check package ${JMC_DIR}/agent README.MD"
        exit 1
    fi


    local javaVersion=`java -version 2>&1 | head -1 | cut -d '"' -f 2 | sed 's/^1\.//' | cut -d '.' -f 1`

    if ! [ "$javaVersion" -eq "$javaVersion" ] 2> /dev/null; then
         printf "%s\n" "WARNING: java version not recognized"
         javaVersion=15
    fi
           
    printf "Java Version:%s\n" "${javaVersion}"
    local pathToAgentTargetDir="${JMC_DIR}/agent/target"
    local pathToAgentJar="${pathToAgentTargetDir}/org.openjdk.jmc.agent-1.0.0-SNAPSHOT.jar"
    printf "Agent path:%s\n" "${pathToAgentJar}"
    if [ -f "${pathToAgentJar}" ]; then        
        if [ "$javaVersion" -lt "8" ]; then
            printf "min. required java version is 8"
            exit 1
        elif [ "$javaVersion" -eq "8" ]; then
            java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -javaagent:${pathToAgentJar}=${pathToAgentTargetDir}/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp ${pathToAgentJar}:${pathToAgentTargetDir}/test-classes/ ${agentExampleClass}
        elif [ "$javaVersion" -lt "13" ]; then 
            java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -XX:+FlightRecorder -javaagent:${pathToAgentJar}=${pathToAgentTargetDir}/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp ${pathToAgentJar}:${pathToAgentTargetDir}/test-classes/ ${agentExampleClass}
        else 
            java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -javaagent:${pathToAgentJar}=${pathToAgentTargetDir}/test-classes/org/openjdk/jmc/agent/test/jfrprobes_template.xml -cp ${pathToAgentJar}:${pathToAgentTargetDir}/test-classes/ ${agentExampleClass}
        fi
    else
        err_log "Agent not found in \"${pathToAgentJar}\". Did you call --packageAgent before?"
        exit 1
    fi
}

function parseArgs() {
    local timestamp="$(date +%Y%m%d%H%M%S)"
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --help)
                printHelp
                exit 0
                ;;
            --test)
                runTests $timestamp
                ;;
            --testUi)
                runUiTests $timestamp
                ;;
            --installCore)
                installCore $timestamp
                ;;
            --packageJmc)
                packageJmc $timestamp
                ;;
            --packageAgent)
                packageAgent $timestamp
                ;;
            --clean)
                clean
                ;;
            --run)
                run
                ;;
            --runAgentExample)
                runAgentByClass "org.openjdk.jmc.agent.test.InstrumentMe"
                ;;
            --runAgentConverterExample)
                runAgentByClass "org.openjdk.jmc.agent.converters.test.InstrumentMeConverter"
                ;;
            *)
                err_log "unknown arguments: $@"
                printHelp
                exit 1
                ;;
        esac
        shift
    done
}

function checkJava() {
    if [[ -z "$JAVA_HOME" ]]; then 
        echo "JAVA_HOME is not defined"
    fi

    if ! command -v java &> /dev/null ; then
        err_log "It seems you do not have java installed. Please ensure you have it installed and executable as \"java\"."
        exit 1
    fi
}

function checkPreconditions() {
    checkJava

    if ! command -v mvn &> /dev/null ; then
        err_log "It seems you do not have maven installed. Please ensure you have it installed and executable as \"mvn\"."
        exit 1
    fi

    BASEDIR=$(mvn help:evaluate -Dexpression=project.build.directory --non-recursive -q -DforceStdout)
    JMC_DIR=$(pwd)
    mkdir -p "${BASEDIR}" # just in case clean was called before
}

checkPreconditions
parseArgs "$@"