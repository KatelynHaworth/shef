package au.id.haworth.shef.api

import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import au.id.haworth.shef.api.ChefApiJSONProtocol._
import spray.http.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/**
  * Defines the API endpoint methods
  * relating to "Roles" in the Chef API.
  *
  * @author Liam Haworth
  */
protected[shef] trait RolesAPI extends ChefAPI {

  import chefClient.{ec, defaultPipeline, organizationName => org}

  /**
    * Requests a list of roles from the Chef API
    *
    * @return A map of Name -> URL entries
    */
  def getRolesList: Future[Map[String, String]] = {
    val rolesPipeline: (HttpRequest) => Future[Map[String, String]] =
      defaultPipeline ~> unmarshal[Map[String, String]]

    rolesPipeline(Get(s"/organizations/$org/roles"))
  }

  /**
    * Informs the Chef API of a new role
    *
    * @param role The role and its settings to send to the API
    * @return Returns response from the server
    */
  def createRole(role: Role): Future[HttpResponse] = defaultPipeline(Post(s"/organizations/$org/roles"))

  /**
    * Requests information about a role from the Chef API
    *
    * @param name The name of the role
    * @return Returns a Role object populated with the requested information
    */
  def getRole(name: String): Future[Role] = {
    val getRolePipeline: (HttpRequest) => Future[Role] =
      defaultPipeline ~> unmarshal[Role]

    getRolePipeline(Get(s"/organizations/$org/roles/$name"))
  }

  /**
    * Informs the Chef API server to update its information
    * regarding a role
    *
    * @param name The name of the role
    * @param role The Role object with the new information populated
    * @return Returns the newly updated Role object
    */
  def updateRole(name: String, role: Role): Future[Role] = {
    val updateRolePipeline: (HttpRequest) => Future[Role] =
      defaultPipeline ~> unmarshal[Role]

    updateRolePipeline(Put(s"/organizations/$org/roles/$name", role))
  }

  /**
    * Informs the Chef API server to delete a role
    *
    * @param name The name of the role
    * @return Returns a Role object with the last known information populated
    */
  def deleteRole(name: String): Future[Role] = {
    val deleteRolePipeline: (HttpRequest) => Future[Role] =
      defaultPipeline ~> unmarshal[Role]

    deleteRolePipeline(Delete(s"/organizations/$org/roles/$name"))
  }
}
