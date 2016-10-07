package theon.json.spray

import spray.json._

import scala.collection.Iterable
import scala.collection.immutable.Seq

object SprayJsonHacks {

  trait DoNotRenderEmptyCollections extends ProductFormats with CollectionFormats with StandardFormats with AdditionalFormats {

    override protected def fromField[T](value: JsValue, fieldName: String)
                                       (implicit reader: JsonReader[T]) = value match {
      case x: JsObject if reader.isInstanceOf[CollectionFormat[_]] & !x.fields.contains(fieldName) =>
        reader.asInstanceOf[CollectionFormat[T]].emptyCollection
      case _ =>
        super.fromField(value, fieldName)(reader)
    }

    override protected def productElement2Field[T](fieldName: String, p: Product, ix: Int, rest: List[JsField] = Nil)
                                                  (implicit writer: JsonWriter[T]): List[JsField] = {
      val value = p.productElement(ix).asInstanceOf[T]
      writer match {
        case cf: CollectionFormat[T] if cf.isEmpty(value) => rest
        case _ => super.productElement2Field(fieldName, p, ix, rest)(writer)
      }
    }

    override implicit def mapFormat[K :JsonFormat, V :JsonFormat] = new CollectionFormat[Map[K, V]](super.mapFormat, Map.empty, _.isEmpty) {
      override def write(map: Map[K,V]) = super.write(map).asJsObject // We need to cast to JsObject to make compiler happy about override
    }

    override def viaSeq[I <: Iterable[T], T :JsonFormat](f: Seq[T] => I): RootJsonFormat[I] =
      CollectionFormat(super.viaSeq(f), f(Seq.empty), _.isEmpty)

    case class CollectionFormat[T](realFormat: RootJsonFormat[T], emptyCollection: T, isEmpty: T => Boolean) extends RootJsonFormat[T] {
      def write(iterable: T) = realFormat.write(iterable)
      def read(value: JsValue) = realFormat.read(value)
    }
  }
}
