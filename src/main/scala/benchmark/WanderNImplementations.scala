package benchmark

import cats.effect.concurrent.Semaphore
import monix.eval.Task

object WanderNImplementations {
  def withSemaphore[T, R](l: Seq[T], parallelism: Int)(f: T => Task[R]): Task[Seq[R]] = for {
    s <- Semaphore[Task](parallelism)
    res <- Task.wander(l)(v => s.withPermit(f(v)))
  } yield res
}
