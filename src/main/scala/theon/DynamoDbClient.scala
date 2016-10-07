package theon

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, RequestEntity}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import theon.auth.AwsRequestSigner
import theon.json.JsonImplementation
import theon.json.spray.SprayJsonImplementation
import theon.model.{CreateTable, CreateTableResponse, DynamoDbException}

import scala.concurrent.Future

object DynamoDbClient {


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

  val requestLogging: Flow[HttpRequest, HttpRequest, NotUsed] = Flow.fromFunction { r =>
    println(r) // TODO: Use Akka logger
    r
  }

  val responseLogging: Flow[HttpResponse, HttpResponse, NotUsed] = Flow.fromFunction { r =>
    println(r.status) // TODO: Use Akka logger
    r.entity.dataBytes.runForeach(bs => println(bs.utf8String))
    r
  }

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
      .via(requestLogging)
      .via(connectionFlow)
      .via(responseLogging)
      .mapAsync(1) {
        case res if res.status.isFailure =>
          dynamoDbFailureUnmarshaller(res.entity).map(failure => throw DynamoDbException(res.status, failure))
        case res =>
          unmarshaller(res.entity)
      }

    convert(source)
  }
}
