# monix-kafka-benchmark

```
::Benchmark KafkaProducer.send::
cores: 12
hostname: MacBook-Pro-Vasilij.local
name: OpenJDK 64-Bit Server VM
osArch: x86_64
osName: Mac OS X
vendor: AdoptOpenJDK
version: 11.0.4+11
Parameters(messages -> 10000): 2262.172311 ms

::Benchmark benchmark.ModifiedProducer.send::
cores: 12
hostname: MacBook-Pro-Vasilij.local
name: OpenJDK 64-Bit Server VM
osArch: x86_64
osName: Mac OS X
vendor: AdoptOpenJDK
version: 11.0.4+11
Parameters(messages -> 10000): 1603.762846 ms
```

```
::Benchmark WanderN.semaphore::
cores: 12
hostname: MBP-Vasilij
name: OpenJDK 64-Bit Server VM
osArch: x86_64
osName: Mac OS X
vendor: AdoptOpenJDK
version: 11.0.4+11
Parameters(messages -> 10000): 1287.049403 ms

::Benchmark KafkaProducer.sliding::
cores: 12
hostname: MBP-Vasilij
name: OpenJDK 64-Bit Server VM
osArch: x86_64
osName: Mac OS X
vendor: AdoptOpenJDK
version: 11.0.4+11
Parameters(messages -> 10000): 1703.3986 ms
```
