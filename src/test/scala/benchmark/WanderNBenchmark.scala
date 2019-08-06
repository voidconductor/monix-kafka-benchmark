package benchmark

import monix.execution.Scheduler
import monix.kafka.{KafkaProducerConfig, KafkaProducerSink}
import monix.kafka.config.Acks
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
  val range = Gen.range("messages")(10000, 10000, 1)

  performance of "WanderN" in {
    measure method "semaphore" in {
      using(range) in { r =>
        Observables
          .createSemaphore(arraySlize, r, 500, 100)(producer.send("test-1", _))
          .completedL
          .runSyncUnsafe()
      }
    }
  }

  performance of "KafkaProducer" in {
    measure method "sliding" in {
      using(range) in { r =>
        val arr = new Array[Byte](arraySlize)
        scala.util.Random.nextBytes(arr)
        Observable
          .repeat(arr)
          .take(r)
          .map(a => new ProducerRecord[String, Array[Byte]]("test-1", a))
          .bufferIntrospective(500)
          .consumeWith(KafkaProducerSink[String, Array[Byte]](config, sc))
          .runSyncUnsafe()
      }
    }
  }
}
