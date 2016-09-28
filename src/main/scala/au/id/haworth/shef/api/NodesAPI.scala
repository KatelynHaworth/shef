package au.id.haworth.shef.api

import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import au.id.haworth.shef.api.ChefApiJSONProtocol._
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

  /**
    * Requests from the Chef API a list of nodes
    * registered to the organization
    *
    * @return A map of Name -> URL entries
    */
  def getNodeList: Future[Map[String, String]] = {
    val nodesPipeline: (HttpRequest) => Future[Map[String, String]] = {
      defaultPipeline ~> unmarshal[Map[String, String]]
    }

    nodesPipeline(Get(s"/organizations/${chefClient.organization}/nodes"))
  }

  /**
    * Requests detail informaton about a node from
    * the Chef API
    *
    * @param nodeName The unique name of the node
    * @return Returns a Node object with the details about the node
    */
  def getNode(nodeName: String): Future[Node] = {
    val nodePipeline: (HttpRequest) => Future[Node] = {
      defaultPipeline ~> unmarshal[Node]
    }

    nodePipeline(Get(s"/organizations/${chefClient.organization}/nodes/$nodeName"))
  }
}
