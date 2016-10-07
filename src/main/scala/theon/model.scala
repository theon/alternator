package theon

import akka.http.scaladsl.model.StatusCode
import akka.util.ByteString

import scala.collection.immutable.Seq

/**
  * Model as described at [[http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/Welcome.html]]
  */
object model {

  sealed trait AttributeType { def awsName: String }
  sealed trait AttributeDefinitionType extends AttributeType

  object AttributeType {
    case object NUMBER extends AttributeDefinitionType {
      val awsName = "N"
    }
    case object STRING extends AttributeDefinitionType {
      val awsName = "S"
    }
    case object BINARY extends AttributeDefinitionType {
      val awsName = "B"
    }
    case object BOOLEAN extends AttributeType {
      val awsName = "BOOL"
    }
    case object STRING_SET extends AttributeType {
      val awsName = "SS"
    }
    case object BINARY_SET extends AttributeType {
      val awsName = "BS"
    }
    case object NUMBER_SET extends AttributeType {
      val awsName = "NS"
    }
    case object LIST extends AttributeType {
      val awsName = "L"
    }
    case object MAP extends AttributeType {
      val awsName = "M"
    }
    case object NULL extends AttributeType {
      val awsName = "NULL"
    }
  }

  sealed trait AttributeValue {
    //def typ: AttributeType
  }

  type AttributeValueMap = Map[String,AttributeValue]

  case class NumberAttributeValue(value: BigDecimal) extends AttributeValue //{ val typ = NUMBER }
  case class StringAttributeValue(value: String) extends AttributeValue //{ val typ = STRING }
  case class BinaryAttributeValue(value: ByteString) extends AttributeValue //{ val typ = BINARY }
  case class BooleanAttributeValue(value: Boolean) extends AttributeValue //{ val typ = BOOLEAN }
  case class StringSetAttributeValue(value: Set[String]) extends AttributeValue //{ val typ = STRING_SET }
  case class BinarySetAttributeValue(value: Set[ByteString]) extends AttributeValue //{ val typ = BINARY_SET }
  case class NumberSetAttributeValue(value: Set[BigDecimal]) extends AttributeValue //{ val typ = NUMBER_SET }
  //TODO: Work out if List takes just values or full key+value attributes
  case class ListAttributeValue(value: Seq[AttributeValue]) extends AttributeValue //{ val typ = LIST }
  case class MapAttributeValue(value: Map[String,AttributeValue]) extends AttributeValue //{ val typ = MAP }
  case object NullAttributeValue extends AttributeValue //{ val typ = NULL }

  case class AttributeDefinition(name: String, typ: AttributeDefinitionType)

  case class GlobalSecondaryIndex(indexName: String, keySchema: Seq[KeySchemaElement], projection: Projection, provisionedThroughput: ProvisionedThroughput)

  case class GlobalSecondaryIndexDescription(backFilling: Boolean, indexArn: String, indexName: String, indexSizeBytes: Long, indexStatus: IndexStatus, itemCount: Long, keySchema: Seq[KeySchemaElement])

  sealed trait IndexStatus
  object IndexStatus {
    case object CREATING extends IndexStatus
    case object UPDATING extends IndexStatus
    case object DELETING extends IndexStatus
    case object ACTIVE extends IndexStatus
  }

  sealed trait TableStatus
  object TableStatus {
    case object CREATING extends TableStatus
    case object UPDATING extends TableStatus
    case object DELETING extends TableStatus
    case object ACTIVE extends TableStatus
  }

  sealed trait KeyType
  object KeyType {
    case object HASH extends KeyType
    case object RANGE extends KeyType
  }

  case class KeySchemaElement(attributeName: String, keyType: KeyType)

  case class LocalSecondaryIndex(indexName: String, keySchema: Seq[KeySchemaElement], projection: Projection)

  case class LocalSecondaryIndexDescription(indexArn: String, indexName: String, indexSizeBytes: Long, itemCount: Long, keySchema: Seq[KeySchemaElement], projection: Projection)

  sealed trait ProjectionType
  object ProjectionType {
    case object KEYS_ONLY extends ProjectionType
    case object INCLUDE extends ProjectionType
    case object ALL extends ProjectionType
  }

  case class Projection(nonKeyAttributes: Seq[String], projectionType: ProjectionType)

  case class ProvisionedThroughput(readCapacityUnits: Long, writeCapacityUnits: Long)

  // TODO: DateTime?
  case class ProvisionedThroughputDescription(lastDecreaseDateTime: BigDecimal, lastIncreaseDateTime: BigDecimal, numberOfDecreasesToday: Long, readCapacityUnits: Long, writeCapacityUnits: Long)

  sealed trait StreamViewType
  object StreamViewType {
    case object KEYS_ONLY extends StreamViewType
    case object NEW_IMAGE extends StreamViewType
    case object OLD_IMAGE extends StreamViewType
    case object NEW_AND_OLD_IMAGES extends StreamViewType
  }


  case class StreamSpecification(streamEnabled: Boolean, streamViewType: StreamViewType)

  case class DynamoDbFailure(failureType: String, message: String)

  case class DynamoDbException(httpStatus: StatusCode, failure: DynamoDbFailure) extends Exception(failure.message)

  sealed trait ReturnConsumedCapacity
  object ReturnConsumedCapacity {
    case object INDEXES extends ReturnConsumedCapacity
    case object TOTAL extends ReturnConsumedCapacity
    case object NONE extends ReturnConsumedCapacity
  }

  case class Capacity(capacityUnits: Double)
  case class ConsumedCapacity(capacityUnits: Double, globalSecondaryIndexes: Map[String,Capacity], localSecondaryIndexes: Map[String,Capacity], table: Option[Capacity], tableName: String)

  sealed trait ReturnItemCollectionMetrics
  object ReturnItemCollectionMetrics {
    case object SIZE extends ReturnItemCollectionMetrics
    case object NONE extends ReturnItemCollectionMetrics
  }

  case class ItemCollectionMetrics(itemCollectionKey: AttributeValueMap, sizeEstimateRangeGB: (Double,Double))

  sealed trait ReturnValues
  object ReturnValues {
    case object UPDATED_NEW extends ReturnValues
    case object UPDATED_OLD extends ReturnValues
    case object ALL_NEW extends ReturnValues
    case object ALL_OLD extends ReturnValues
    case object NONE extends ReturnValues
  }

  // Actions

  case class CreateTable(attributeDefinitions: Seq[AttributeDefinition],
                         globalSecondaryIndexes: Seq[GlobalSecondaryIndex],
                         keySchema: Seq[KeySchemaElement],
                         localSecondaryIndexes: Seq[LocalSecondaryIndex],
                         provisionedThroughput: ProvisionedThroughput,
                         streamSpecification: Option[StreamSpecification],
                         tableName: String)

  case class CreateTableResponse(tableDescription: TableDescription)
  case class TableDescription(attributeDefinitions: Seq[AttributeDefinition],
                              creationDateTime: BigDecimal, // TODO: DateTime?
                              globalSecondaryIndexes: Seq[GlobalSecondaryIndex],
                              itemCount: Long,
                              keySchema: Seq[KeySchemaElement],
                              latestStreamArn: Option[String],
                              latestStreamLabel: Option[String],
                              localSecondaryIndexes: Seq[LocalSecondaryIndexDescription],
                              provisionedThroughput: ProvisionedThroughputDescription,
                              streamSpecification: Option[StreamSpecification],
                              tableArn: String,
                              tableName: String,
                              tableSizeBytes: Long,
                              tableStatus: TableStatus)

  case class PutItem(conditionExpression: Option[String],
                     expressionAttributeNames: Map[String,String],
                     expressionAttributeValues: AttributeValueMap,
                     item: AttributeValueMap,
                     returnConsumedCapacity: ReturnConsumedCapacity,
                     returnItemCollectionMetrics: ReturnItemCollectionMetrics,
                     returnValues: ReturnValues,
                     tableName: String)

  case class PutItemResponse(attributes: AttributeValueMap,
                             consumedCapacity: Option[ConsumedCapacity],
                             itemCollectionMetrics: Option[ItemCollectionMetrics])

  case class DeleteItem(key: AttributeValueMap,
                        tableName: String,
                        conditionalExpression: Option[String],
                        expressionAttributeNames: Map[String,String],
                        expressionAttributeValues: AttributeValueMap,
                        returnConsumedCapacity: ReturnConsumedCapacity,
                        returnItemCollectionMetrics: ReturnItemCollectionMetrics,
                        returnValues: ReturnValues)

  case class DeleteItemResponse(attributes: AttributeValueMap,
                                consumedCapacity: Option[ConsumedCapacity],
                                itemCollectionMetrics: Option[ItemCollectionMetrics])
}
