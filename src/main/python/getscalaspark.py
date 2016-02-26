#export SPARK_HOME="/home/vagrant/spark-1.5.2-bin-hadoop2.6/"
#export PYTHONPATH=$SPARK_HOME/python/:$SPARK_HOME/python/lib/py4j-0.8.2.1-src.zip:$PYTHONPATH
#import sys
#sys.path.append('/home/vagrant/spark-1.5.2-bin-hadoop2.6')
#sys.path.append('/home/vagrant/spark-1.5.2-bin-hadoop2.6/python')
#sys.path.append('/home/vagrant/spark-1.5.2-bin-hadoop2.6/python/lib/py4j-0.8.2.1-src.zip')

#from py4j.java_gateway import JavaGateway
import py4j.java_gateway
import pyspark
import sys, getopt, traceback, json, re

from py4j.java_gateway import java_import, JavaGateway, GatewayClient
from py4j.java_collections import SetConverter, MapConverter, ListConverter
from py4j.protocol import Py4JJavaError

from pyspark.conf import SparkConf
from pyspark.context import SparkContext
from pyspark.rdd import RDD
from pyspark.files import SparkFiles
from pyspark.storagelevel import StorageLevel
from pyspark.accumulators import Accumulator, AccumulatorParam
from pyspark.broadcast import Broadcast
from pyspark.serializers import MarshalSerializer, PickleSerializer

# for back compatibility
from pyspark.sql import SQLContext, HiveContext, SchemaRDD, Row

###################################################################
def getSparkContext():
  try:
    client = GatewayClient(port=int(25333))
    gateway = JavaGateway(client, auto_convert = True)
    entry_point = gateway.entry_point

    java_import(gateway.jvm, "org.apache.spark.SparkContext")
    java_import(gateway.jvm, "org.apache.spark.SparkEnv")
    java_import(gateway.jvm, "org.apache.spark.SparkConf")
    java_import(gateway.jvm, "org.apache.spark.api.java.*")
    java_import(gateway.jvm, "org.apache.spark.api.python.*")
    java_import(gateway.jvm, "org.apache.spark.mllib.api.python.*")
    java_import(gateway.jvm, "org.apache.spark.*")
     

    ScalaSparkContextWrapper = entry_point.ScalaSparkContextWrapper()
    sconf = ScalaSparkContextWrapper.getSparkConf()
    conf = SparkConf(_jvm = gateway.jvm, _jconf = sconf)
    jsc = ScalaSparkContextWrapper.getSparkContext()
    sc = SparkContext(jsc=jsc, gateway=gateway, conf=conf) 
    return sc

  except Py4JJavaError:
    print("except Py4JJavaError")
    return None  
  
  except Exception:
    print("except")
    return None

def getNumbers():
  try:
    client = GatewayClient(port=int(25333))
    gateway = JavaGateway(client, auto_convert = True)
    entry_point = gateway.entry_point
    java_import(gateway.jvm,'java.util.*')
    SimpleDataWrapper = entry_point.SimpleDataWrapper()

    num = SimpleDataWrapper.get()
    l = list()
    count = 0
    size = num.size()
    while count < size:
      l.append(num.head())
      count = count + 1
      num = num.tail()
    return l

  except Py4JJavaError:
    print("except Py4JJavaError")
    return None 
  
  except Exception:
    print("except")
    return None

def sendResult(result):
  try:
    client = GatewayClient(port=int(25333))
    gateway = JavaGateway(client, auto_convert = True)
    entry_point = gateway.entry_point
    java_import(gateway.jvm,'java.util.*')
    SimpleDataWrapper = entry_point.SimpleDataWrapper()
    SimpleDataWrapper.set(result)

  except Py4JJavaError:
    print("except Py4JJavaError")

  except Exception:
    print("except")

