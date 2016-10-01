import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, _}
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}
 
object WebClient {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val signer = new SignatureV4Signer("test", "test")

    val xAmzJson10 = ContentType(MediaType.customWithFixedCharset("application", "x-amz-json-1.0", HttpCharsets.`UTF-8`))

    val headers =
      Host("dynamodb.eu-west-1.localhost") ::
      RawHeader("X-Amz-Target", "DynamoDB_20120810.CreateTable") :: Nil

    val request = signer.sign(HttpRequest(uri = "/", method = HttpMethods.POST, headers = headers, entity = HttpEntity.Strict(xAmzJson10, ByteString(
      """
        |{
        |    "AttributeDefinitions": [
        |        {
        |            "AttributeName": "ForumName",
        |            "AttributeType": "S"
        |        },
        |        {
        |            "AttributeName": "Subject",
        |            "AttributeType": "S"
        |        },
        |        {
        |            "AttributeName": "LastPostDateTime",
        |            "AttributeType": "S"
        |        }
        |    ],
        |    "TableName": "Thread",
        |    "KeySchema": [
        |        {
        |            "AttributeName": "ForumName",
        |            "KeyType": "HASH"
        |        },
        |        {
        |            "AttributeName": "Subject",
        |            "KeyType": "RANGE"
        |        }
        |    ],
        |    "LocalSecondaryIndexes": [
        |        {
        |            "IndexName": "LastPostIndex",
        |            "KeySchema": [
        |                {
        |                    "AttributeName": "ForumName",
        |                    "KeyType": "HASH"
        |                },
        |                {
        |                    "AttributeName": "LastPostDateTime",
        |                    "KeyType": "RANGE"
        |                }
        |            ],
        |            "Projection": {
        |                "ProjectionType": "KEYS_ONLY"
        |            }
        |        }
        |    ],
        |    "ProvisionedThroughput": {
        |        "ReadCapacityUnits": 5,
        |        "WriteCapacityUnits": 5
        |    }
        |}
      """.stripMargin))))

    println(request)

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection("localhost", 8000)
    val responseFuture: Future[HttpResponse] =
      Source.single(request)
        .via(connectionFlow)
        .runWith(Sink.head)
 
    responseFuture.andThen {
      case Success(res) =>
        println(res.status)
        res.entity.dataBytes.runForeach(bs => println(bs.utf8String))
      case Failure(_) =>
        println("request failed")
    }.andThen {
      case _ => system.terminate()
    }
  }
}