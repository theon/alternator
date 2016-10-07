package theon.json.spray

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling.{Unmarshaller, _}
import akka.util.ByteString
import spray.json._
import theon.`application/x-aws-json-1.0`
import theon.json.JsonImplementation
import theon.json.spray.SprayJsonHacks.DoNotRenderEmptyCollections
import theon.model._

trait SprayJsonImplementation extends DefaultJsonProtocol with DoNotRenderEmptyCollections with JsonImplementation {

  implicit object AttributeTypeFormat extends JsonFormat[AttributeType] {
    def write(x: AttributeType) = JsString(x.awsName)
    def read(value: JsValue): AttributeType = value match {
      case JsString("N") => AttributeType.NUMBER
      case JsString("S") => AttributeType.STRING
      case JsString("B") => AttributeType.BINARY
      case JsString("BOOL") => AttributeType.BOOLEAN
      case JsString("SS") => AttributeType.STRING_SET
      case JsString("BS") => AttributeType.BINARY_SET
      case JsString("NS") => AttributeType.NUMBER_SET
      case JsString("L") => AttributeType.LIST
      case JsString("M") => AttributeType.MAP
      case JsString("NULL") => AttributeType.NULL
      case x => deserializationError(x + " is an unknown AWS type field name")
    }
  }

  implicit object AttributeDefinitionTypeFormat extends JsonFormat[AttributeDefinitionType] {
    def write(x: AttributeDefinitionType) = JsString(x.awsName)
    def read(value: JsValue): AttributeDefinitionType = value match {
      case JsString("N") => AttributeType.NUMBER
      case JsString("S") => AttributeType.STRING
      case JsString("B") => AttributeType.BINARY
      case x => deserializationError(x + " is an unknown AWS attribute definition type field name")
    }
  }

  implicit object AttributeValueFormat extends JsonFormat[AttributeValue] {
    def write(x: AttributeValue) = x match {
      case x: NumberAttributeValue => NumberAttributeValueFormat.write(x)
      case x: StringAttributeValue => StringAttributeValueFormat.write(x)
      case x: BinaryAttributeValue => BinaryAttributeValueFormat.write(x)
      case x: BooleanAttributeValue => BooleanAttributeValueFormat.write(x)
      case x: StringSetAttributeValue => StringSetAttributeValueFormat.write(x)
      case x: BinarySetAttributeValue => BinarySetAttributeValueFormat.write(x)
      case x: NumberSetAttributeValue => NumberSetAttributeValueFormat.write(x)
      case x: ListAttributeValue => ListAttributeValueFormat.write(x)
      case x: MapAttributeValue => MapAttributeValueFormat.write(x)
      case x: NullAttributeValue.type => NullAttributeValueFormat.write(x)
    }

    def read(value: JsValue): AttributeValue = {
      val fields = value.asJsObject.fields

      if(fields.get("NULL").contains(JsTrue)) {
        NullAttributeValueFormat.read(value)
      }

      // TODO: Tidier way to do this?
      fields.keys.filterNot(_ == "NULL").headOption match {
        case Some("N") => NumberAttributeValueFormat.read(value)
        case Some("S") => StringAttributeValueFormat.read(value)
        case Some("B") => BinaryAttributeValueFormat.read(value)
        case Some("BOOL") => BooleanAttributeValueFormat.read(value)
        case Some("SS") => StringSetAttributeValueFormat.read(value)
        case Some("BS") => BinarySetAttributeValueFormat.read(value)
        case Some("NS") => NumberSetAttributeValueFormat.read(value)
        case Some("L") => ListAttributeValueFormat.read(value)
        case Some("M") => MapAttributeValueFormat.read(value)
        case x => deserializationError(x + " is an unknown AWS type field name")
      }
    }
  }

  implicit object NumberAttributeValueFormat extends JsonFormat[NumberAttributeValue] {
    def write(x: NumberAttributeValue) = JsObject("N" -> JsString(x.value.toString))
    def read(value: JsValue) = value.asJsObject.fields.get("N") match {
      case Some(JsString(x)) => NumberAttributeValue(BigDecimal(x))
      case x => deserializationError("Expected { \"N\": \"number\" }, got " + x)
    }
  }

  implicit object StringAttributeValueFormat extends JsonFormat[StringAttributeValue] {
    def write(x: StringAttributeValue) = JsObject("S" -> JsString(x.value))
    def read(value: JsValue) = value.asJsObject.fields.get("S") match {
      case Some(JsString(x)) => StringAttributeValue(x)
      case x => deserializationError("Expected { \"S\": \"string\" }, got " + x)
    }
  }

  private def base64Encode(bytes: ByteString): String = {
    new sun.misc.BASE64Encoder().encode(bytes.asByteBuffer)
  }
  private def base64Decode(str: String): ByteString = {
    val bytes = new sun.misc.BASE64Decoder().decodeBuffer(str)
    ByteString(bytes)
  }

  implicit object BinaryAttributeValueFormat extends JsonFormat[BinaryAttributeValue] {
    def write(x: BinaryAttributeValue) = JsObject("B" -> JsString(base64Encode(x.value)))
    def read(value: JsValue) = value.asJsObject.fields.get("B") match {
      case Some(JsString(x)) => BinaryAttributeValue(base64Decode(x))
      case x => deserializationError("Expected { \"B\": \"base64Bytes\" }, got " + x)
    }
  }

  implicit object BooleanAttributeValueFormat extends JsonFormat[BooleanAttributeValue] {
    def write(x: BooleanAttributeValue) = JsObject("BOOL" -> JsBoolean(x.value))
    def read(value: JsValue) = value.asJsObject.fields.get("BOOL") match {
      case Some(JsBoolean(x)) => BooleanAttributeValue(x)
      case x => deserializationError("Expected { \"BOOL\": boolean }, got " + x)
    }
  }

  implicit object StringSetAttributeValueFormat extends JsonFormat[StringSetAttributeValue] {
    def write(x: StringSetAttributeValue) = JsObject("SS" -> JsArray(x.value.toVector.map(JsString.apply)))
    def read(value: JsValue) = value.asJsObject.fields.get("SS") match {
      case Some(JsArray(elems)) => StringSetAttributeValue(elems.collect { case JsString(s) => s }.toSet)
      case x => deserializationError("Expected { \"SS\": [ \"string\", ... ] }, got " + x)
    }
  }

  implicit object BinarySetAttributeValueFormat extends JsonFormat[BinarySetAttributeValue] {
    def write(x: BinarySetAttributeValue) = {
      val base64Strings = x.value.toVector.map(byteStr => JsString(base64Encode(byteStr)))
      JsObject("BS" -> JsArray(base64Strings))
    }
    def read(value: JsValue) = value.asJsObject.fields.get("BS") match {
      case Some(JsArray(elems)) => BinarySetAttributeValue(elems.collect { case JsString(s) => base64Decode(s) }.toSet)
      case x => deserializationError("Expected { \"BS\": [ \"string\", ... ] }, got " + x)
    }
  }

  implicit object NumberSetAttributeValueFormat extends JsonFormat[NumberSetAttributeValue] {
    def write(x: NumberSetAttributeValue) = JsObject("NS" -> JsArray(x.value.toVector.map(num => JsString(num.toString))))
    def read(value: JsValue) = value.asJsObject.fields.get("NS") match {
      case Some(JsArray(elems)) => NumberSetAttributeValue(elems.collect { case JsString(s) => BigDecimal(s) }.toSet)
      case x => deserializationError("Expected { \"NS\": [ \"number\", ... ] }, got " + x)
    }
  }

  implicit object ListAttributeValueFormat extends JsonFormat[ListAttributeValue] {
    def write(x: ListAttributeValue) = JsObject("L" -> x.value.toJson)
    def read(value: JsValue) = value.asJsObject.fields.get("L") match {
      case Some(JsArray(elems)) => ListAttributeValue(elems.map(_.convertTo[AttributeValue]))
      case x => deserializationError("Expected { \"L\": [ attributeValue, ... ] }, got " + x)
    }
  }

  implicit object MapAttributeValueFormat extends JsonFormat[MapAttributeValue] {
    def write(x: MapAttributeValue) = JsObject("M" -> JsObject(x.value.mapValues(_.toJson)))
    def read(value: JsValue) = value.asJsObject.fields.get("M") match {
      case Some(JsObject(fields)) => MapAttributeValue(fields.mapValues(_.convertTo[AttributeValue]))
      case x => deserializationError("Expected { \"M\": { \"name\": attributeValue, ... } }, got " + x)
    }
  }

  implicit object NullAttributeValueFormat extends JsonFormat[NullAttributeValue.type ] {
    def write(nullValue: NullAttributeValue.type): JsValue = JsNull
    def read(value: JsValue): NullAttributeValue.type = NullAttributeValue
  }

  implicit val attributeDefinitionFormat = jsonFormat(AttributeDefinition.apply, "AttributeName", "AttributeType")

  implicit object KeyTypeFormat extends JsonFormat[KeyType] {
    def write(x: KeyType) = JsString(x.toString)
    def read(value: JsValue): KeyType = value match {
      case JsString("HASH") => KeyType.HASH
      case JsString("RANGE") => KeyType.RANGE
      case x => deserializationError(x + " is an unknown AWS Key Type")
    }
  }

  implicit val keySchemaElementFormat = jsonFormat(KeySchemaElement.apply, "AttributeName", "KeyType")

  implicit object ProjectionTypeFormat extends JsonFormat[ProjectionType] {
    def write(x: ProjectionType) = JsString(x.toString)
    def read(value: JsValue): ProjectionType = value match {
      case JsString("KEYS_ONLY") => ProjectionType.KEYS_ONLY
      case JsString("INCLUDE") => ProjectionType.INCLUDE
      case JsString("ALL") => ProjectionType.ALL
      case x => deserializationError(x + " is an unknown AWS Projection Type")
    }
  }

  implicit val projectionFormat = jsonFormat(Projection, "NonKeyAttributes", "ProjectionType")

  implicit val provisionedThroughputFormat = jsonFormat(ProvisionedThroughput.apply, "ReadCapacityUnits", "WriteCapacityUnits")

  implicit val provisionedThroughputDescriptionFormat = jsonFormat(ProvisionedThroughputDescription.apply, "LastDecreaseDateTime", "LastIncreaseDateTime", "NumberOfDecreasesToday", "ReadCapacityUnits", "WriteCapacityUnits")

  implicit val globalSecondaryIndexFormat = jsonFormat(GlobalSecondaryIndex.apply, "IndexName", "KeySchema", "Projection", "ProvisionedThroughput")

  implicit object IndexStatusFormat extends JsonFormat[IndexStatus] {
    def write(x: IndexStatus) = JsString(x.toString)
    def read(value: JsValue): IndexStatus = value match {
      case JsString("ACTIVE") => IndexStatus.ACTIVE
      case JsString("CREATING") => IndexStatus.CREATING
      case JsString("UPDATING") => IndexStatus.UPDATING
      case JsString("DELETING") => IndexStatus.DELETING
      case x => deserializationError(x + " is an unknown AWS Index Status")
    }
  }

  implicit object TableStatusFormat extends JsonFormat[TableStatus] {
    def write(x: TableStatus) = JsString(x.toString)
    def read(value: JsValue): TableStatus = value match {
      case JsString("ACTIVE") => TableStatus.ACTIVE
      case JsString("CREATING") => TableStatus.CREATING
      case JsString("UPDATING") => TableStatus.UPDATING
      case JsString("DELETING") => TableStatus.DELETING
      case x => deserializationError(x + " is an unknown AWS Table Status")
    }
  }

  implicit val globalSecondaryIndexDescriptionFormat = jsonFormat(GlobalSecondaryIndexDescription.apply, "Backfilling", "IndexArn", "IndexName", "IndexSizeBytes", "IndexStatus", "ItemCount", "KeySchema")

  implicit object StreamViewTypeFormat extends JsonFormat[StreamViewType] {
    def write(x: StreamViewType) = JsString(x.toString)
    def read(value: JsValue): StreamViewType = value match {
      case JsString("KEYS_ONLY") => StreamViewType.KEYS_ONLY
      case JsString("NEW_IMAGE") => StreamViewType.NEW_IMAGE
      case JsString("OLD_IMAGE") => StreamViewType.OLD_IMAGE
      case JsString("NEW_AND_OLD_IMAGES") => StreamViewType.NEW_AND_OLD_IMAGES
      case x => deserializationError(x + " is an unknown AWS Stream View Type")
    }
  }

  implicit val localSecondaryIndexFormat = jsonFormat(LocalSecondaryIndex.apply, "IndexName", "KeySchema", "Projection")

  implicit val localSecondaryIndexDescriptionFormat = jsonFormat(LocalSecondaryIndexDescription.apply, "IndexArn", "IndexName", "IndexSizeBytes", "ItemCount", "KeySchema", "Projection")

  implicit val streamSpecificationFormat = jsonFormat(StreamSpecification.apply, "StreamEnabled", "StreamViewType")

  implicit val tableDescriptionFormat = jsonFormat(TableDescription.apply, "AttributeDefinitions", "CreationDateTime", "GlobalSecondaryIndexes", "ItemCount", "KeySchema", "LatestStreamArn", "LatestStreamLabel", "LocalSecondaryIndexes", "ProvisionedThroughput", "StreamSpecification", "TableArn", "TableName", "TableSizeBytes", "TableStatus")

  implicit val dynamoDbFailureFormat = jsonFormat(DynamoDbFailure.apply, "__type", "message")
  implicit def dynamoDbFailureUnmarshaller: FromEntityUnmarshaller[DynamoDbFailure] = awsJsonUnmarshaller[DynamoDbFailure]

  implicit object ReturnConsumedCapacityFormat extends JsonFormat[ReturnConsumedCapacity] {
    def write(x: ReturnConsumedCapacity) = JsString(x.toString)
    def read(value: JsValue): ReturnConsumedCapacity = value match {
      case JsString("INDEXES") => ReturnConsumedCapacity.INDEXES
      case JsString("TOTAL") => ReturnConsumedCapacity.TOTAL
      case JsString("NONE") => ReturnConsumedCapacity.NONE
      case x => deserializationError(x + " is an unknown AWS ReturnConsumedCapacity")
    }
  }

  implicit val capacityFormat = jsonFormat(Capacity, "CapacityUnits")

  implicit val consumedCapacityFormat = jsonFormat(ConsumedCapacity, "CapacityUnits", "GlobalSecondaryIndexes", "LocalSecondaryIndexes", "Table", "TableName")

  implicit object ReturnItemCollectionMetricsFormat extends JsonFormat[ReturnItemCollectionMetrics] {
    def write(x: ReturnItemCollectionMetrics) = JsString(x.toString)
    def read(value: JsValue): ReturnItemCollectionMetrics = value match {
      case JsString("SIZE") => ReturnItemCollectionMetrics.SIZE
      case JsString("NONE") => ReturnItemCollectionMetrics.NONE
      case x => deserializationError(x + " is an unknown AWS ReturnItemCollectionMetrics")
    }
  }

  implicit val itemCollectionMetrics = jsonFormat(ItemCollectionMetrics.apply, "ItemCollectionKey", "SizeEstimateRangeGB")

  implicit object ReturnValuesFormat extends JsonFormat[ReturnValues] {
    def write(x: ReturnValues) = JsString(x.toString)
    def read(value: JsValue): ReturnValues = value match {
      case JsString("ALL_NEW") => ReturnValues.ALL_NEW
      case JsString("ALL_OLD") => ReturnValues.ALL_OLD
      case JsString("UPDATED_NEW") => ReturnValues.UPDATED_NEW
      case JsString("UPDATED_OLD") => ReturnValues.UPDATED_OLD
      case JsString("NONE") => ReturnValues.NONE
      case x => deserializationError(x + " is an unknown AWS ReturnValues")
    }
  }

  // Actions

  def awsJsonUnmarshaller[T](implicit reader: RootJsonReader[T]) =
    Unmarshaller
      .stringUnmarshaller
      .forContentTypes(`application/x-aws-json-1.0`)
      .map(input => reader.read(input.parseJson))

  implicit val createTableFormat = jsonFormat(CreateTable.apply, "AttributeDefinitions", "GlobalSecondaryIndexes", "KeySchema", "LocalSecondaryIndexes", "ProvisionedThroughput", "StreamSpecification", "TableName")
  implicit val createTableMarshaller: ToEntityMarshaller[CreateTable] = sprayJsonMarshaller[CreateTable]

  implicit val createTableResponseFormat = jsonFormat(CreateTableResponse, "TableDescription")
  implicit val createTableResponseUnmarshaller: FromEntityUnmarshaller[CreateTableResponse] = awsJsonUnmarshaller[CreateTableResponse]

  implicit def putItemFormat = jsonFormat(PutItem.apply, "ConditionalExpression", "ExpressionAttributeNames", "ExpressionAttributeValues", "Item", "ReturnConsumedCapacity", "ReturnItemCollectionMetrics", "ReturnValues", "TableName")
  implicit def putItemMarshaller: ToEntityMarshaller[PutItem] = sprayJsonMarshaller[PutItem]

  implicit def putItemResponseFormat = jsonFormat(PutItemResponse.apply, "Attributes", "ConsumedCapacity", "ItemCollectionMetrics")
  implicit def putItemResponseUnmarshaller: FromEntityUnmarshaller[PutItemResponse] = awsJsonUnmarshaller[PutItemResponse]

  implicit def deleteItemFormat = jsonFormat(DeleteItem.apply, "Key", "TableName", "ConditionalExpression", "ExpressionAttributeNames", "ExpressionAttributeValues", "ReturnConsumedCapacity", "ReturnItemCollectionMetrics", "ReturnValues")
  implicit def deleteItemMarshaller: ToEntityMarshaller[DeleteItem] = sprayJsonMarshaller[DeleteItem]

  implicit def deleteItemResponseFormat = jsonFormat(DeleteItemResponse.apply, "Attributes", "ConsumedCapacity", "ItemCollectionMetrics")
  implicit def deleteItemResponseUnmarshaller: FromEntityUnmarshaller[DeleteItemResponse] = awsJsonUnmarshaller[DeleteItemResponse]
}

object SprayJsonImplementation extends SprayJsonImplementation

