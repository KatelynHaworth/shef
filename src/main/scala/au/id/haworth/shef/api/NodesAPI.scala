package au.id.haworth.shef.api

import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import au.id.haworth.shef.api.ChefApiJSONProtocol._
import spray.http.{HttpRequest, HttpResponse}

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
    val nodesPipeline: (HttpRequest) => Future[Map[String, String]] =
      defaultPipeline ~> unmarshal[Map[String, String]]

    nodesPipeline(Get(s"/organizations/${chefClient.organization}/nodes"))
  }

  /**
    * Informs the Chef API of a new Node and its settings
    *
    * @param node The node to register with the Chef API server
    * @return Returns the name of the node and its Chef API URL
    */
  def createNode(node: Node): Future[HttpResponse] =
    defaultPipeline(Post(s"/organizations/${chefClient.organization}/nodes", node))

  /**
    * Requests detail informaton about a node from
    * the Chef API
    *
    * @param nodeName The unique name of the node
    * @return Returns a Node object with the details about the node
    */
  def getNode(nodeName: String): Future[Node] = {
    val nodePipeline: (HttpRequest) => Future[Node] =
      defaultPipeline ~> unmarshal[Node]

    nodePipeline(Get(s"/organizations/${chefClient.organization}/nodes/$nodeName"))
  }

  /**
    * Informs the Chef API of updates to a Nodes details
    *
    * @param name The name of the node
    * @param node The node object with the changes made
    * @return Returns a Node object with the changes implemented
    */
  def updateNode(name: String, node: Node): Future[Node] = {
    val updatePipeline: (HttpRequest) => Future[Node] =
      defaultPipeline ~> unmarshal[Node]

    updatePipeline(Put(s"/organizations/${chefClient.organization}/nodes/$name"))
  }

  /**
    * Informs the Chef API to unregister and delete the node
    *
    * @param name The name of the node
    * @return Returns a Node with the settings last known by the Chef API
    */
  def deleteNode(name: String): Future[Node] = {
    val deletePipeline: (HttpRequest) => Future[Node] =
      defaultPipeline ~> unmarshal[Node]

    deletePipeline(Delete(s"/organizations/${chefClient.organization}/nodes/$name"))
  }
}
