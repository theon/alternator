package theon.json

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import theon.model._

trait JsonImplementation {

  // TODO: Is there a way to easily support pluggable JSON implementation without all these marshallers?

  implicit def createTableMarshaller: ToEntityMarshaller[CreateTable]
  implicit def createTableResponseUnmarshaller: FromEntityUnmarshaller[CreateTableResponse]

  implicit def putItemMarshaller: ToEntityMarshaller[PutItem]
  implicit def putItemResponseUnmarshaller: FromEntityUnmarshaller[PutItemResponse]

  implicit def deleteItemMarshaller: ToEntityMarshaller[DeleteItem]
  implicit def deleteItemResponseUnmarshaller: FromEntityUnmarshaller[DeleteItemResponse]

  implicit def dynamoDbFailureUnmarshaller: FromEntityUnmarshaller[DynamoDbFailure]
}
