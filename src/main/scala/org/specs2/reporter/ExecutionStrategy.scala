package org.specs2
package reporter

import java.util.concurrent.Executors
import scalaz._
import Scalaz._
import concurrent._
import Strategy.Executor
import main.Arguments
import specification._
import control.NamedThreadFactory

/**
 * Generic trait for executing Fragments, which are sorted according to their dependencies
 */
private[specs2]
trait ExecutionStrategy {
  def execute(implicit arguments: Arguments): Seq[FragmentSeq] => Seq[ExecutedFragment]
}

/**
 * This trait uses Scalaz promises to execute Fragments concurrently
 * 
 * It uses a Fixed thread pool with a number of threads to execute the fragments.
 * The default number is 4 but this can be overriden by providing different Arguments
 */
private[specs2]
trait DefaultExecutionStrategy extends ExecutionStrategy with FragmentExecution {
  
  def execute(implicit arguments: Arguments) = (fragments: Seq[FragmentSeq]) => {
    implicit val executor = Executors.newFixedThreadPool(arguments.threadsNb, new NamedThreadFactory("specs2.DefaultExecutionStrategy"))
    try {
      fragments.map { fs =>
        val args = arguments <| fs.arguments
        if (fs.fragments.size > 1 && !args.sequential)
          fs.fragments.map(f => promise(executeFragment(args)(f))).sequence.get
        else
          fs.fragments.map(f => executeFragment(args)(f))
      }.flatten
    } finally {
      executor.shutdown()
    }
  }
}
