package com.provectus.lymph.actors

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{Props, ActorRef, Actor}
import akka.pattern.ask
import com.provectus.lymph.{Constants, LymphConfig}
import com.provectus.lymph.actors.tools.{JSONSchemas, JSONValidator}

import org.json4s.NoTypeHints
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._

import net.sigusr.mqtt.api._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

import com.provectus.lymph.jobs.{JobResult, JobConfiguration}

/** MQTT interface */
private[lymph] class MQTTService extends Actor {

  // Connect to MQTT with host/port from config
  context.actorOf(Manager.props(new InetSocketAddress(InetAddress.getByName(LymphConfig.MQTT.host), LymphConfig.MQTT.port))) ! Connect(Constants.Actors.mqttServiceName)

  override def receive: Receive = {
    // Connected to MQTT server
    case Connected =>
      println("Connected to mqtt")
      // Subscribing to MQTT topic
      sender() ! Subscribe(Vector((LymphConfig.MQTT.subscribeTopic, AtMostOnce)), 1)
      // We are ready to receive message from MQ server
      context become ready(sender())
    case ConnectionFailure(reason) => println(s"Connection to mqtt failed [$reason]")
  }

  // actor which is used for running jobs according to request
  lazy val jobRequestActor: ActorRef = context.actorOf(Props[JobRunner], name = Constants.Actors.asyncJobRunnerName)

  def ready(mqttManager: ActorRef): Receive = {

    // Subscribed to MQTT topic
    case Subscribed(vQoS, MessageId(1)) =>
      println("Successfully subscribed to topic foo")

    // Received a message
    case Message(topic, payload) =>
      val stringMessage = new String(payload.to[Array], "UTF-8")
      println(s"[$topic] $stringMessage")

      // we need to check if message is a request
      val isMessageValidJar = JSONValidator.validate(stringMessage, JSONSchemas.jobRequest)
      val isMessageValidPy = JSONValidator.validate(stringMessage, JSONSchemas.jobRequestPy)
      // if it a request
      if (isMessageValidJar || isMessageValidPy) {
        implicit val formats = Serialization.formats(NoTypeHints)
        val json = parse(stringMessage)
        // map request into JobConfiguration
        val jobCreatingRequest = json.extract[JobConfiguration]

        // Run job asynchronously
        val future = jobRequestActor.ask(jobCreatingRequest)(timeout = LymphConfig.Contexts.timeout(jobCreatingRequest.name))

        future
          .recover {
            case error: Throwable => Right(error.toString)
          }
          .onSuccess {
            case result: Either[Map[String, Any], String] =>
              val jobResult: JobResult = result match {
                case Left(jobResult: Map[String, Any]) =>
                  JobResult(success = true, payload = jobResult, request = jobCreatingRequest, errors = List.empty)
                case Right(error: String) =>
                  JobResult(success = false, payload = Map.empty[String, Any], request = jobCreatingRequest, errors = List(error))
              }

              val jsonString = write(jobResult)
              mqttManager ! Publish(LymphConfig.MQTT.publishTopic, jsonString.getBytes("UTF-8").to[Vector])
              println(s"${write(result)}")
          }
      }
    }

}
