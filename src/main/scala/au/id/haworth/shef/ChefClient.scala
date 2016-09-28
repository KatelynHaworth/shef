package au.id.haworth.shef

import java.security.PrivateKey

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.can.Http.{HostConnectorInfo, HostConnectorSetup}
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http.{HttpRequest, _}
import au.id.haworth.shef.api.{ChefAPIException, NodesAPI, RolesAPI}

import scala.concurrent.Await
import scala.concurrent.duration._

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
    apply(host, organization, client, clientKey, 443, ssl = true)(system)

  /**
    * Builds a new ChefClient
    *
    * @param host The FQDN of the Chef Server
    * @param organization The Chef organization name
    * @param client The Chef Client name
    * @param clientKey The private key for the client
    * @param port The port the API is running on (Default: 443)
    * @param ssl Defines if requests to the API should be encrypt via SSL
    * @return The new ChefClient
    */
  def apply(host: String, organization: String, client: String, clientKey: String, port: Int, ssl: Boolean) (implicit system: ActorSystem): ChefClient = {
    new ChefClient(host, organization, client, ChefUtils.privateKeyFromPEMString(clientKey), port, ssl = ssl)(system)
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
  * @param ssl Defines if requests to the API should be encrypt via SSL
  * @author Liam Haworth
  */
class ChefClient(host: String, protected[shef] val organization: String, client: String, clientKey: PrivateKey, port: Int, ssl: Boolean) (protected[shef] implicit val system: ActorSystem)
      extends NodesAPI
      with    RolesAPI {

  /**
    * Defines the timeout for generating host connector information
    */
  private implicit val timeout: Timeout = Timeout(10.seconds)

  /**
    * Defines the execution context for the Chef API client
    */
  protected[shef] implicit val ec = system.dispatcher

  /**
    * Defines the host connector information for sending requests to the API server
    */
  private def getHostConnectorInfo: HostConnectorInfo = Await.result(
    IO(Http) ? HostConnectorSetup(host = host, port = port, sslEncryption = ssl),
    timeout.duration
  ).asInstanceOf[HostConnectorInfo]

  /**
    * Provides this Chef API client to the API endpoint providers
    */
  override protected[shef] val chefClient = this

  /**
    * Generates the headers required to make a
    * successful API request to Chef
    */
  private val addChefHeaders: (HttpRequest) => HttpRequest = (req: HttpRequest) => {
    val timetamp = ChefUtils.getISO8601TimeStamp()
    val authHeader = ChefUtils.buildCanonicalHeader(req.method.value, req.uri.path.toString(), req.entity.data.asString(HttpCharsets.`UTF-8`), client, timetamp)
    val authHeaderSegments = ChefUtils.signString(authHeader, clientKey).grouped(60).toList

    req ~> addHeader("X-Ops-UserId",           client) ~>
      addHeader("X-Chef-Version",         "0.10.4") ~>
      addHeader("X-Ops-Sign",             "algorithm=sha1;version=1.1") ~>
      addHeader("X-Ops-Timestamp",        timetamp) ~>
      addHeader("X-Ops-Content-Hash",     ChefUtils.encodeAndHashString(req.entity.data.asString(HttpCharsets.`UTF-8`))) ~>
      addHeader("X-Ops-Server-API-Info",  "1") ~>
      addHeaders(authHeaderSegments.indices.map( i =>
        RawHeader(s"X-Ops-Authorization-${i+1}", authHeaderSegments(i))
      ).toList)
  }

  /**
    * Defines a pipeline to handle errors sent back from the API server
    */
  protected[shef] val mapResponseErrors = (res: HttpResponse) => {
    if(res.status.isSuccess) res
    else throw ChefAPIException(res)
  }

  /**
    * Defines the default Spray pipeline used when making requests to the API
    */
  protected[shef] val defaultPipeline = {
    addChefHeaders ~>
    sendReceive(getHostConnectorInfo.hostConnector) ~>
    mapResponseErrors
  }
}
