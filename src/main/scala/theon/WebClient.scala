package theon

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import theon.actions.CreateTableRequest
import theon.auth.SignatureV4Signer
import theon.model.AttributeType._
import theon.model.KeyType._
import theon.model.{AttributeDefinition, KeySchemaElement, ProvisionedThroughput}
import theon.json.DynamoDbSprayProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.util.{Failure, Success}
 
object WebClient {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val signer = new SignatureV4Signer("test", "test")
    val client = new DynamoDbClient("localhost", 8000, signer)

    //val xAmzJson10 = ContentType(MediaType.customWithFixedCharset("application", "x-amz-json-1.0", HttpCharsets.`UTF-8`))

    val responseFuture = client.singleRequest(CreateTableRequest("user", AttributeDefinition("id", STRING) :: Nil, KeySchemaElement("id", HASH) :: Nil, ProvisionedThroughput(100, 10)))
 
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