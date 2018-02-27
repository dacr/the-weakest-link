package dummy

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling.Marshal

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
  case class Chain(messages:List[String],depth:Int=2)

  def remoteCall(chain:Chain, uri:String):Future[HttpResponse] = {
    Marshal(chain).to[RequestEntity].flatMap{ entity =>
      Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.POST, entity = entity))
    }
  }

  def main(args:Array[String]) {
    val checkRoute = pathSingleSlash {get { complete { Check("OK")}}}
    val chainRoute =
      path("chain") {
        post {
          entity(as[Chain]) { chain =>
            val depth=chain.depth
            Kamon.currentSpan().tag("current-depth", depth.toString)
            val newMessage = s"loopback $depth"
            val newChain=chain.copy(newMessage::chain.messages, depth-1)
            val targetURI= "http://localhost:8080/chain"
            if (depth>1) {
              complete { remoteCall(newChain, targetURI) }
            } else {
              complete { newChain }
            }
          }
        }
      }
    val bindingFuture = Http().bindAndHandle(checkRoute~chainRoute, "0.0.0.0", 8080)
  }
}
