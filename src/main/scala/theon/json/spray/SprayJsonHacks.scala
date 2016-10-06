package theon.json.spray

import spray.json.{AdditionalFormats, CollectionFormats, JsNull, JsObject, JsValue, JsonFormat, JsonReader, ProductFormats, RootJsonFormat, StandardFormats}

import scala.collection.Iterable
import scala.collection.immutable.Seq

object SprayJsonHacks {
  trait NullEmptyCollections extends CollectionFormats {
    override def viaSeq[I <: Iterable[T], T :JsonFormat](f: Seq[T] => I): RootJsonFormat[I] = {
      def superFormat = super.viaSeq(f)
      new RootJsonFormat[I] {
        def write(iterable: I) = if (iterable.isEmpty) JsNull else superFormat.write(iterable)
        def read(value: JsValue) = if(value == JsNull) f(Seq.empty) else superFormat.read(value)
      }
    }
  }

  trait MissingCollectionsEmpty extends ProductFormats with CollectionFormats with StandardFormats with AdditionalFormats {

    override protected def fromField[T](value: JsValue, fieldName: String)
                                       (implicit reader: JsonReader[T]) = value match {
      case x: JsObject if
      (reader.isInstanceOf[CollectionFormat[_]] &
              !x.fields.contains(fieldName)) =>
        reader.asInstanceOf[CollectionFormat[T]].emptyCollection
      case _ =>
        super.fromField(value, fieldName)(reader)
    }

    override def viaSeq[I <: Iterable[T], T :JsonFormat](f: Seq[T] => I): RootJsonFormat[I] =
      new CollectionFormat(super.viaSeq(f), f(Seq.empty))

    class CollectionFormat[T](realFormat: RootJsonFormat[T], val emptyCollection: T) extends RootJsonFormat[T] {
      def write(iterable: T) = realFormat.write(iterable)
      def read(value: JsValue) = realFormat.read(value)
    }
  }
}
