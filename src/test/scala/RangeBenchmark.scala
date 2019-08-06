import monix.eval.Task
import monix.execution.Scheduler
import monix.kafka.config.Acks
import monix.kafka.{KafkaProducer, KafkaProducerConfig}
import monix.reactive.Observable
import org.scalameter.api._

import scala.concurrent.duration._

object RangeBenchmark extends Bench.LocalTime {
  val config = KafkaProducerConfig.default.copy(
    bootstrapServers = List("localhost:9092"),
    lingerTime = 10.millis,
    acks = Acks.NonZero(1),
    batchSizeInBytes = 16777216,
  )

  implicit val sc = Scheduler.io()
  val array = new Array[Byte](10 * 1024)
  scala.util.Random.nextBytes(array)

  val producer = KafkaProducer[String, Array[Byte]](config, sc)
  val lockFreeProducer = ModifiedProducer[String, Array[Byte]](config, sc)
  val range = Gen.range("messages")(10000, 10000, 1)


  performance of "KafkaProducer" in {
    measure method "send" in {
      using(range) in { r =>
        Observable
          .range(1, r)
          .map { _ =>
            array
          }
          .bufferIntrospective(500)
          .mapEval(v => Main.wanderWithParallelism(v, 64)(producer.send("test", _)))
          .completedL
          .runSyncUnsafe()
      }
    }
  }

  performance of "ModifiedProducer" in {
    measure method "send" in {
      using(range) in { r =>
        Observable
          .range(1, r)
          .map { _ =>
            array
          }
          .bufferIntrospective(500)
          .mapEval(v => Main.wanderWithParallelism(v, 64)(lockFreeProducer.send("test", _)))
          .completedL
          .runSyncUnsafe()
      }
    }
  }
}
