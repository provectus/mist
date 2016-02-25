package com.provectus.lymph.pythonexecuter

/**
  * Created by lblokhin on 24.02.16.
  */
import com.provectus.lymph.LymphJob
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.api.java.JavaSparkContext

import py4j.GatewayServer
import sys.process._

import scala.collection.JavaConversions._

object DataWrapper{
  var data: Any = _
  def set(in: Any) = { data = in }
  def set(in: java.util.ArrayList[Int]) = { data = asScalaBuffer(in).toList }
  def get(): Any = data
}

object SparkContextWrapper{
  var m_context : JavaSparkContext = _
  var m_conf : SparkConf = _

  def getSparkConf(): SparkConf = m_conf
  def setSparkConf(conf: SparkConf) = {m_conf = conf}

  def getSparkContext(): JavaSparkContext = m_context
  def setSparkContext(sc: SparkContext) = {m_context = new JavaSparkContext(sc)}
}

object SimplePython extends LymphJob {
  /** Contains implementation of spark job with ordinary [[org.apache.spark.SparkContext]]
    * Abstract method must be overridden
    *
    * @param context    spark context
    * @param parameters user parameters
    * @return result of the job
    */
  //var cmd = "python /vagrant/examples/src/main/python/example.py"
  var cmd = "python "

  def AddPyPath(pypath: String) = {cmd = "python " + pypath }

  def ScalaSparkContextWrapper() = SparkContextWrapper

  def SimpleDataWrapper = DataWrapper

  override def doStuff(context: SparkContext, parameters: Map[String, Any]): Map[String, Any] = {

    val numbers: List[Int] = parameters("digits").asInstanceOf[List[Int]]

    SimpleDataWrapper.set(numbers)
    ScalaSparkContextWrapper.setSparkConf(context.getConf)
    ScalaSparkContextWrapper.setSparkContext(context)

    val gatewayServer: GatewayServer = new GatewayServer(SimplePython)
    gatewayServer.start()
    val boundPort: Int = gatewayServer.getListeningPort

    if (boundPort == -1) {
      println("GatewayServer failed to bind; exiting")
      throw new Exception("GatewayServer to Python exception")
    } else {
      println(s"Started PythonGatewayServer on port $boundPort")
    }

    var starttime = context.startTime
    println(s"$starttime")

    val exitCode = cmd.!
    gatewayServer.shutdown()
    println("Exiting due to broken pipe from Python driver")

    println(exitCode)
    if( exitCode!=0 )
      throw new Exception("Python exception")

    Map("result" -> SimpleDataWrapper.get())
  }

}
