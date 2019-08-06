package benchmark

import monix.eval.Task
import monix.reactive.Observable

import scala.util.Random.nextBytes

object Observables {
  def createSemaphore[R](
      messageSize: Int,
      count: Int,
      chunks: Int,
      parallelism: Int)(action: Array[Byte] => Task[R]): Observable[Seq[R]] = {
    val array = new Array[Byte](messageSize)
    nextBytes(array)

    Observable
      .repeat(array)
      .take(count)
      .bufferIntrospective(chunks)
      .mapEval(v => WanderNImplementations.withSemaphore(v, parallelism)(action))
  }
}
