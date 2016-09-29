#!/bin/bash
set -e

if [ "$1" = 'tests' ]; then
  export PYTHONPATH=$MIST_HOME/src/main/python:$SPARK_HOME/python/:`readlink -f $SPARK_HOME/python/lib/py4j*`:$PYTHONPATH
  $SPARK_HOME/sbin/start-master.sh
  $SPARK_HOME/sbin/start-slave.sh localhost:7077
  cd $MIST_HOME
  ./sbt/sbt -DsparkVersion=$SPARK_VERSION -Dconfig.file=configs/docker.conf "project examples" package "project mist" test
elif [ "$1" = 'mist' ]; then
  export PYTHONPATH=$MIST_HOME/src/main/python:$SPARK_HOME/python/:`readlink -f $SPARK_HOME/python/lib/py4j*`:$PYTHONPATH
  cd $MIST_HOME
  ./bin/start master --config configs/docker.conf --jar target/scala-*/mist-assembly-*.jar
else
  exec "$@"
fi
