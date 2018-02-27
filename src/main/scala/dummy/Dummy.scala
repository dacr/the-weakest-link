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

  val usageCounter = Kamon.counter("usage.counter")

  case class Chain(messages:List[String],depth:Int)

  def remoteCall(chain:Chain, uri:String):Future[HttpResponse] = {
    Marshal(chain).to[RequestEntity].flatMap{ entity =>
      Http().singleRequest(HttpRequest(uri = uri, method = HttpMethods.POST, entity = entity))
    }
  }

  def main(args:Array[String]) {
    val chainRoute =
      path("chain") {
        post {
          entity(as[Chain]) { chain =>
            usageCounter.increment
            val depth=chain.depth
            val span = Kamon.buildSpan("chaining").start()
            span.tag("current-depth", depth.toString)
            val newMessage = s"loopback $depth"
            val newChain=chain.copy(newMessage::chain.messages, depth-1)
            if (depth>0) {
              val inprogress = remoteCall(newChain, "http://localhost:8080/chain")
              inprogress.andThen { case _ =>
                span.tag("status", "continuing").finish()
              }
              onComplete(inprogress) { resp =>
                complete(resp)
              }
            } else {
              complete {
                span.tag("status","finished").finish()
                newChain
              }
            }
          }
        }
      }
    val bindingFuture = Http().bindAndHandle(chainRoute, "0.0.0.0", 8080)
  }
}
