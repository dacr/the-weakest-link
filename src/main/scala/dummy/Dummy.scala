package dummy

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal

import kamon.Kamon
import kamon.jaeger.JaegerReporter
import kamon.prometheus.PrometheusReporter

import scala.concurrent.Future

import de.heikoseeberger.akkahttpjson4s._
import Json4sSupport._
import org.json4s.{ DefaultFormats, jackson }

object Dummy {
  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  implicit val jsonSerialization = jackson.Serialization
  implicit val jsonFormats       = DefaultFormats

  Kamon.addReporter(new JaegerReporter())
  Kamon.addReporter(new PrometheusReporter())

  implicit val system = ActorSystem("mysystem")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  case class Check(status:String)
  case class ChainUpstream(maxDepth:Int=2, currentDepth:Int=0)
  case class ChainDownstream(story:String)

  def remoteCall(up:ChainUpstream, uri:String):Future[HttpResponse] = {
    Marshal(up).to[RequestEntity].flatMap{ entity =>
      Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.POST, entity = entity))
    }
  }

  def myStoryPart = "... "             // TODO :

  val myNeighborIp = "127.0.0.1"       // TODO :

  val myNeighBorTargetURI= s"http://$myNeighborIp:8080/chain"

  val checkRoute = pathSingleSlash { get { complete { Check("OK")} } }

  val askRoute = path("ask") { get { complete { ChainDownstream(myStoryPart)} } }

  val chainRoute =
    path("chain") {
      post {
        entity(as[ChainUpstream]) { up =>
          val depth = up.currentDepth
          Kamon.currentSpan().tag("depth", depth.toString)
          Kamon.currentSpan().tag("storyPart", myStoryPart)
          if (depth<up.maxDepth) {
            complete {
              val nextUp=up.copy(currentDepth=depth+1)
              val resp = remoteCall(nextUp, myNeighBorTargetURI)
              Unmarshal(resp).to[ChainDownstream]
              resp
            }
          } else {
            complete { ChainDownstream(myStoryPart) }
          }
        }
      }
    }

  def main(args:Array[String]) {
    val routes = checkRoute~chainRoute~askRoute
    val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", 8080)
  }
}
