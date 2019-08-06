package benchmark

import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.atomic.Atomic
import monix.execution.cancelables.{SingleAssignCancelable, StackedCancelable}
import monix.execution.{Callback, Cancelable, Scheduler}
import monix.kafka.{KafkaProducer, KafkaProducerConfig, Serializer}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata, Callback => KafkaCallback, KafkaProducer => ApacheKafkaProducer}

import scala.util.control.NonFatal

trait ModifiedProducer[K, V] extends Serializable {
  def underlying: Task[ApacheKafkaProducer[K, V]]
  def send(topic: String, value: V): Task[Option[RecordMetadata]]
  def send(topic: String, key: K, value: V): Task[Option[RecordMetadata]]
  def send(record: ProducerRecord[K, V]): Task[Option[RecordMetadata]]
  def close(): Task[Unit]
}

object ModifiedProducer {

  /** Builds a [[KafkaProducer]] instance. */
  def apply[K, V](config: KafkaProducerConfig, sc: Scheduler)(
    implicit K: Serializer[K],
    V: Serializer[V]): ModifiedProducer[K, V] =
    new Implementation[K, V](config, sc)

  private final class Implementation[K, V](config: KafkaProducerConfig, sc: Scheduler)(
    implicit K: Serializer[K],
    V: Serializer[V])
    extends ModifiedProducer[K, V] with StrictLogging {

    private val isCanceled = Atomic(false)

    // Gets initialized on the first `send`
    private lazy val producerRef = {
      logger.info(s"Kafka producer connecting to servers: ${config.bootstrapServers.mkString(",")}")
      new ApacheKafkaProducer[K, V](config.toJavaMap, K.create(), V.create())
    }

    def underlying: Task[ApacheKafkaProducer[K, V]] =
      Task.eval(producerRef)

    def send(topic: String, value: V): Task[Option[RecordMetadata]] =
      send(new ProducerRecord[K, V](topic, value))

    def send(topic: String, key: K, value: V): Task[Option[RecordMetadata]] =
      send(new ProducerRecord[K, V](topic, key, value))

    def send(record: ProducerRecord[K, V]): Task[Option[RecordMetadata]] =
      Task.create[Option[RecordMetadata]] { (s, cb) =>
        val asyncCb = Callback.forked(cb)(s)
        val connection = StackedCancelable()
        // Forcing asynchronous boundary
        sc.executeAsync(() => {
          if (isCanceled.get()) {
            asyncCb.onSuccess(None)
          } else {
            val isActive = Atomic(true)
            val cancelable = SingleAssignCancelable()
            try {
              // Force evaluation
              val producer = producerRef

              // Using asynchronous API
              val future = producer.send(
                record,
                new KafkaCallback {
                  def onCompletion(meta: RecordMetadata, exception: Exception): Unit =
                    if (isActive.getAndSet(false)) {
                      connection.pop()
                      if (exception != null)
                        asyncCb.onError(exception)
                      else
                        asyncCb.onSuccess(Option(meta))
                    } else if (exception != null) {
                      s.reportFailure(exception)
                    }
                }
              )

              cancelable := Cancelable(() => future.cancel(false))
            } catch {
              case NonFatal(ex) =>
                // Needs synchronization, otherwise we are violating the contract
                if (isActive.compareAndSet(expect = true, update = false)) {
                  connection.pop()
                  ex match {
                    case _: IllegalStateException if isCanceled.get() =>
                      asyncCb.onSuccess(None)
                    case _ =>
                      asyncCb.onError(ex)
                  }
                } else {
                  s.reportFailure(ex)
                }
            }
          }
        })
        connection
      }

    def close(): Task[Unit] =
      Task.create { (s, cb) =>
        val asyncCb = Callback.forked(cb)(s)
        // Forcing asynchronous boundary
        sc.executeAsync { () =>
        {
          if (!isCanceled.compareAndSet(expect = false, update = true)) {
            asyncCb.onSuccess(())
          } else {
            try {
              producerRef.close()
              asyncCb.onSuccess(())
            } catch {
              case NonFatal(ex) =>
                asyncCb.onError(ex)
            }
          }
        }
        }
      }
  }
}
