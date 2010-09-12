package org.specs2
package specification

trait After {
  def after: Any
  def apply[T <: Result](a: =>T) = {
	try { a	 } 
	finally { after }
  }  
}
