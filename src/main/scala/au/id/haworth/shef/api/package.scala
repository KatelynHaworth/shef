package au.id.haworth.shef

import spray.http.HttpResponse
import spray.json.{DefaultJsonProtocol, JsArray, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat, RootJsonFormat}
import spray.json.lenses.JsonLenses._
import spray.json.DefaultJsonProtocol._

/**
  * Defines package-wide items for
  * the API package
  *
  * @author Liam Haworth
  */
package object api {

  /**
    * Defines a Node registered with the Chef Server
    *
    * @param name The name of the node
    * @param chef_environment Defines the Chef environment for this node
    * @param run_list Defines the run list for the node
    * @param automatic Defines the attributes automatically set by Ohia
    * @param normal Defines the attributes provided by users
    * @param default Defines the attributes provided as defaults in cookbooks
    * @param `override` Defines attributes that have the highest precedence
    */
  case class Node(
                   name:              String,
                   chef_environment:  Option[String]            = None,
                   run_list:          Option[List[String]]      = None,
                   automatic:         Option[Map[String, Any]]  = None,
                   normal:            Option[Map[String, Any]]  = None,
                   default:           Option[Map[String, Any]]  = None,
                   `override`:        Option[Map[String, Any]]  = None
                 )

  /**
    * Defines a Role registered with the Chef Server
    *
    * @param name Defines the name of the role
    * @param description Defines a short description of the role
    * @param run_list Defines the cookbooks/roles that make up this role
    * @param default_attributes Defines the default attributes for this role
    * @param override_attributes Defines the overriden attributes for this role
    */
  case class Role(
                   name:                String,
                   description:         String,
                   run_list:            List[String],
                   default_attributes:  Option[Map[String, Any]]  = None,
                   override_attributes: Option[Map[String, Any]]  = None
                 )

  /**
    * The Chef API JSON Protocol is used by
    * the client to decode JSON messages from
    * the API server into Scala objects and vice-versa
    */
  object ChefApiJSONProtocol extends DefaultJsonProtocol {

    /**
      * Defines a JSON Format that can handle the type "Any"
      *
      * Sourced from: https://groups.google.com/d/msg/spray-user/zZl_LbH8fN8/ITDQy3PkbQsJ
      */
    implicit object AnyJsonFormat extends JsonFormat[Any] {
      def write(x: Any) = x match {
        case n: Int => JsNumber(n)
        case s: String => JsString(s)
        case x: Seq[_] => seqFormat[Any].write(x)
        case m: Map[String, _] => mapFormat[String, Any].write(m)
        case b: Boolean if b => JsTrue
        case b: Boolean if !b => JsFalse
      }

      def read(value: JsValue) = value match {
        case JsNumber(n) => n.intValue()
        case JsString(s) => s
        case a: JsArray => listFormat[Any].read(value)
        case o: JsObject => mapFormat[String, Any].read(value)
        case JsTrue => true
        case JsFalse => false
        case JsNull => null
      }
    }

    /**
      * Defines the JSON Format for: <code>Node</code>
      */
    implicit val nodeJsonFormat: RootJsonFormat[Node] = jsonFormat7(Node)

    /**
      * Defines the JSON Format for: <code>Role</code>
      */
    implicit val roleJsonFormat: RootJsonFormat[Role] = jsonFormat5(Role)
  }

  /**
    * Used to define a Chef API endpoint provider
    */
  trait ChefAPI {

    /**
      * Defines the Chef Client that constructed the APi endpoint provider
      */
    protected[shef] val chefClient: ChefClient
  }

  /**
    * Represents errors returned by the Chef API on bad requests
    *
    * @param errorCode The HTTP status code
    * @param errorTitle The HTTP status code "reason"
    * @param errorMessage The error message returned from the API server
    */
  case class ChefAPIException(errorCode: Int, errorTitle: String, errorMessage: String) extends Exception(s"$errorTitle ($errorCode): $errorMessage")

  /**
    * Provides a helper object for generating errors
    * straight from the HttpResponse
    */
  object ChefAPIException {

    /**
      * Generates a ChefAPIException straight from a
      * HttpResponse by extracting the error message out
      * of the returned JSON and the HTTP status code
      *
      * @param res The HTTP response to build the exception from
      * @return Returns a fully formed ChefAPIException
      */
    def apply(res: HttpResponse): ChefAPIException =
      new ChefAPIException(
        res.status.intValue, res.status.reason,
        if (res.entity.nonEmpty) res.entity.asString.extract[List[String]](
          'error
        ).mkString("\n") else "NO ERROR MESSAGE RETURNED"
      )
  }
}
