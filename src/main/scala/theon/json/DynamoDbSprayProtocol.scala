package theon.json

import akka.util.ByteString
import spray.json.DefaultJsonProtocol
import spray.json._
import theon.actions._
import theon.model._

import scala.collection.Iterable
import scala.collection.immutable.Seq

trait DynamoDbSprayProtocol extends DefaultJsonProtocol with NullEmptyCollections {

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
      case x: NumberAttributeValue => NumberFormat.write(x)
      case x: StringAttributeValue => StringFormat.write(x)
      case x: BinaryAttributeValue => BinaryFormat.write(x)
      case x: BooleanAttributeValue => BooleanFormat.write(x)
      case x: StringSetAttributeValue => StringSetFormat.write(x)
      case x: BinarySetAttributeValue => BinarySetFormat.write(x)
      case x: NumberSetAttributeValue => NumberSetFormat.write(x)
      case x: ListAttributeValue => ListFormat.write(x)
      case x: MapAttributeValue => MapFormat.write(x)
      case x: NullAttributeValue.type => NullFormat.write(x)
    }

    def read(value: JsValue): AttributeValue = {
      val fields = value.asJsObject.fields

      if(fields.get("NULL").contains(JsTrue)) {
        NullFormat.read(value)
      }

      // TODO: Tidier way to do this?
      fields.keys.filterNot(_ == "NULL").headOption match {
        case Some("N") => NumberFormat.read(value)
        case Some("S") => StringFormat.read(value)
        case Some("B") => BinaryFormat.read(value)
        case Some("BOOL") => BooleanFormat.read(value)
        case Some("SS") => StringSetFormat.read(value)
        case Some("BS") => BinarySetFormat.read(value)
        case Some("NS") => NumberSetFormat.read(value)
        case Some("L") => ListFormat.read(value)
        case Some("M") => MapFormat.read(value)
        case x => deserializationError(x + " is an unknown AWS type field name")
      }
    }
  }

  implicit object NumberFormat extends JsonFormat[NumberAttributeValue] {
    def write(x: NumberAttributeValue) = JsObject("N" -> JsString(x.value.toString))
    def read(value: JsValue) = value.asJsObject.fields.get("N") match {
      case Some(JsString(x)) => NumberAttributeValue(BigDecimal(x))
      case x => deserializationError("Expected { \"N\": \"number\" }, got " + x)
    }
  }

  implicit object StringFormat extends JsonFormat[StringAttributeValue] {
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

  implicit object BinaryFormat extends JsonFormat[BinaryAttributeValue] {
    def write(x: BinaryAttributeValue) = JsObject("B" -> JsString(base64Encode(x.value)))
    def read(value: JsValue) = value.asJsObject.fields.get("B") match {
      case Some(JsString(x)) => BinaryAttributeValue(base64Decode(x))
      case x => deserializationError("Expected { \"B\": \"base64Bytes\" }, got " + x)
    }
  }

  implicit object BooleanFormat extends JsonFormat[BooleanAttributeValue] {
    def write(x: BooleanAttributeValue) = JsObject("BOOL" -> JsBoolean(x.value))
    def read(value: JsValue) = value.asJsObject.fields.get("BOOL") match {
      case Some(JsBoolean(x)) => BooleanAttributeValue(x)
      case x => deserializationError("Expected { \"BOOL\": boolean }, got " + x)
    }
  }

  implicit object StringSetFormat extends JsonFormat[StringSetAttributeValue] {
    def write(x: StringSetAttributeValue) = JsObject("SS" -> JsArray(x.value.toVector.map(JsString.apply)))
    def read(value: JsValue) = value.asJsObject.fields.get("SS") match {
      case Some(JsArray(elems)) => StringSetAttributeValue(elems.collect { case JsString(s) => s }.toSet)
      case x => deserializationError("Expected { \"SS\": [ \"string\", ... ] }, got " + x)
    }
  }

  implicit object BinarySetFormat extends JsonFormat[BinarySetAttributeValue] {
    def write(x: BinarySetAttributeValue) = {
      val base64Strings = x.value.toVector.map(byteStr => JsString(base64Encode(byteStr)))
      JsObject("BS" -> JsArray(base64Strings))
    }
    def read(value: JsValue) = value.asJsObject.fields.get("BS") match {
      case Some(JsArray(elems)) => BinarySetAttributeValue(elems.collect { case JsString(s) => base64Decode(s) }.toSet)
      case x => deserializationError("Expected { \"BS\": [ \"string\", ... ] }, got " + x)
    }
  }

  implicit object NumberSetFormat extends JsonFormat[NumberSetAttributeValue] {
    def write(x: NumberSetAttributeValue) = JsObject("NS" -> JsArray(x.value.toVector.map(num => JsString(num.toString))))
    def read(value: JsValue) = value.asJsObject.fields.get("NS") match {
      case Some(JsArray(elems)) => NumberSetAttributeValue(elems.collect { case JsString(s) => BigDecimal(s) }.toSet)
      case x => deserializationError("Expected { \"NS\": [ \"number\", ... ] }, got " + x)
    }
  }

  implicit object ListFormat extends JsonFormat[ListAttributeValue] {
    def write(x: ListAttributeValue) = JsObject("L" -> x.value.toJson)
    def read(value: JsValue) = value.asJsObject.fields.get("L") match {
      case Some(JsArray(elems)) => ListAttributeValue(elems.map(_.convertTo[AttributeValue]))
      case x => deserializationError("Expected { \"L\": [ attributeValue, ... ] }, got " + x)
    }
  }

  implicit object MapFormat extends JsonFormat[MapAttributeValue] {
    def write(x: MapAttributeValue) = JsObject("M" -> JsObject(x.value.mapValues(_.toJson)))
    def read(value: JsValue) = value.asJsObject.fields.get("M") match {
      case Some(JsObject(fields)) => MapAttributeValue(fields.mapValues(_.convertTo[AttributeValue]))
      case x => deserializationError("Expected { \"M\": { \"name\": attributeValue, ... } }, got " + x)
    }
  }

  implicit object NullFormat extends JsonFormat[NullAttributeValue.type ] {
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

  implicit val globalSecondaryIndexFormat = jsonFormat(GlobalSecondaryIndex.apply, "IndexName", "KeySchema", "Projection", "ProvisionedThroughput")

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

  implicit val streamSpecificationFormat = jsonFormat(StreamSpecification.apply, "StreamEnabled", "StreamViewType")

  // Actions

  implicit val createTableFormat = jsonFormat(CreateTable.apply, "AttributeDefinitions", "GlobalSecondaryIndexes", "KeySchema", "LocalSecondaryIndexes", "ProvisionedThroughput", "StreamSpecification", "TableName")

}

object DynamoDbSprayProtocol extends DynamoDbSprayProtocol

trait NullEmptyCollections extends CollectionFormats {
  override def viaSeq[I <: Iterable[T], T :JsonFormat](f: Seq[T] => I): RootJsonFormat[I] = {
    def superFormat = super.viaSeq(f)
    new RootJsonFormat[I] {
      def write(iterable: I) = if (iterable.isEmpty) JsNull else superFormat.write(iterable)
      def read(value: JsValue) = superFormat.read(value)
    }
  }
}
