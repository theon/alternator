package theon

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

  sealed trait KeyType
  object KeyType {
    case object HASH extends KeyType
    case object RANGE extends KeyType
  }

  case class KeySchemaElement(attributeName: String, keyType: KeyType)

  case class LocalSecondaryIndex(indexName: String, keySchema: Seq[KeySchemaElement], projection: Projection)

  sealed trait ProjectionType
  object ProjectionType {
    case object KEYS_ONLY extends ProjectionType
    case object INCLUDE extends ProjectionType
    case object ALL extends ProjectionType
  }

  case class Projection(nonKeyAttributes: Seq[String], projectionType: ProjectionType)

  case class ProvisionedThroughput(readCapacityUnits: Int, writeCapacityUnits: Int)

  sealed trait StreamViewType
  object StreamViewType {
    case object KEYS_ONLY extends StreamViewType
    case object NEW_IMAGE extends StreamViewType
    case object OLD_IMAGE extends StreamViewType
    case object NEW_AND_OLD_IMAGES extends StreamViewType
  }


  case class StreamSpecification(streamEnabled: Boolean, streamViewType: StreamViewType)
}
