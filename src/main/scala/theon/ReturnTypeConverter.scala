package theon

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

trait ReturnTypeConverter {
  type ReturnType[_]
  def convert[T](x: Source[T, NotUsed]): ReturnType[T]
}

trait SourceReturnType extends ReturnTypeConverter {
  type ReturnType[T] = Source[T, NotUsed]
  def convert[T](x: Source[T, NotUsed]): ReturnType[T] = x
}

trait FutureReturnType extends ReturnTypeConverter {
  type ReturnType[T] = Future[T]
  def materializer: ActorMaterializer
  def convert[T](x: Source[T, NotUsed]) = x.runWith(Sink.head)(materializer)
}

trait BlockingReturnType extends ReturnTypeConverter {
  import scala.concurrent.Await
  import scala.concurrent.duration._
  type ReturnType[T] = T

  // TODO: Log warning that this should only be used for testing!

  def materializer: ActorMaterializer
  def convert[T](x: Source[T, NotUsed]) = Await.result(x.runWith(Sink.head)(materializer), 5.seconds) // TODO: Get timeout from config
}
