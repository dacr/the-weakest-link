package dummy

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import better.files._
import kamon.Kamon
import kamon.jaeger.JaegerReporter
import kamon.prometheus.PrometheusReporter

import scala.concurrent.Future
import de.heikoseeberger.akkahttpjson4s._
import Json4sSupport._
import com.typesafe.config.ConfigFactory
import kamon.metric.MeasurementUnit
import org.json4s.{DefaultFormats, jackson}


case class Check(status:String)
case class ChainUpstream(maxDepth:Int = 0, currentDepth:Int = 0, failDepth:Int = -1, slowDepth:Int = -1)
case class ChainDownstream(story:String)


object Dummy {
  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  implicit val jsonSerialization = jackson.Serialization
  implicit val jsonFormats       = DefaultFormats

  Kamon.addReporter(new JaegerReporter())
  Kamon.addReporter(new PrometheusReporter())
  val remoteCallCounter = Kamon.counter("remoteCall")

  implicit val system = ActorSystem("mysystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  val config = ConfigFactory.load()

  val myNeighborIp = config.getString("weakest-link.neighbor")
  val myNeighBorTargetURI= s"http://$myNeighborIp:8080/chain"

  def myStoryPart:String = {
    val storyFile = file"myStoryPart.txt"
    if (!storyFile.exists) {
      val hostname=java.net.InetAddress.getLocalHost.getHostName
      storyFile.createIfNotExists().appendText(s"undefined-$hostname")
    }
    storyFile.lines.map(_.trim).mkString(" ")
  }

  def remoteCall(up:ChainUpstream, uri:String):Future[ChainDownstream] = {
    remoteCallCounter.increment()
    val futureResponse = Marshal(up).to[RequestEntity].flatMap{ entity =>
      Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.POST, entity = entity))
    }
    futureResponse.flatMap{ resp => Unmarshal(resp.entity).to[ChainDownstream] }
  }

  val checkRoute =
    pathSingleSlash {
      get {
        complete {
          Check("OK")
        }
      }
    }

  val askRoute =
    path("ask") {
      get {
        complete {
          ChainDownstream(myStoryPart)
        }
      }
    }

  val askNextRoute =
    path("asknext") {
      get {
        complete {
          remoteCall(ChainUpstream(), myNeighBorTargetURI)
        }
      }
    }

  val chainRoute =
    path("chain") {
      post {
        entity(as[ChainUpstream]) { up =>
          import up.{currentDepth=>depth, failDepth, slowDepth, maxDepth}
          Kamon.currentSpan().tag("depth", depth.toString)
          Kamon.currentSpan().tag("storyPart", myStoryPart)
          if (failDepth>=0 && depth==failDepth) throw new RuntimeException("ISSUE")
          if (slowDepth>=0 && depth==slowDepth) Thread.sleep(2*1000L)
          if (depth < maxDepth) {
            complete {
              val nextUp=up.copy(currentDepth = depth + 1)
              val futureResponse = remoteCall(nextUp, myNeighBorTargetURI)
              futureResponse.map(response =>
                response.copy(story = response.story.trim + " " + myStoryPart)
              )
            }
          } else {
            complete { ChainDownstream(myStoryPart) }
          }
        }
      }
    }

  def main(args:Array[String]) {
    logger.info("Application starting")
    logger.info(s"My story part is '$myStoryPart'")
    logger.info(s"My nearest neighbor is '$myNeighborIp'")
    val routes = checkRoute~chainRoute~askRoute~askNextRoute
    val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", 8080)
    bindingFuture.map( _=> logger.info("Application started"))
  }
}
