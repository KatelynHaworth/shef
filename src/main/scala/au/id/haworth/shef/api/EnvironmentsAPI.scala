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
protected[shef] trait EnvironmentsAPI extends ChefAPI {

  import chefClient.{ec, defaultPipeline, organizationName => org}

  /**
    * Retrieves a list of environments from the Chef server
    *
    * @return A list of Environment Name -> Resource URL pairs
    */
  def getEnvironmentsList: Future[Map[String, String]] = {
    val getEnvironmentsListPipeline: (HttpRequest) => Future[Map[String, String]] =
      defaultPipeline ~> unmarshal[Map[String, String]]

    getEnvironmentsListPipeline(Get(s"/organizations/$org/environments"))
  }

  /**
    * Informs the Chef Server of a new environment definition
    *
    * @param env The Environment with the settings populated
    * @return Returns the newly defined Environment
    */
  def createEnvironment(env: Environment): Future[HttpResponse] = defaultPipeline(Post(s"/organizations/$org/environments", env))

  /**
    * Retrieves a environment definition from the Chef server
    *
    * @param name The name of the environment
    * @return Returns a Environment object
    */
  def getEnvironment(name: String): Future[Environment] = {
    val getEnvironmentPipeline: (HttpRequest) => Future[Environment] =
      defaultPipeline ~> unmarshal[Environment]

    getEnvironmentPipeline(Get(s"/organizations/$org/environments/$name"))
  }

  /**
    * Informs the Chef server of changes to a environment definition
    *
    * @param name The name of the environment
    * @param env The environment with the changes populated in it
    * @return Returns a updated Environment object
    */
  def updateEnvironment(name: String, env: Environment): Future[Environment] = {
    val updateEnvironmentPipeline: (HttpRequest) => Future[Environment] =
      defaultPipeline ~> unmarshal[Environment]

    updateEnvironmentPipeline(Put(s"/organizations/$org/environments/$name", env))
  }

  /**
    * Informs the Chef server that is should delete a environment definition
    *
    * @param name The name of the environment
    * @return Returns an Environment object with the last known settings of the definition
    */
  def deleteEnvironment(name: String): Future[Environment] = {
    val deleteEnvironmentPipeline: (HttpRequest) => Future[Environment] =
      defaultPipeline ~> unmarshal[Environment]

    deleteEnvironmentPipeline(Delete(s"/organizations/$org/environments/$name"))
  }
}
