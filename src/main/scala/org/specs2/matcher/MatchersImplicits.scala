package org.specs2
package matcher

import control.Exceptions._
import execute._
import Expectable._
import scalaz._, Scalaz._
import Generator._
import text.Quote._
import text.Plural._
import MatchResultMessages._

/**
 * This trait provides implicit definitions from MatchResults and Booleans to Results.
 *
 * It also allows to:
 *
 * * create matchers from functions
 * * create matchers for seqs and sets from single matchers
 */
trait MatchersImplicits extends Expectations {
  /** 
   * implicit definition to transform a Seq of MatchResults to a Result
   */ 
  implicit def seqToResult[T](r: Seq[MatchResult[T]]): Result = r.reduceLeft(_ and _).toResult
  /** 
   * implicit definition to transform any MatchResult to a Result
   */ 
  implicit def asResult[T](r: MatchResult[T]): Result = r.toResult
  
  /** 
   * implicit definition to accept any boolean value as a Result
   * This avoids writing b must beTrue 
   */ 
  implicit def toResult(b: Boolean): Result = {
    new BeTrueMatcher().apply(createExpectable(b)).toResult
  }

  /**
   * Add functionalities to functions returning matchers so that they can be combined before taking a value and
   * returning actual matchers
   */
  implicit def matcherFunction[S, T](f: S => Matcher[T]) = new MatcherFunction(f)
  implicit def matcherFunction2[T](f: T => Matcher[T]) = new MatcherFunction2(f)

  class MatcherFunction[S, T](f: S => Matcher[T]) {
  
    /**
     * @return a function which will return a matcher checking a sequence of objects
     */
    def toSeq = new Function1[Seq[S], Matcher[Seq[T]]] {
      def apply(s: Seq[S]) = new SeqMatcher(s, f)
    }

    /**
     * @return a function which will return a matcher checking a set of objects
     */
    def toSet = new Function1[Set[S], Matcher[Set[T]]] {
      def apply(s: Set[S]) = new SetMatcher(s, f)
    }
  }
  class MatcherFunction2[T](f: T => Matcher[T]) {
    /**
     * @return a function which will return the composition of a matcher and a function
     */
    def ^^^[A](g: A => T) = (a: A) => 
      new Matcher[A] {
        def apply[B <: A](b: Expectable[B]) = {
          val r = f(g(a)).apply(b.map(g))
          result(r, b)
        }
      }
  }

  /**
   * The <code>SeqMatcher</code> class is a matcher matching a sequence of objects with a matcher returned by a function.<p>
   * Usage:<code>List(1, 2, 3) must ((beEqualTo(_:Int)).toSeq)(List(1, 2, 3)) </code>
   */
  class SeqMatcher[S, T](s: Seq[S], f: S => Matcher[T]) extends Matcher[Seq[T]] {
    def apply[U <: Seq[T]](t: Expectable[U]) = {
      val bothSequences = t.value.toList zip s.toList
      val results = bothSequences.map { case (t1, s1) => f(s1).apply(createExpectable(t1)) }
      if (s.size != t.value.size)
        result(false,
               "the seqs contain the same number of elements",
                t.description + " contains " + t.value.size + " elements while " + q(s) + " contains " + s.size + " elements",
                t)
      else
        result(FoldrGenerator[List].reduce(MatchResultMessageReducer[T], results), t)
    }
  }

  /**
   * The <code>SetMatcher</code> class is a matcher matching a set of objects with a matcher returned by a function.<p>
   * Usage:<code>List(1, 2, 3) must ((beEqualTo(_:Int)).toSet)(List(2, 1, 3)) </code>
   */
  class SetMatcher[S, T](s: Set[S], f: S => Matcher[T]) extends Matcher[Set[T]] {
    def apply[U <: Set[T]](t: Expectable[U]) = {
      val setToTest = t
      if (s.size != setToTest.value.size)
        result(false, 
               "the sets contain the same number of elements",
                setToTest.description + " contains " + setToTest.value.size + " elements while " + q(s) + " contains " + s.size + " elements",
                setToTest)
      else {
        val results = setToTest.value.map { (element: T) =>
          s.find { (otherElement:S) => f(otherElement).apply(createExpectable(element)).isSuccess } match {
            case None => result(false, "all matches", "no match for element " + q(element), setToTest)
            case Some(x) => result(true, q(element) + " matches with " + x, "no match for element " + q(element), setToTest)
          }
        }
        result(FoldrGenerator[Set].reduce(MatchResultMessageReducer[U], results), setToTest)
      }
    }
  }

  /**
   * This method transform a function to a Matcher
   */
  implicit def functionToMatcher[T](f: (T => Boolean, String)): Matcher[T] =
    functionAndMessagesToMatcher[T]((f._1, (t:T) => "not ("+q(t)+" "+f._2+")", (t:T) => q(t)+" "+f._2))
  /**
   * This method transform a function to a Matcher
   */
  implicit def functionToMatcher2[T](f: (T => Boolean, String, String)): Matcher[T] =
    functionAndMessagesToMatcher[T]((f._1, (t:T) => q(t)+" "+f._2, (t:T) => q(t)+" "+f._3))
  /**
   * This method transform a function to a Matcher
   */
  implicit def functionAndKoMessageToMatcher[T](f: (T => Boolean, T => String)): Matcher[T] =
    functionAndMessagesToMatcher[T]((f._1, (t:T) => "not ("+f._2(t)+")", f._2))
  /**
   * This method transform a function, with function descriptors to a Matcher
   */
  implicit def functionAndMessagesToMatcher[T](f: (T => Boolean, T => String, T => String)): Matcher[T] = new Matcher[T] {
    def apply[S <: T](s: Expectable[S]) = {
      val expectable = s
      val functionResult = f._1(expectable.value)
      result(functionResult,  f._2(s.value), f._3(s.value), expectable)
    }
  }

  implicit def verifyFunction[U, T](t: U => MatchResult[T]) = new MatchResultFunctionVerification(t)
  class MatchResultFunctionVerification[U, T](t: U => MatchResult[T]) {
    def forall[S <: Seq[U]](seq: S) = {
      if (seq.isEmpty)
        Matcher.result(true, "ok", "ko", createExpectable(seq))
      else {
        val r = seq.drop(1).foldLeft(t.apply(seq.head)) { (res, cur) =>
          if (res.isSuccess) t.apply(cur)
          else res
        }
        Matcher.result(r.isSuccess,
               "All elements of "+q(seq.mkString(", "))+" are matching ok",
               "In the sequence "+q(seq.mkString(", "))+", the "+(seq.indexOf(r.expectable.value)+1).th+" element is failing: "+r.message,
               createExpectable(seq))
      }
    }

    def foreach[S <: Seq[U]](seq: S) = {
      if (seq.isEmpty)
        Matcher.result(true, "ok", "ko", createExpectable(seq))
      else {
        val r = seq.drop(1).foldLeft(t.apply(seq.head).toResult) { (res, cur) =>
          res |+| t.apply(cur).toResult
        }
        Matcher.result(r.isSuccess,
                       "All elements of "+q(seq.mkString(", "))+" are matching ok",
                       r.message,
                       createExpectable(seq))

      }
    }

    def atLeastOnce[S <: Seq[U]](seq: S) = {
      if (seq.isEmpty)
        Matcher.result(false, "ok", "ko", createExpectable(seq))
      else {
        val r = seq.drop(1).foldLeft(t.apply(seq.head)) { (res, cur) =>
          if (res.isSuccess) res
          else t.apply(cur)
        }
        Matcher.result(r.isSuccess,
          "In the sequence "+q(seq.mkString(", "))+", the "+(seq.indexOf(r.expectable.value)+1).th+" element is matching: "+r.message,
          "No element of "+q(seq.mkString(", "))+" is matching ok",
          createExpectable(seq))
      }
    }
  }
}
private[specs2]
object MatchersImplicits extends MatchersImplicits
