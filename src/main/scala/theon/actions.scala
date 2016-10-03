package theon

import akka.NotUsed
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.scaladsl.Source
import theon.model.{StreamSpecification, _}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext


object actions {

  case class CreateTable(attributeDefinitions: Seq[AttributeDefinition],
                         globalSecondaryIndexes: Seq[GlobalSecondaryIndex],
                         keySchema: Seq[KeySchemaElement],
                         localSecondaryIndexes: Seq[LocalSecondaryIndex],
                         provisionedThroughput: ProvisionedThroughput,
                         streamSpecification: Option[StreamSpecification],
                         tableName: String)

  object CreateTableRequest {
    def apply(tableName: String,
              attributeDefinitions: Seq[AttributeDefinition],
              keySchema: Seq[KeySchemaElement],
              provisionedThroughput: ProvisionedThroughput,
              globalSecondaryIndexes: Seq[GlobalSecondaryIndex] = Seq.empty,
              localSecondaryIndexes: Seq[LocalSecondaryIndex] = Seq.empty,
              streamSpecification: Option[StreamSpecification] = None)
             (implicit marshaller: ToEntityMarshaller[CreateTable], ec: ExecutionContext): Source[HttpRequest, NotUsed] = {
      val createTable = CreateTable(attributeDefinitions, globalSecondaryIndexes, keySchema, localSecondaryIndexes, provisionedThroughput, streamSpecification, tableName)

      val httpRequest = Marshal(createTable).to[RequestEntity] map { entity =>
        HttpRequest(uri = "/", method = HttpMethods.POST, entity = entity, headers = `X-Amz-Target`("DynamoDB_20120810.CreateTable") :: Nil)
      }

      Source.fromFuture(httpRequest)
    }
  }

}
