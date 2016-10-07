package theon

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, RequestEntity}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import theon.auth.AwsRequestSigner
import theon.json.JsonImplementation
import theon.json.spray.SprayJsonImplementation
import theon.model._

import scala.collection.immutable.Seq
import scala.concurrent.Future

object DynamoDbClient {


  def apply(host: String,
            port: Int,
            auth: AwsRequestSigner)
           (implicit system: ActorSystem) = {
    new DynamoDbClient(host, port, auth)(system, ActorMaterializer()) with SprayJsonImplementation with FutureReturnType
  }

  def blocking(host: String,
               port: Int,
               auth: AwsRequestSigner)
              (implicit system: ActorSystem) = {
    new DynamoDbClient(host, port, auth)(system, ActorMaterializer()) with SprayJsonImplementation with BlockingReturnType
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

  def createTable(tableName: String,
                  attributeDefinitions: Seq[AttributeDefinition],
                  keySchema: Seq[KeySchemaElement],
                  provisionedThroughput: ProvisionedThroughput,
                  globalSecondaryIndexes: Seq[GlobalSecondaryIndex] = Seq.empty,
                  localSecondaryIndexes: Seq[LocalSecondaryIndex] = Seq.empty,
                  streamSpecification: Option[StreamSpecification] = None): ReturnType[CreateTableResponse] = {

    val request = CreateTable(attributeDefinitions, globalSecondaryIndexes, keySchema, localSecondaryIndexes, provisionedThroughput, streamSpecification, tableName)
    val httpRequest = Marshal(request).to[RequestEntity] map { entity =>
      HttpRequest(method = POST, entity = entity, headers = `X-Amz-Target`("DynamoDB_20120810.CreateTable") :: Nil)
    }
    singleRequest[CreateTableResponse](httpRequest)
  }

  def putItem(tableName: String,
              item: AttributeValueMap,
              returnConsumedCapacity: ReturnConsumedCapacity = ReturnConsumedCapacity.NONE,
              returnItemCollectionMetrics: ReturnItemCollectionMetrics = ReturnItemCollectionMetrics.NONE,
              returnValues: ReturnValues = ReturnValues.NONE,
              conditionalExpression: Option[String] = None,
              expressionAttributeNames: Map[String,String] = Map.empty,
              expressionAttributeValues: AttributeValueMap = Map.empty): ReturnType[PutItemResponse] = {
    val request = PutItem(conditionalExpression, expressionAttributeNames, expressionAttributeValues, item, returnConsumedCapacity, returnItemCollectionMetrics, returnValues, tableName)
    val httpRequest = Marshal(request).to[RequestEntity] map { entity =>
      HttpRequest(method = POST, entity = entity, headers = `X-Amz-Target`("DynamoDB_20120810.PutItem") :: Nil)
    }
    singleRequest[PutItemResponse](httpRequest)
  }

  def deleteItem(tableName: String,
                 key: AttributeValueMap,
                 returnConsumedCapacity: ReturnConsumedCapacity = ReturnConsumedCapacity.NONE,
                 returnItemCollectionMetrics: ReturnItemCollectionMetrics = ReturnItemCollectionMetrics.NONE,
                 returnValues: ReturnValues = ReturnValues.NONE,
                 conditionalExpression: Option[String] = None,
                 expressionAttributeNames: Map[String,String] = Map.empty,
                 expressionAttributeValues: AttributeValueMap = Map.empty): ReturnType[DeleteItemResponse] = {
    val request = DeleteItem(key, tableName, conditionalExpression, expressionAttributeNames, expressionAttributeValues, returnConsumedCapacity, returnItemCollectionMetrics, returnValues)
    val httpRequest = Marshal(request).to[RequestEntity] map { entity =>
      HttpRequest(method = POST, entity = entity, headers = `X-Amz-Target`("DynamoDB_20120810.DeleteItem") :: Nil)
    }
    singleRequest[DeleteItemResponse](httpRequest)
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
