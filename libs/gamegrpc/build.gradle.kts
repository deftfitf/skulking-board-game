val ScalaBinary = "2.13"

plugins {
    `java-library`
    id("com.lightbend.akka.grpc.gradle").version("2.0.0")
}

akkaGrpc {
    generateClient = true
    generateServer = true
}

dependencies {
    implementation(platform("com.typesafe.akka:akka-bom_$ScalaBinary:2.6.14"))
    implementation("com.typesafe.akka:akka-actor-typed_$ScalaBinary")
    implementation("com.typesafe.akka:akka-protobuf_$ScalaBinary")
}