package theon.json

import akka.util.ByteString
import theon.model._
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class SprayJsonSupportTest extends FlatSpec with Matchers {

  import DynamoDbSprayProtocol._

  "A Number Attribute Value" should "render to JSON" in {
    NumberAttributeValue(123.45).toJson.compactPrint should equal("""{"N":"123.45"}""")
  }

  it should "parse from JSON" in {
    """{"N":"123.45"}""".parseJson.convertTo[NumberAttributeValue] should equal(NumberAttributeValue(123.45))
    """{"N":"123.45"}""".parseJson.convertTo[AttributeValue]       should equal(NumberAttributeValue(123.45))
  }

  "A String Attribute Value" should "render to JSON" in {
    StringAttributeValue("hotdog").toJson.compactPrint should equal("""{"S":"hotdog"}""")
  }

  it should "parse from JSON" in {
    """{"S":"hotdog"}""".parseJson.convertTo[StringAttributeValue] should equal(StringAttributeValue("hotdog"))
    """{"S":"hotdog"}""".parseJson.convertTo[AttributeValue]       should equal(StringAttributeValue("hotdog"))
  }

  "A Binary Attribute Value" should "render to JSON" in {
    BinaryAttributeValue(ByteString("hotdog", "UTF-8")).toJson.compactPrint should equal("""{"B":"aG90ZG9n"}""")
  }

  it should "parse from JSON" in {
    """{"B":"aG90ZG9n"}""".parseJson.convertTo[BinaryAttributeValue] should equal(BinaryAttributeValue(ByteString("hotdog")))
    """{"B":"aG90ZG9n"}""".parseJson.convertTo[AttributeValue]       should equal(BinaryAttributeValue(ByteString("hotdog")))
  }

  "A Boolean Attribute Value" should "render to JSON" in {
    BooleanAttributeValue(true).toJson.compactPrint should equal("""{"BOOL":true}""")
  }

  it should "parse from JSON" in {
    """{"BOOL":true}""".parseJson.convertTo[BooleanAttributeValue] should equal(BooleanAttributeValue(true))
    """{"BOOL":true}""".parseJson.convertTo[AttributeValue]        should equal(BooleanAttributeValue(true))
  }

  "A String Set Attribute Value" should "render to JSON" in {
    StringSetAttributeValue(Set("hot", "dog")).toJson.compactPrint should equal("""{"SS":["hot","dog"]}""")
  }

  it should "parse from JSON" in {
    """{"SS":["hot","dog"]}""".parseJson.convertTo[StringSetAttributeValue] should equal(StringSetAttributeValue(Set("hot", "dog")))
    """{"SS":["hot","dog"]}""".parseJson.convertTo[AttributeValue]          should equal(StringSetAttributeValue(Set("hot", "dog")))
  }

  "A Binary Set Attribute Value" should "render to JSON" in {
    BinarySetAttributeValue(Set(ByteString("hot", "UTF-8"), ByteString("dog", "UTF-8"))).toJson.compactPrint should equal("""{"BS":["aG90","ZG9n"]}""")
  }

  it should "parse from JSON" in {
    """{"BS":["aG90","ZG9n"]}""".parseJson.convertTo[BinarySetAttributeValue] should equal(BinarySetAttributeValue(Set(ByteString("hot", "UTF-8"), ByteString("dog", "UTF-8"))))
    """{"BS":["aG90","ZG9n"]}""".parseJson.convertTo[AttributeValue]          should equal(BinarySetAttributeValue(Set(ByteString("hot", "UTF-8"), ByteString("dog", "UTF-8"))))
  }

  "A Number Set Attribute Value" should "render to JSON" in {
    NumberSetAttributeValue(Set(1, 2, 3)).toJson.compactPrint should equal("""{"NS":["1","2","3"]}""")
  }

  it should "parse from JSON" in {
    """{"NS":["1","2","3"]}""".parseJson.convertTo[NumberSetAttributeValue] should equal(NumberSetAttributeValue(Set(1, 2, 3)))
    """{"NS":["1","2","3"]}""".parseJson.convertTo[AttributeValue]          should equal(NumberSetAttributeValue(Set(1, 2, 3)))
  }

  "A List Attribute Value" should "render to JSON" in {
    ListAttributeValue(List(StringAttributeValue("hot"), StringAttributeValue("dog"))).toJson.compactPrint should equal("""{"L":[{"S":"hot"},{"S":"dog"}]}""")
  }

  it should "parse from JSON" in {
    """{"L":[{"S":"hot"},{"S":"dog"}]}""".parseJson.convertTo[ListAttributeValue] should equal(ListAttributeValue(List(StringAttributeValue("hot"), StringAttributeValue("dog"))))
    """{"L":[{"S":"hot"},{"S":"dog"}]}""".parseJson.convertTo[AttributeValue]     should equal(ListAttributeValue(List(StringAttributeValue("hot"), StringAttributeValue("dog"))))
  }

  "A Map Attribute Value" should "render to JSON" in {
    MapAttributeValue(Map("hotdogs" -> NumberAttributeValue(1), "burgers" -> NumberAttributeValue(2))).toJson.compactPrint should equal("""{"M":{"hotdogs":{"N":"1"},"burgers":{"N":"2"}}}""")
  }

  it should "parse from JSON" in {
    """{"M":{"hotdogs":{"N":"1"},"burgers":{"N":"2"}}}""".parseJson.convertTo[MapAttributeValue] should equal(MapAttributeValue(Map("hotdogs" -> NumberAttributeValue(1), "burgers" -> NumberAttributeValue(2))))
    """{"M":{"hotdogs":{"N":"1"},"burgers":{"N":"2"}}}""".parseJson.convertTo[AttributeValue]    should equal(MapAttributeValue(Map("hotdogs" -> NumberAttributeValue(1), "burgers" -> NumberAttributeValue(2))))
  }
}
