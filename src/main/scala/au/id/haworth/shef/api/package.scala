package au.id.haworth.shef

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

}
