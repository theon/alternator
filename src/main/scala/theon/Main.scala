package theon

import java.util.UUID

import akka.actor.ActorSystem
import theon.auth.SignatureV4Signer
import theon.model.AttributeType._
import theon.model.KeyType._
import theon.model._
 
object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val executionContext = system.dispatcher

    val signer = new SignatureV4Signer("test2", "test")
    val client = DynamoDbClient.blocking("localhost", 8000, signer)

    try {

      val tableName = UUID.randomUUID.toString

      val createResponse = client.createTable(tableName, AttributeDefinition("id", STRING) :: Nil, KeySchemaElement("id", HASH) :: Nil, ProvisionedThroughput(100, 10))
      println(createResponse)

      val putResponse = client.putItem(tableName, Map("id" -> StringAttributeValue("blah")))
      println(putResponse)

      val getResponse = client.getItem(tableName, Map("id" -> StringAttributeValue("blah")))
      println(getResponse)

      val deleteResponse = client.deleteItem(tableName, Map("id" -> StringAttributeValue("blah")))
      println(deleteResponse)

    } catch {
      case e: Exception => e.printStackTrace
    }

    system.terminate()
  }
}