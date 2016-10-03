import akka.http.scaladsl.model.headers.RawHeader

package object theon {

  def `X-Amz-Target`(target: String) = RawHeader("X-Amz-Target", target)
}
