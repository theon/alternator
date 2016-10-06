package theon

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, RequestEntity}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import theon.DynamoDbClient.ReturnTypeConverter
import theon.auth.{AwsRequestSigner, SignatureV4Signer}
import theon.json.JsonImplementation
import theon.json.spray.SprayJsonImplementation
import theon.model.{CreateTable, CreateTableResponse}

import scala.concurrent.Future

object DynamoDbClient {
  trait ReturnTypeConverter {
    type ReturnType[_]
    def convert[T](x: Source[T, NotUsed]): ReturnType[T]
  }

  trait SourceReturnType extends ReturnTypeConverter {
    type ReturnType[T] = Source[T, NotUsed]
    def convert[T](x: Source[T, NotUsed]): ReturnType[T] = x
  }

  trait FutureReturnType extends ReturnTypeConverter {
    type ReturnType[T] = Future[T]
    def materializer: ActorMaterializer
    def convert[T](x: Source[T, NotUsed]) = x.runWith(Sink.head)(materializer)
  }

  def apply(host: String,
            port: Int,
            auth: AwsRequestSigner)
           (implicit system: ActorSystem, materializer: ActorMaterializer) = {
    new DynamoDbClient(host, port, auth) with SprayJsonImplementation with FutureReturnType
  }
}

class DynamoDbClient(host: String,
                     port: Int,
                     auth: AwsRequestSigner)
                    (implicit val system: ActorSystem, val materializer: ActorMaterializer) {
  this: ReturnTypeConverter with JsonImplementation =>

  import system.dispatcher

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(host, port)

  def createTable[T](req: CreateTable): ReturnType[CreateTableResponse] = {
    val httpRequest = Marshal(req).to[RequestEntity] map { entity =>
      HttpRequest(uri = "/", method = HttpMethods.POST, entity = entity, headers = `X-Amz-Target`("DynamoDB_20120810.CreateTable") :: Nil)
    }
    singleRequest[CreateTableResponse](httpRequest)
  }

  def singleRequest[O](request: Future[HttpRequest])
                      (implicit unmarshaller: FromEntityUnmarshaller[O]): ReturnType[O] = {
    val source = Source.fromFuture(request)
      .via(auth.sign)
      .via(connectionFlow)
      .map { r =>
        // Logging
        println(r.status)
        r.entity.dataBytes.runForeach(bs => println(bs.utf8String))
        r
      }
      .mapAsync(1)(res => unmarshaller(res.entity))

    convert(source)
  }
}
