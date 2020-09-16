#!/bin/bash
set -e # immediately exit if any command has a non-zero exit status
set -u # a reference to any variable you have not previously defined causes the program to immediately exit
set -o pipefail # If any command in a pipeline fails, that return code will be used as the return code of the whole pipeline

PROGNAME=$(basename "$0")

JETTY_PID=""

function err_report() {
    err_log "$(date +%T) ${PROGNAME}: Error on line $1"
    err_log "$(date +%T) current working directory: $PWD"
}

function exitTrap() {
    if [ -n "${JETTY_PID}" ]; then
        echo "$(date +%T) terminating jetty server"
        pkill -P "${JETTY_PID}"
    fi
}

function err_log() {
    echo "$@" >&2
}

trap 'err_report $LINENO' ERR
trap 'exitTrap' EXIT

function printHelp() {
    echo "usage: call ./$(basename "$0") with the following options:"
    {
        printf " \t%s\t%s\n" "--runTests" "to run the tests"
        printf " \t%s\t%s\n" "--runUiTests" "to run the tests including UI tests"
        printf " \t%s\t%s\n" "--packageJmc" "to package JMC"
        printf " \t%s\t%s\n" "--clean" "to run maven clean"
        printf " \t%s\t%s\n" "--run" "to run JMC once it was packaged"
        printf " \t%s\t%s\n" "--help" "to show this help dialog"
    } | column -ts $'\t'
}

if [ $# -eq 0 ]; then
    printHelp
    exit 0
fi

function runTests() {
    mvn verify
}

function runUiTests() {
    mvn verify -P uitests
}

function packageJmc() {
    local timestamp
    timestamp="$(date +%Y%m%d%H%M%S)"
    local BASEDIR
    BASEDIR=$(mvn help:evaluate -Dexpression=project.build.directory --non-recursive -q -DforceStdout)

    mkdir -p "${BASEDIR}" # just in case clean was called before

    local p2SiteLog="${BASEDIR}/build_${timestamp}.1.p2_site.log"
    local jettyLog="${BASEDIR}/build_${timestamp}.2.jetty.log"
    local installLog="${BASEDIR}/build_${timestamp}.3.install.log"
    local packageLog="${BASEDIR}/build_${timestamp}.4.package.log"

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

    while [ "$(tail -n 1 "${jettyLog}")" != "[INFO] Started Jetty Server" ]; do
        echo "$(date +%T) waiting for jetty server to start"
        sleep 1
    done
    echo "$(date +%T) jetty server up and running"

    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }
    pushd core 1> /dev/null || {
        err_log "directory core not found"
        exit 1
    }

    echo "$(date +%T) installing core artfacts - logging output to ${installLog}"
    mvn clean install --log-file "${installLog}"

    popd 1> /dev/null || {
        err_log "could not go to project root directory"
        exit 1
    }
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

function clean() {
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
    local BASEDIR
    BASEDIR="$(mvn help:evaluate -Dexpression=project.build.directory --non-recursive -q -DforceStdout)"

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

function parseArgs() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --help)
                printHelp
                exit 0
                ;;
            --test)
                runTests
                ;;
            --testUi)
                runUiTests
                ;;
            --packageJmc)
                packageJmc
                ;;
            --clean)
                clean
                ;;
            --run)
                run
                ;;
            *)
                err_log "unknown argument \"$1\""
                printHelp
                exit 1
                ;;
        esac
        shift
    done
}

function checkPreconditions() {
    if ! command -v   mvn &> /dev/null ; then
        err_log "It seems you do not have maven installed. Please ensure you have it installed and executable as \"mvn\"."
        exit 1
    fi

    if ! command -v   java &> /dev/null ; then
        err_log "It seems you do not have java installed. Please ensure you have it installed and executable as \"java\"."
        exit 1
    fi
}

checkPreconditions
parseArgs "$@"
