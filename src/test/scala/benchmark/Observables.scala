package benchmark

import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.duration._
import scala.util.Random.nextBytes

object Observables {
  def createSemaphore[R](messageSize: Int, count: Int, parallelism: Int)(
      action: Array[Byte] => Task[R]): Observable[Seq[R]] = {
    val array = new Array[Byte](messageSize)
    nextBytes(array)

    Observable
      .repeat(array)
      .take(count)
      .bufferTimedWithPressure(1.second, 30000)
      .mapEval(v =>
        WanderNImplementations.withSemaphore(v, parallelism)(action))
  }


  def createSliding[R](messageSize: Int, count: Int, parallelism: Int)(
    action: Array[Byte] => Task[R]): Observable[Seq[R]] = {
    val array = new Array[Byte](messageSize)
    nextBytes(array)

    Observable
      .repeat(array)
      .take(count)
      .bufferTimedWithPressure(1.second, 30000)
      .mapEval(v =>
        WanderNImplementations.withSiding(v, parallelism)(action))
  }
}
