package benchmark

import monix.eval.Task
import monix.execution.Scheduler
import monix.kafka.config.Acks
import monix.kafka.{KafkaProducerConfig, KafkaProducerSink}
import monix.reactive.Observable
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalameter.Bench
import org.scalameter.api.Gen

import scala.concurrent.duration._

object WanderNBenchmark extends Bench.LocalTime {
  val config = KafkaProducerConfig.default.copy(
    bootstrapServers = List("localhost:9092"),
    lingerTime = 10.millis,
    acks = Acks.NonZero(1),
    batchSizeInBytes = 16777216,
    maxRequestSizeInBytes = 33554432,
    monixSinkParallelism = 100
  )

  implicit val sc: Scheduler = Scheduler.io()
  val arraySlize = 10 * 1024

  val producer = ModifiedProducer[String, Array[Byte]](config, sc)
  val range = Gen.unit("messages")

  performance of "WanderN" in {
    measure method "semaphore" in {
      using(range) in { r =>
        Observables
          .createSemaphore(arraySlize, 30000, 100)(v => Task {v.map(_ + 1)})
          .completedL
          .runSyncUnsafe()
      }
    }
  }

  performance of "WanderN" in {
    measure method "sliding" in {
      using(range) in { r =>
        Observables
          .createSliding(arraySlize, 30000, 100)(v => Task {v.map(_ + 1)})
          .completedL
          .runSyncUnsafe()
      }
    }
  }
}
