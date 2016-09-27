package au.id.haworth.shef.api

import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.http.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/**
  * Defines the API endpoint methods
  * relating to "Nodes" in the Chef API.
  *
  * @author Liam Haworth
  */
protected[shef] abstract class NodesAPI extends ChefAPI {

  import chefClient.{connectionBase, addChefHeaders, system, ec}

  def getNodeList(): Future[Map[String, String]] = {
    val nodesPipeline: HttpRequest => Future[Map[String, String]] = {
        addChefHeaders ~>
        logRequest({ req =>
          println(req)
        }) ~>
        sendReceive ~>
        logResponse({ res =>
          println(res)
        }) ~>
        unmarshal[Map[String, String]]
    }

    nodesPipeline(Get(s"$connectionBase/organizations/${chefClient.organization}/nodes"))
  }
}
