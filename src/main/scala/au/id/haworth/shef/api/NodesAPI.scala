package au.id.haworth.shef.api

import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.http.HttpRequest

import scala.concurrent.Future

/**
  * Defines the API endpoint methods
  * relating to "Nodes" in the Chef API.
  *
  * @author Liam Haworth
  */
protected[shef] abstract class NodesAPI extends ChefAPI {

  import chefClient.{ec, defaultPipeline}

  def getNodeList: Future[Map[String, String]] = {
    val nodesPipeline: HttpRequest => Future[Map[String, String]] = {
      defaultPipeline ~>
      unmarshal[Map[String, String]]
    }

    nodesPipeline(Get(s"/organizations/${chefClient.organization}/nodes"))
  }
}
