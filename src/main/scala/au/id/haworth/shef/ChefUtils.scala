package au.id.haworth.shef

import java.io.StringReader
import java.security.Security
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.signers.RSADigestSigner
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}

/**
 * Provides the basic helper methods required
 * by the library to perform request to a Chef API
 *
 * @author Liam Haworth
 */
object ChefUtils {

  /**
   * Builds a Cipher Private Key from a RSA PEM string
   *
   * @param pemString The string version of the RSA PEM file
   * @return CipherParameters
   */
  def privateKeyFromPEMString(pemString: String): CipherParameters = {
    val pemParser = new PEMParser(new StringReader(pemString))

    val keyPair = pemParser.readObject().asInstanceOf[PEMKeyPair]
    pemParser.close()

    PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo)
  }

  /**
   * Signs a string with a RSA private key
   *
   * @param s The string to encrypt
   * @param key The private key to sign the string with
   * @return String
   */
  def signString(s: String, key: CipherParameters): String = {
    Security.addProvider(new BouncyCastleProvider())

    val stringData = s.getBytes("UTF-8")

    val signer = new RSADigestSigner(new SHA1Digest())
    signer.init(true, key)
    signer.update(stringData, 0, stringData.length)

    val encodedSignature = Base64.encodeBase64(signer.generateSignature())

    new String(encodedSignature)
  }

  /**
   * Hashes a string with SHA1 then encodes it using base64
   * @param s The string to hash and encode
   * @return String
   */
  def encodeAndHashString(s: String) = new String(Base64.encodeBase64(
    DigestUtils.getSha1Digest.digest(s.getBytes("UTF-8"))
  ))

  /**
   * Formats a datetime stamp into a ISO-8601 compliant format
   *
   * @param date The date to format (Default to the current datetime stamp if not set)
   * @return String
   */
  def getISO8601TimeStamp(date: Date = new Date()): String = {
    val utc = TimeZone.getTimeZone("UTC")
    val isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    isoFormatter.setTimeZone(utc)

    isoFormatter.format(date)
  }

  /**
   * Builds a canonicalized path compacting multiple slashes and removing ending slash
   *
   * @param path The path to canonicalize
   * @return String
   */
  def canonicalPath(path: String): String = {
    val canonicalPath = path.replaceAll("\\/+", "/")

    if (canonicalPath.endsWith("/") && canonicalPath.length() > 1)
      canonicalPath.substring(0, path.length() - 1)
    else
      canonicalPath
  }

  /**
   * Builds an unsigned canonical header for a Chef API request used to authenticate the request
   * against the "requesting user". If the header matches (when signed) the request and can be verified with
   * the public key for the "requesting user" then the request is marked as "Authorized".
   *
   * <b>NOTE:</b> This header must be signed and encoded before being used as a header
   *
   * @param method The type of HTTP method used by the request
   * @param path The URI of the request
   * @param content The body/content of the request
   * @param client The client making the request
   * @param timestamp The timestamp of the request
   * @return String
   */
  def buildCanonicalHeader(method: String, path: String, content: String, client: String, timestamp: String): String = {
    val hashedPath = encodeAndHashString(path)
    val hashedContent = encodeAndHashString(content)

    s"Method:$method\n" +
    s"Hashed Path:$hashedPath\n" +
    s"X-Ops-Content-Hash:$hashedContent\n" +
    s"X-Ops-Timestamp:$timestamp\n" +
    s"X-Ops-UserId:$client"
  }

  import RequestMethod._

  /**
   * Builds a HttpRequestBase based on the desired request type
   *
   * @param requestMethod The type of request desired
   * @param requestUrl The URL the request is aimed for
   * @return HttpRequestBase
   */
  def getRequestBaseFromMethod(requestMethod: Method, requestUrl: String): HttpRequestBase = {
    var request: HttpRequestBase = null

    requestMethod match {
      case POST =>
        request = new HttpPost(requestUrl)

      case GET =>
        request = new HttpGet(requestUrl)

      case PUT =>
        request = new HttpPut(requestUrl)

      case DELETE =>
        request = new HttpDelete(requestUrl)
    }

    request.addHeader("User-Agent", "Shef/1.0 Java Scala")

    request
  }

  /**
   * Builds a HTTP request to be sent to a Chef Server for a API call
   *
   * @param chefServer Chef Server to send the request to
   * @param requestMethod HTTP method to use in the request
   * @param requestPath Chef API endpoint to send request to
   * @param requestContent JSON String for POST or PUT requests
   * @return HttpRequestBase
   */
  def buildRequest(chefServer: ChefServer, requestMethod: Method, requestPath: String, requestContent: String): HttpRequestBase = {
    val endpoint = s"/organizations/${chefServer.organization}/$requestPath"
    val url = s"${chefServer.schema}://${chefServer.server}:${chefServer.port}$endpoint"

    var request = getRequestBaseFromMethod(requestMethod, url)
    val requestTimestamp = getISO8601TimeStamp()

    val authorizationHeader = buildCanonicalHeader(
      requestMethod.toString,
      endpoint,
      requestContent,
      chefServer.client,
      requestTimestamp
    )

    println(authorizationHeader)

    val signedHeader = signString(authorizationHeader, privateKeyFromPEMString(chefServer.clientKey)).grouped(60).toList

    request.addHeader("X-Ops-UserId",           chefServer.client)
    request.addHeader("X-Chef-Version",         "0.10.4")
    request.addHeader("X-Ops-Sign",             "algorithm=sha1;version=1.0")
    request.addHeader("X-Ops-Timestamp",        requestTimestamp)
    request.addHeader("X-Ops-Content-Hash",     encodeAndHashString(requestContent))
    request.addHeader("Accept",                 "application/json")

    if(requestMethod == POST || requestMethod == PUT) {
      val entityRequest = request.asInstanceOf[HttpEntityEnclosingRequestBase]

      val requestBody = new StringEntity(requestContent)
      requestBody.setContentType("application/json")

      entityRequest.setEntity(requestBody)
      request = entityRequest
    }

    for(segment <- 0 until signedHeader.length) {
      request.addHeader(s"X-Ops-Authorization-${segment + 1}", signedHeader(segment))
    }

    request
  }

  /**
   * Builds a API request then sends it to the Chef API server and processes the output
   *
   * @param chefServer Chef Server to send the request to
   * @param requestMethod HTTP method to use in the request
   * @param requestPath Chef API endpoint to send request to
   * @param requestContent JSON String for POST or PUT requests
   * @return
   */
  def sendRequestToServer(chefServer: ChefServer, requestMethod: Method, requestPath: String, requestContent: String) = {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog")
    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true")
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG")

    val httpClient = HttpClientBuilder.create().build()
    val builtRequest = buildRequest(chefServer, requestMethod, requestPath, requestContent)

    val requestResponse = httpClient.execute(builtRequest)
  }
}

/**
 * Defines a set of available HTTP methods
 * that can be used in Chef requests
 */
object RequestMethod extends Enumeration {

  /**
   * Defines a type that can be used when referencing a RequestMethod
   */
  type Method = Value

  /**
   * Defines a POST request. Used to CREATE objects on the server
   */
  val POST = Value("POST")

  /**
   * Defines a GET request. Used to READ objects on the server
   */
  val GET = Value("GET")

  /**
   * Defines a PUT request. Used to UPDATE objects on the server
   */
  val PUT = Value("PUT")

  /**
   * Defines a DELETE request. Used to DELETE objects on the server
   */
  val DELETE = Value("DELETE")
}

/**
 * Defines a Chef Server that can receive API requests
 *
 * @param server The Chef API Server's FQDN
 * @param port The port on which the API server is running (Defaults to 443)
 * @param schema The HTTP schema to use (Defaults to https)
 * @param organization The organization the requests should be for
 * @param client The client to connect as
 * @param clientKey The client's RSA private key used to sign requests
 */
case class ChefServer(server: String, port: Int = 443, schema: String = "https", organization: String, client: String, clientKey: String)
