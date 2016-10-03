package theon

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import theon.auth.AwsRequestSigner

import scala.concurrent.Future

class DynamoDbClient(host: String, port: Int, auth: AwsRequestSigner)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(host, port)

  def singleRequest(request: Source[HttpRequest, NotUsed]): Future[HttpResponse] = {
    request
      .via(auth.sign)
      .via(connectionFlow)
      .runWith(Sink.head)
  }
}
