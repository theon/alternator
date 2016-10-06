package theon

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import theon.auth.SignatureV4Signer
import theon.model.AttributeType._
import theon.model.KeyType._
import theon.model.{AttributeDefinition, CreateTable, KeySchemaElement, ProvisionedThroughput}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}
 
object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val signer = new SignatureV4Signer("test2", "test")
    val client = DynamoDbClient("localhost", 8000, signer)

    //val xAmzJson10 = ContentType(MediaType.customWithFixedCharset("application", "x-amz-json-1.0", HttpCharsets.`UTF-8`))

    val responseFuture = client.createTable(CreateTable(AttributeDefinition("id", STRING) :: Nil, Seq.empty, KeySchemaElement("id", HASH) :: Nil, Seq.empty, ProvisionedThroughput(100, 10), None, UUID.randomUUID.toString))
 
    responseFuture.andThen {
      case Success(res) =>
        println(res)
        //res.entity.dataBytes.runForeach(bs => println(bs.utf8String))
      case Failure(e) =>
        e.printStackTrace()
        println("request failed")
    }.andThen {
      case _ => system.terminate()
    }
  }
}