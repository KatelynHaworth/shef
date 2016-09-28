package au.id.haworth.shef

import spray.http.HttpResponse
import spray.json.lenses.JsonLenses._
import spray.json.DefaultJsonProtocol._

/**
  * Defines package-wide items for
  * the API package
  */
package object api {

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
