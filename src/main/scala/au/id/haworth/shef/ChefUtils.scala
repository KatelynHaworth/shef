package au.id.haworth.shef

import java.io.StringReader
import java.security.{Signature, PrivateKey, Security}
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}

/**
 * Provides the basic helper methods required
 * by the library to perform requests to a Chef API
 *
 * @author Liam Haworth
 */
object ChefUtils {

  /**
    * Builds a Cipher Private Key from a RSA PEM string
    *
    * @param pemString The string version of the RSA PEM file
    * @return PrivateKey
    */
  protected[shef] def privateKeyFromPEMString(pemString: String): PrivateKey = {
    val pemParser = new PEMParser(new StringReader(pemString))

    val keyPair = pemParser.readObject().asInstanceOf[PEMKeyPair]
    pemParser.close()

    new JcaPEMKeyConverter().getPrivateKey(keyPair.getPrivateKeyInfo)
  }

  /**
    * Signs a string with a RSA private key
    *
    * @param s   The string to encrypt
    * @param key The private key to sign the string with
    * @return String
    */
  protected[shef] def signString(s: String, key: PrivateKey): String = {
    Security.addProvider(new BouncyCastleProvider())

    val stringData = s.getBytes("UTF-8")

    val signer = Signature.getInstance("RSA", "BC")
    signer.initSign(key)
    signer.update(stringData, 0, stringData.length)

    val encodedSignature = Base64.encodeBase64(
      signer.sign()
    )

    new String(encodedSignature)
  }

  /**
    * Hashes a string with SHA1 then encodes it using base64
    *
    * @param s The string to hash and encode
    * @return String
    */
  protected[shef] def encodeAndHashString(s: String) = new String(Base64.encodeBase64(
    DigestUtils.getSha1Digest.digest(s.getBytes("UTF-8"))
  ))

  /**
    * Formats a datetime stamp into a ISO-8601 compliant format
    *
    * @param date The date to format (Default to the current datetime stamp if not set)
    * @return String
    */
  protected[shef] def getISO8601TimeStamp(date: Date = new Date()): String = {
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
  protected[shef] def canonicalPath(path: String): String = {
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
    * @param method    The type of HTTP method used by the request
    * @param path      The URI of the request
    * @param content   The body/content of the request
    * @param client    The client making the request
    * @param timestamp The timestamp of the request
    * @return String
    */
  protected[shef] def buildCanonicalHeader(method: String, path: String, content: String, client: String, timestamp: String): String = {
    val hashedPath = encodeAndHashString(path)
    val hashedContent = encodeAndHashString(content)

    s"Method:$method\n" +
    s"Hashed Path:$hashedPath\n" +
    s"X-Ops-Content-Hash:$hashedContent\n" +
    s"X-Ops-Timestamp:$timestamp\n" +
    s"X-Ops-UserId:$client"
  }
}