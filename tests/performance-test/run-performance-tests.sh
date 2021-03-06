#!/bin/bash
###
# #%L
# %%
# Copyright (C) 2016 BMW Car IT GmbH
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###

### PREREQUISITE ###
# The JavaScript test assumes that 'npm install' was executed within the
# /test/performance directory
####################

# Shell script parameters
JETTY_PATH=""
JOYNR_BIN_DIR=""
PERFORMANCETESTS_BIN_DIR=""
PERFORMANCETESTS_SOURCE_DIR=""
PERFORMANCETESTS_RESULTS_DIR=""
TESTCASE=""
USE_MAVEN=ON # Indicates whether java applications shall be started with maven or as standalone apps
MOSQUITTO_CONF=""
USE_NPM=ON # Indicates whether npm will be used to launch javascript applications.

### Constants ###
DOMAINNAME="performance_test_domain"

# If a test case uses a java consumer, some warmup runs are required in order
# to force the java runtime to perform all JIT optimizations
JAVA_WARMUPS=50

# For test cases with a single consumer, this constant stores the number of messages which
# will be transmitted during the test
SINGLECONSUMER_RUNS=5000

# For test cases with several consumers, this constant stores how many consumer instances will
# be created
MULTICONSUMER_NUMINSTANCES=5

# For test cases with several consumers, this constant stores how many messages a single
# consumer transmits
MULTICONSUMER_RUNS=200

# If a test case has to transmit a string, the length will be determined by this constant
INPUTDATA_STRINGLENGTH=100

# If a test case has to transmit a byte array, the length will be determined by this constant
INPUTDATA_BYTEARRAYSIZE=100

MQTT_BROKER_URI="tcp://localhost:1883"

# Process IDs for processes which must be terminated later
JETTY_PID=""
MOSQUITTO_PID=""
CLUSTER_CONTROLLER_PID=""
PROVIDER_PID=""

# arguments which are passed to the C++ cluster-controller
ADDITIONAL_CC_ARGS=""

SKIPBYTEARRAYSIZETIMESK=false

function waitUntilJettyStarted {
    started=0
    count=0
    while [ "$started" != "200" -a "$count" -lt "30" ]
    do
            sleep 2
            started=`curl -o /dev/null --silent --head --write-out '%{http_code}\n' \
            http://localhost:8080/bounceproxy/time/`
            let count+=1
    done
    if [ "$started" != "200" ]
    then
            # startup failed
            echo "ERROR: Failed to start jetty"
            exit
    fi
    echo "Jetty started."
    sleep 5
}

function startJetty {
    echo '### Starting jetty ###'

    JETTY_STDOUT=$PERFORMANCETESTS_RESULTS_DIR/jetty_stdout.txt
    JETTY_STDERR=$PERFORMANCETESTS_RESULTS_DIR/jetty_stderr.txt

    cd $JETTY_PATH

    if [ "$USE_MAVEN" != "ON" ]
    then
        java -jar start.jar 1>$JETTY_STDOUT 2>$JETTY_STDERR & JETTY_PID=$!
    else
        mvn jetty:run-war --quiet 1>$JETTY_STDOUT 2>$JETTY_STDERR & JETTY_PID=$!
    fi

    waitUntilJettyStarted
}

function startMosquitto {
    echo '### Starting mosquitto ###'

    MOSQUITTO_STDOUT=$PERFORMANCETESTS_RESULTS_DIR/mosquitto_stdout.txt
    MOSQUITTO_STDERR=$PERFORMANCETESTS_RESULTS_DIR/mosquitto_stderr.txt

    if [ "$MOSQUITTO_CONF" != "" ] && [ -f $MOSQUITTO_CONF ]
    then
        mosquitto -c $MOSQUITTO_CONF 1>$MOSQUITTO_STDOUT 2>$MOSQUITTO_STDERR & MOSQUITTO_PID=$!
    else
        echo "WARNING: No mosquitto.conf provided"
        mosquitto 1>$MOSQUITTO_STDOUT 2>$MOSQUITTO_STDERR & MOSQUITTO_PID=$!
    fi

    sleep 2

    echo 'Mosquitto started'
}

function startCppClusterController {
    echo '### Starting cluster controller ###'

    CC_STDOUT=$PERFORMANCETESTS_RESULTS_DIR/cc_stdout.txt
    CC_STDERR=$PERFORMANCETESTS_RESULTS_DIR/cc_stderr.txt

    cd $JOYNR_BIN_DIR

    # ensure previously created persistence files are gone
    rm -Rf *.persist joynr.settings

    ./cluster-controller $ADDITIONAL_CC_ARGS 1>$CC_STDOUT 2>$CC_STDERR & CLUSTER_CONTROLLER_PID=$!

    # Wait long enough in order to allow the cluster controller finish its start procedure
    sleep 5

    echo "Cluster controller started"
}

function startCppPerformanceTestProvider {
    echo '### Starting c++ performance test provider ###'

    PROVIDER_STDOUT=$PERFORMANCETESTS_RESULTS_DIR/provider_stdout.txt
    PROVIDER_STDERR=$PERFORMANCETESTS_RESULTS_DIR/provider_stderr.txt

    cd $PERFORMANCETESTS_BIN_DIR
    ./performance-provider-app --globalscope on --domain $DOMAINNAME 1>$PROVIDER_STDOUT 2>$PROVIDER_STDERR & PROVIDER_PID=$!

    # Wait long enough in order to allow the provider to finish the registration procedure
    sleep 5

    echo "C++ performance test provider started"
}

function startJavaPerformanceTestProvider {
    echo '### Starting java performance test provider (in process cc) ###'

    PROVIDER_STDOUT=$PERFORMANCETESTS_RESULTS_DIR/provider_stdout.txt
    PROVIDER_STDERR=$PERFORMANCETESTS_RESULTS_DIR/provider_stderr.txt

    PROVIDERCLASS="io.joynr.performance.EchoProviderApplication"
    PROVIDERARGS="-d $DOMAINNAME -s GLOBAL -r IN_PROCESS_CC  -b MQTT -mbu $MQTT_BROKER_URI"

    cd $PERFORMANCETESTS_SOURCE_DIR

    if [ "$USE_MAVEN" != "ON" ]
    then
        java -jar performance-test-provider.jar $PROVIDERARGS 1>$PROVIDER_STDOUT 2>$PROVIDER_STDERR & PROVIDER_PID=$!
    else
        mvn exec:java -o -Dexec.mainClass="$PROVIDERCLASS" -Dexec.args="$PROVIDERARGS" \
            1>$PROVIDER_STDOUT 2>$PROVIDER_STDERR & PROVIDER_PID=$!
    fi

    sleep 5

    echo "Performance test provider started"
}

function performJavaConsumerTest {
    MODE_PARAM=$1
    TESTCASE_PARAM=$2
    STDOUT_PARAM=$3
    REPORTFILE_PARAM=$4
    NUM_INSTANCES=$5
    NUM_RUNS=$6

    CONSUMERCLASS="io.joynr.performance.ConsumerApplication"
    CONSUMERARGS="-d $DOMAINNAME -w $JAVA_WARMUPS -r $NUM_RUNS \
                  -s $MODE_PARAM -t $TESTCASE_PARAM -bs $INPUTDATA_BYTEARRAYSIZE \
                  -sl $INPUTDATA_STRINGLENGTH"

    cd $PERFORMANCETESTS_SOURCE_DIR

    TEST_PIDS=()
    for (( i=0; i < $NUM_INSTANCES; ++i ))
    do
        echo "Launching consumer $i ..."

        if [ "$USE_MAVEN" != "ON" ]
        then
            java -jar performance-test-consumer.jar $CONSUMERARGS 1>>$STDOUT_PARAM 2>>$REPORTFILE_PARAM & CUR_PID=$!
        else
           mvn exec:java -o -Dexec.mainClass="$CONSUMERCLASS" \
           -Dexec.args="$CONSUMERARGS" 1>>$STDOUT_PARAM 2>>$REPORTFILE_PARAM & CUR_PID=$!
        fi

        TEST_PIDS+=$CUR_PID
        TEST_PIDS+=" "
    done

    echo "Waiting until consumers finished ..."
    wait $TEST_PIDS
}

function performCppSerializerTest {
    STDOUT_PARAM=$1
    REPORTFILE_PARAM=$2

    cd $PERFORMANCETESTS_BIN_DIR

    ./performance-serializer 1>>$STDOUT_PARAM 2>>$REPORTFILE_PARAM
}

function performCppConsumerTest {
    MODE_PARAM=$1
    TESTCASE_PARAM=$2
    STDOUT_PARAM=$3
    REPORTFILE_PARAM=$4
    NUM_INSTANCES=$5
    NUM_RUNS=$6

    CONSUMERARGS="-r $NUM_RUNS -t $TESTCASE_PARAM"

    cd $PERFORMANCETESTS_BIN_DIR
    if [ "$MODE_PARAM" == "SHORTCIRCUIT" ]
    then
        PERFORMCPPBINARY="performance-short-circuit"
    else
        CONSUMERARGS+=" -d $DOMAINNAME -s $MODE_PARAM -l $INPUTDATA_STRINGLENGTH \
                       -b $INPUTDATA_BYTEARRAYSIZE"
        PERFORMCPPBINARY="performance-consumer-app"
    fi

    TEST_PIDS=()
    for (( i=0; i < $NUM_INSTANCES; ++i ))
    do
        echo "Launching consumer $i ..."
        ./$PERFORMCPPBINARY $CONSUMERARGS 1>>$STDOUT_PARAM 2>>$REPORTFILE_PARAM & CUR_PID=$!
        TEST_PIDS+=$CUR_PID
        TEST_PIDS+=" "
    done

    echo "Waiting until consumers finished ..."
    wait $TEST_PIDS
}

function performJsConsumerTest {
    STDOUT_PARAM=$1
    REPORTFILE_PARAM=$2
    VIACC=$3
    STARTPROVIDER=$4
    SKIPBYTEARRAYSIZETIMEK=$5
    PROVIDER_STDOUT=$PERFORMANCETESTS_RESULTS_DIR/provider_stdout.txt
    PROVIDER_STDERR=$PERFORMANCETESTS_RESULTS_DIR/provider_stderr.txt

    cd $PERFORMANCETESTS_SOURCE_DIR

    if [ "$STARTPROVIDER" == "ON" ]
    then
        if [ "$USE_NPM" == "ON" ]
        then
            npm run-script --performance-test:domain=$DOMAINNAME \
                             startprovider 1>>$PROVIDER_STDOUT 2>>$PROVIDER_STDERR & PROVIDER_PID=$!
        else
            # This call assumes that the required js dependencies are installed locally
            node src/main/js/provider.js $DOMAINNAME 1>>$PROVIDER_STDOUT 2>>$REPORTFILE_PARAM & PROVIDER_PID=$!
        fi
    fi

    if [ "$USE_NPM" == "ON" ]
    then
        npm run-script --performance-test:runs=$SINGLECONSUMER_RUNS \
                       --performance-test:domain=$DOMAINNAME \
                       --performance-test:stringlength=$INPUTDATA_STRINGLENGTH \
                       --performance-test:bytearraylength=$INPUTDATA_BYTEARRAYSIZE \
                       --performance-test:viacc=$VIACC \
                       --performance-test:skipByteArraySizeTimesK=$SKIPBYTEARRAYSIZETIMEK \
                         startconsumer 1>>$STDOUT_PARAM 2>>$REPORTFILE_PARAM
    else
        export runs=$SINGLECONSUMER_RUNS
        export domain=$DOMAINNAME
        export stringlength=$INPUTDATA_STRINGLENGTH
        export bytearraylength=$INPUTDATA_BYTEARRAYSIZE
        export viacc=$VIACC
        # This call assumes that the required js dependencies are installed locally
        node src/main/js/consumer.js 1>>$STDOUT_PARAM 2>>$REPORTFILE_PARAM
    fi
}

function stopJetty {
    echo "Stopping jetty"

    if [ "$USE_MAVEN" != "ON" ]
    then
        kill $JETTY_PID
    else
        cd $JETTY_PATH
        mvn jetty:stop --quiet
    fi

    wait $JETTY_PID
    JETTY_PID=""
}

function stopMosquitto {
    echo "Stopping mosquitto"
    kill $MOSQUITTO_PID
    wait $MOSQUITTO_PID
    MOSQUITTO_PID=""
}

function stopCppClusterController {
    echo "Killing C++ CC"
    kill $CLUSTER_CONTROLLER_PID
    wait $CLUSTER_CONTROLLER_PID
    CLUSTER_CONTROLLER_PID=""
}

function stopAnyProvider {
    echo "Killing provider"
    if [ "$PROVIDER_PID" != "" ]
    then
        echo "USE_MAVEN: $USE_MAVEN"
        if [ "$USE_MAVEN" != "ON" ]
        then
            echo "do not call pkill for provider id $PROVIDER_ID"
        else
            # pkill is required if maven is used to start a provider. Maven launches the
            # provider as a child process, which seems not to be killed automatically along
            # with the parent process
            pkill -P $PROVIDER_PID
        fi
        kill $PROVIDER_PID
        wait $PROVIDER_PID
        PROVIDER_PID=""
    fi
}

function echoUsage {
    echo "Usage: run-performance-tests.sh -j <jetty-dir> -p <performance-bin-dir> \
-r <performance-results-dir> -s <performance-source-dir> \
-t <JAVA_SYNC|JAVA_ASYNC|JAVA_MULTICONSUMER|JS_ASYNC|OAP_TO_BACKEND_MOSQ|\
CPP_SYNC|CPP_ASYNC|CPP_MULTICONSUMER|ALL> -y <joynr-bin-dir>\
[-c <number-of-consumers> -x <number-of-runs> -m <use maven ON|OFF> -z <mosquitto.conf> -n <use node ON|OFF>]"
}

function checkDirExists {
    if [ -z "$1" ] || [ ! -d "$1" ]
    then
        echo "Directory \"$1\" does not exist"
        echoUsage
        exit 1
    fi
}

while getopts "c:d:j:k:p:r:s:t:x:y:m:z:n:a:" OPTIONS;
do
    case $OPTIONS in
        a)
            ADDITIONAL_CC_ARGS=$OPTARG
            ;;
        c)
            MULTICONSUMER_NUMINSTANCES=$OPTARG
            ;;
        d)
            DOMAINNAME=${OPTARG%/}
            ;;
        j)
            JETTY_PATH=${OPTARG%/}
            ;;
        k)
            SKIPBYTEARRAYSIZETIMESK=$OPTARG
            ;;
        p)
            PERFORMANCETESTS_BIN_DIR=${OPTARG%/}
            ;;
        r)
            PERFORMANCETESTS_RESULTS_DIR=${OPTARG%/}
            ;;
        s)
            PERFORMANCETESTS_SOURCE_DIR=${OPTARG%/}
            ;;
        t)
            TESTCASE=$OPTARG
            ;;
        x)
            SINGLECONSUMER_RUNS=$OPTARG
            MULTICONSUMER_RUNS=$OPTARG
            ;;
        y)
            JOYNR_BIN_DIR=${OPTARG%/}
            ;;
        m)
            USE_MAVEN=$OPTARG
            ;;
        z)
            MOSQUITTO_CONF=$OPTARG
            ;;
        n)
            USE_NPM=$OPTARG
            ;;
        \?)
            echoUsage
            exit 1
            ;;
    esac
done

if [ "$TESTCASE" != "JAVA_SYNC" ] && [ "$TESTCASE" != "JAVA_ASYNC" ] && \
   [ "$TESTCASE" != "JAVA_MULTICONSUMER" ] && \
   [ "$TESTCASE" != "JS_ASYNC" ] && [ "$TESTCASE" != "JS_SHORTCIRCUIT" ] && \
   [ "$TESTCASE" != "JS_CONSUMER" ] && [ "$TESTCASE" != "OAP_TO_BACKEND_MOSQ" ] && \
   [ "$TESTCASE" != "CPP_SYNC" ] && [ "$TESTCASE" != "CPP_ASYNC" ] && \
   [ "$TESTCASE" != "CPP_MULTICONSUMER" ] && [ "$TESTCASE" != "CPP_SERIALIZER" ] && \
   [ "$TESTCASE" != "CPP_SHORTCIRCUIT" ] && [ "$TESTCASE" != "CPP_PROVIDER" ] 
then
    echo "\"$TESTCASE\" is not a valid testcase"
    echo "-t option can be either JAVA_SYNC, JAVA_ASYNC, JAVA_MULTICONSUMER, JS_ASYNC, \
JS_CONSUMER, JS_SHORTCIRCUIT, OAP_TO_BACKEND_MOSQ, CPP_SYNC, CPP_ASYNC, CPP_MULTICONSUMER, \
CPP_SERIALIZER, CPP_SHORTCIRCUIT, CPP_PROVIDER"
    echoUsage
    exit 1
fi

checkDirExists $JOYNR_BIN_DIR
checkDirExists $PERFORMANCETESTS_BIN_DIR
checkDirExists $PERFORMANCETESTS_RESULTS_DIR
checkDirExists $PERFORMANCETESTS_SOURCE_DIR

REPORTFILE=$PERFORMANCETESTS_RESULTS_DIR/performancetest-result.txt
STDOUT=$PERFORMANCETESTS_RESULTS_DIR/consumer-stdout.txt

rm -f $STDOUT
rm -f $REPORTFILE

TESTCASES=('SEND_STRING' 'SEND_STRUCT' 'SEND_BYTEARRAY')

if [ ! $SKIPBYTEARRAYSIZETIMESK ]
then
    TESTCASES+=('SEND_BYTEARRAY_WITH_SIZE_TIMES_K')
fi

if [ "$TESTCASE" != "OAP_TO_BACKEND_MOSQ" ]
then
    startCppClusterController

    echo "### Starting performance tests ###"

    for mode in 'ASYNC' 'SYNC'; do
        if [ "$TESTCASE" == "JAVA_$mode" ]
        then
            startCppPerformanceTestProvider
            for testcase in 'SEND_STRING' 'SEND_STRUCT' 'SEND_BYTEARRAY'; do
                echo "Testcase: JAVA $testcase" | tee -a $REPORTFILE
                performJavaConsumerTest $mode $testcase $STDOUT $REPORTFILE 1 $SINGLECONSUMER_RUNS
            done
        fi
    done

    if [ "$TESTCASE" == "JAVA_MULTICONSUMER" ]
    then
        startCppPerformanceTestProvider
        for testcase in 'SEND_STRING' 'SEND_STRUCT' 'SEND_BYTEARRAY'; do
            echo "Testcase: JAVA $testcase / MULTIPLE CONSUMERS" | tee -a $REPORTFILE
            performJavaConsumerTest "ASYNC" $testcase $STDOUT $REPORTFILE $MULTICONSUMER_NUMINSTANCES $MULTICONSUMER_RUNS
        done
    fi

    for mode in 'ASYNC' 'SYNC' 'SHORTCIRCUIT'; do
        if [ "$TESTCASE" == "CPP_$mode" ]
        then
            startCppPerformanceTestProvider
            for testcase in ${TESTCASES[@]}; do
                echo "Testcase: $TESTCASE::$testcase" | tee -a $REPORTFILE
                performCppConsumerTest $mode $testcase $STDOUT $REPORTFILE 1 $SINGLECONSUMER_RUNS
            done
        fi
    done

    if [ "$TESTCASE" == "CPP_SERIALIZER" ]
    then
        echo "Testcase: CPP_SERIALIZER" | tee -a $REPORTFILE
        performCppSerializerTest $STDOUT $REPORTFILE
    fi

    if [ "$TESTCASE" == "CPP_MULTICONSUMER" ]
    then
        startCppPerformanceTestProvider
        for testcase in 'SEND_STRING' 'SEND_STRUCT' 'SEND_BYTEARRAY'; do
            echo "Testcase: CPP $testcase / MULTIPLE CONSUMERS" | tee -a $REPORTFILE
            performCppConsumerTest "ASYNC" $testcase $STDOUT $REPORTFILE $MULTICONSUMER_NUMINSTANCES $MULTICONSUMER_RUNS
        done
    fi

    if [ "$TESTCASE" == "JS_ASYNC" ]
    then
        echo "Testcase: JS_ASYNC" | tee -a $REPORTFILE
        performJsConsumerTest $STDOUT $REPORTFILE true ON $SKIPBYTEARRAYSIZETIMESK
    fi

    if [ "$TESTCASE" == "JS_SHORTCIRCUIT" ]
    then
        echo "Testcase: JS_SHORTCIRCUIT" | tee -a $REPORTFILE
        performJsConsumerTest $STDOUT $REPORTFILE false OFF $SKIPBYTEARRAYSIZETIMESK
    fi

    if [ "$TESTCASE" == "JS_CONSUMER" ]
    then
        echo "Testcase: JS_CONSUMER for domain $DOMAINNAME" | tee -a $REPORTFILE
        performJsConsumerTest $STDOUT $REPORTFILE true OFF $SKIPBYTEARRAYSIZETIMESK
    fi

    if [ "$TESTCASE" == "CPP_PROVIDER" ]
    then
        echo "Testcase: CPP_PROVIDER for domain $DOMAINNAME" | tee -a $REPORTFILE
        startCppPerformanceTestProvider $STDOUT $REPORTFILE true OFF
        # this testcase is used to start a provider which is then accessed from an external consumer
        # in order to keep the provider running, we sleep for a long time here
        sleep 100000000
    fi

    stopAnyProvider
    stopCppClusterController
fi

if [ "$TESTCASE" == "OAP_TO_BACKEND_MOSQ" ]
then
    checkDirExists $JETTY_PATH
    startJetty
    startMosquitto
    startCppClusterController
    startJavaPerformanceTestProvider

    echo "### Starting performance tests ###"

    echo "Testcase: OAP_TO_BACKEND_MOSQ" | tee -a $REPORTFILE
    performJsConsumerTest $STDOUT $REPORTFILE true OFF $SKIPBYTEARRAYSIZETIMESK

    stopAnyProvider
    stopCppClusterController
    stopMosquitto
    stopJetty
fi
