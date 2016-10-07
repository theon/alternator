import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.headers.RawHeader

package object theon {
  def `X-Amz-Target`(target: String) = RawHeader("X-Amz-Target", target)
  val `application/x-aws-json-1.0` = MediaType.applicationWithOpenCharset("x-amz-json-1.0")
}
