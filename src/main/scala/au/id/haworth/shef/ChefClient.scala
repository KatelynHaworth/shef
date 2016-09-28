package au.id.haworth.shef

import java.security.PrivateKey

import akka.actor.ActorSystem
import au.id.haworth.shef.api.NodesAPI
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http._

/**
  * Provides an easier way to build a ChefClient
  *
  * @author Liam Haworth
  */
object ChefClient {

  /**
    * Builds a new ChefClient
    *
    * @param host The FQDN of the Chef Server
    * @param organization The Chef organization name
    * @param client The Chef Client name
    * @param clientKey The private key for the client
    * @return The new ChefClient
    */
  def apply(host: String, organization: String, client: String, clientKey: String) (implicit system: ActorSystem): ChefClient =
    apply(host, organization, client, clientKey, 443, "https")(system)

  /**
    * Builds a new ChefClient
    *
    * @param host The FQDN of the Chef Server
    * @param organization The Chef organization name
    * @param client The Chef Client name
    * @param clientKey The private key for the client
    * @param port The port the API is running on (Default: 443)
    * @param schema The schema the API will respond to (HTTP or HTTPS, Default: HTTPS)
    * @return The new ChefClient
    */
  def apply(host: String, organization: String, client: String, clientKey: String, port: Int, schema: String) (implicit system: ActorSystem): ChefClient = {
    new ChefClient(host, organization, client, ChefUtils.privateKeyFromPEMString(clientKey), port, schema)(system)
  }
}

/**
  * The ChefClient is a library for
  * developers to have an easy way
  * to talk to the Chef Server API
  *
  * @param host The FQDN of the Chef Server
  * @param organization The Chef organization name
  * @param client The Chef Client name
  * @param clientKey The private key for the client
  * @param port The port the API is running on (Default: 443)
  * @param schema The schema the API will respond to (HTTP or HTTPS, Default: HTTPS)
  * @author Liam Haworth
  */
class ChefClient(host: String, val organization: String, client: String, clientKey: PrivateKey, port: Int, schema: String) (implicit val system: ActorSystem)
      extends NodesAPI {

  /**
    * Provides this Chef API client to the API endpoint providers
    */
  override protected[shef] val chefClient = this

  /**
    * Defines the execution context for the Chef API client
    */
  protected[shef] implicit val ec = system.dispatcher

  /**
    * Defines the pipeline for adding the required headers to the HTTP request
    */
  protected[shef] val addChefHeaders: RequestTransformer = buildChefHeaders(_)

  /**
    * Defines the base for all requests
    */
  protected[shef] val connectionBase: String = s"$schema://$host:$port"

  /**
    * Generates the headers required to make a
    * successful API request to Chef
    *
    * @param req The request destined for the API server
    * @return The modified HttpRequest
    */
  private def buildChefHeaders(req: HttpRequest): HttpRequest = {
    val timetamp = ChefUtils.getISO8601TimeStamp()
    val authHeader = ChefUtils.buildCanonicalHeader(req.method.value, req.uri.path.toString(), req.entity.data.asString(HttpCharsets.`UTF-8`), client, timetamp)
    val authHeaderSegments = ChefUtils.signString(authHeader, clientKey).grouped(60).toList

    val headers = addHeader("X-Ops-UserId",           client) ~>
                  addHeader("X-Chef-Version",         "0.10.4") ~>
                  addHeader("X-Ops-Sign",             "algorithm=sha1;version=1.1") ~>
                  addHeader("X-Ops-Timestamp",        timetamp) ~>
                  addHeader("X-Ops-Content-Hash",     ChefUtils.encodeAndHashString(req.entity.data.asString(HttpCharsets.`UTF-8`))) ~>
                  addHeader("X-Ops-Server-API-Info",  "1") ~>
                  addHeaders(authHeaderSegments.indices.map( i =>
                    RawHeader(s"X-Ops-Authorization-${i+1}", authHeaderSegments(i))
                  ).toList)

    headers.apply(req)
  }
}
