name := "monix-kafka-benchamrk"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "Sonatype OSS Snapshots" at
  "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "com.storm-enroute" %% "scalameter" % "0.18",
  "io.monix" %% "monix-kafka-1x" % "1.0.0-RC4",
  "org.apache.kafka" % "kafka-clients" % "2.3.0"
)

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

