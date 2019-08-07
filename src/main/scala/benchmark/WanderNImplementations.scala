package benchmark

import cats.effect.concurrent.Semaphore
import monix.eval.Task

object WanderNImplementations {
  def withSemaphore[T, R](l: Seq[T], parallelism: Int)(f: T => Task[R]): Task[Seq[R]] = for {
    s <- Semaphore[Task](parallelism)
    res <- Task.wander(l)(v => s.withPermit(f(v)))
  } yield res

  def withSiding[T, R](l: Seq[T], parallelism: Int)(f: T => Task[R]): Task[Seq[R]] = {
    if (parallelism == 1)
      Task.traverse(l)(f)
    else {
      val batches = l.sliding(parallelism, parallelism)
      val tasks = for (b <- batches) yield Task.gather(b.map(f))
      Task.sequence(tasks.toList).map(_.flatten)
    }
  }
}
