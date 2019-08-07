package benchmark

import monix.execution.Scheduler
import monix.kafka.config.Acks
import monix.kafka.{KafkaProducer, KafkaProducerConfig}
import org.scalameter.api._

import scala.concurrent.duration._

object KafkaProducersBenchmark extends Bench.LocalTime {
  val config = KafkaProducerConfig.default.copy(
    bootstrapServers = List("localhost:9092"),
    lingerTime = 10.millis,
    acks = Acks.NonZero(1),
    batchSizeInBytes = 16777216,
  )

  implicit val sc = Scheduler.io()
  val arraySlize = 10 * 1024

  val producer = KafkaProducer[String, Array[Byte]](config, sc)
  val lockFreeProducer = ModifiedProducer[String, Array[Byte]](config, sc)
  val range = Gen.range("messages")(10000, 10000, 1)

  performance of "KafkaProducer" in {
    measure method "send" in {
      using(range) in { r =>
        Observables
          .createSemaphore(arraySlize, r, 64)(producer.send("test-1", _))
          .completedL
          .runSyncUnsafe()
      }
    }
  }

  performance of "benchmark.ModifiedProducer" in {
    measure method "send" in {
      using(range) in { r =>
        Observables
          .createSemaphore(arraySlize, r, 64)(
            lockFreeProducer.send("test-1", _))
          .completedL
          .runSyncUnsafe()
      }
    }
  }
}
