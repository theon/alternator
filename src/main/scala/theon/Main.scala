package theon

import java.util.UUID

import akka.actor.ActorSystem
import theon.auth.SignatureV4Signer
import theon.model.AttributeType._
import theon.model.KeyType._
import theon.model._

import scala.util.{Failure, Success}
 
object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.dispatcher

    val signer = new SignatureV4Signer("test2", "test")
    val client = DynamoDbClient("localhost", 8000, signer)

    val tableName = UUID.randomUUID.toString

    val responseFuture = client.createTable(tableName, AttributeDefinition("id", STRING) :: Nil, KeySchemaElement("id", HASH) :: Nil, ProvisionedThroughput(100, 10))

    responseFuture.andThen {
      case Success(res) =>
        println(res)
      case Failure(e) =>
        e.printStackTrace()
        println("request failed")
    }.andThen {
      case _ =>

        val putResponse = client.putItem(tableName, Map("id" -> StringAttributeValue("blah")))

        putResponse.andThen {
          case Success(res) =>
            println(res)
          case Failure(e) =>
            e.printStackTrace()
            println("put request failed")
        }.andThen {
          case _ =>
            system.terminate()
        }
    }
  }
}