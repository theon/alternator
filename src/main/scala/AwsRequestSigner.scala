import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.headers.{Authorization, Date, GenericHttpCredentials, RawHeader}
import akka.http.scaladsl.model.{DateTime, HttpCharsets, HttpRequest}

trait AwsRequestSigner {
  def sign(request: HttpRequest): HttpRequest
}

class SignatureV4Signer(accessKeyId: String, secretAccessKey: String) extends AwsRequestSigner {
  def sign(r: HttpRequest): HttpRequest = {
    val now = DateTime(currentTimeMillis)
    val auth = authHeader(r, now)

    val addHeaders =
      Date(now) +:
      auth +:
      r.headers

    r.copy(headers = addHeaders ++ r.headers)
  }

  val paramsToSign = Set(
    "acl", "lifecycle", "location", "logging", "notification", "partNumber", "policy", "requestPayment",
    "torrent", "uploadId", "uploads", "versionId", "versioning", "versions", "website", "response-content-type",
    "response-content-language", "response-expires", "response-cache-control", "response-content-disposition",
    "response-content-encoding", "delete")

  private def authHeader(r: HttpRequest, date: DateTime): Authorization = {
    val content = r.entity match {
      case Strict(ct, data) =>
        val charset = ct.charsetOption.getOrElse(HttpCharsets.`UTF-8`)
        data.utf8String.getBytes(charset.value)
      case _ => Array.emptyByteArray
    }

    val signedHeaders = r.headers.sortBy(_.name()).map(_.lowercaseName()).mkString(";")

    val canonicalRequest =
      r.method + "\n" +
      r.uri.path + "\n" +
      r.uri.query().sortBy(_._1).toString().replaceAll("&", "&\n") + "\n" + // TODO: Make better
      r.headers.sortBy(_.name()).map(h => h.lowercaseName() + ":" + h.value()).mkString("\n") + "\n" +
      signedHeaders + "\n" +
      hex(sha256(content))

    val dateString = date.toIsoDateString().replace("-", "") // TODO: Fix replace
    val credentialScope = dateString + "/eu-west-1/dynamodb/aws4_request" // TODO: Fix hardcoded region+service

    val stringToSign =
      "AWS4-HMAC-SHA256\n" +
      date.toIsoDateTimeString().replace("-", "").replace(":", "") + "\n" + // TODO: Fix replace
      credentialScope + "\n" +
      hex(sha256(canonicalRequest.getBytes("UTF-8")))

    val kDate = hmacSha256(dateString, ("AWS4" + secretAccessKey).getBytes("UTF-8"))
    val kRegion = hmacSha256("eu-west-1", kDate)
    val kService = hmacSha256("dynamodb", kRegion)
    val kSigning = hmacSha256("aws4_request", kService)

    val signature = hex(hmacSha256(stringToSign, kSigning))

    Authorization(GenericHttpCredentials("AWS4-HMAC-SHA256", s"Credential=$accessKeyId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"))
  }

  private def hex(bytes: Array[Byte]): String = {
    bytes.map("%02X" format _).mkString
  }

  private def sha256(bytes: Array[Byte]): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA-256")
    md.digest(bytes)
  }

  private def hmacSha256(str: String, secret: Array[Byte]): Array[Byte] = hmacSha256(str.getBytes("UTF-8"), secret)

  private def hmacSha256(bytes: Array[Byte], secret: Array[Byte]): Array[Byte] = {
    val signingKey = new SecretKeySpec(secret, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)
    mac.doFinal(bytes)
  }

  def currentTimeMillis = System.currentTimeMillis

}
