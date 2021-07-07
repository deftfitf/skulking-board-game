group = "gameserver"
version = "0.0.1-SNAPSHOT"
val ScalaBinary = "2.13"
val AkkaManagementVersion = "1.1.0"

plugins {
    application
    id("com.lightbend.akka.grpc.gradle").version("2.0.0")
}

application {
    mainClass.set("gameserver.GameServerApplication")
    applicationDefaultJvmArgs = listOf("-Dconfig.resource=local2.conf")
}

dependencies {
    implementation(project(":libs:dynamodbdao"))
    implementation(project(":libs:gamedomain"))
    implementation(project(":libs:gamegrpc"))

    implementation(platform("com.typesafe.akka:akka-bom_$ScalaBinary:2.6.14"))
    implementation("com.typesafe.akka:akka-persistence-typed_$ScalaBinary")
    implementation("com.typesafe.akka:akka-cluster-sharding-typed_$ScalaBinary")
    implementation("com.typesafe.akka:akka-discovery_$ScalaBinary")
    implementation("com.typesafe.akka:akka-serialization-jackson_$ScalaBinary")
    implementation("com.typesafe.akka:akka-protobuf_$ScalaBinary")

    implementation("com.lightbend.akka.management:akka-management_$ScalaBinary:$AkkaManagementVersion")
    implementation("com.lightbend.akka.management:akka-management-cluster-http_$ScalaBinary:$AkkaManagementVersion")
    implementation("com.lightbend.akka.management:akka-management-cluster-bootstrap_$ScalaBinary:$AkkaManagementVersion")

    implementation("com.typesafe.akka:akka-persistence-dynamodb_$ScalaBinary:1.2.0-RC2")

    implementation("com.typesafe.akka:akka-http-spray-json_$ScalaBinary:10.2.3")

    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("org.mockito:mockito-core:3.6.0")
    testImplementation("com.typesafe.akka:akka-persistence-testkit_$ScalaBinary")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_$ScalaBinary")
}

val dockerImageTag = "gameserver/$version".toLowerCase()

tasks.register<Exec>("buildDockerfile") {
    commandLine("docker", "build", "-t", dockerImageTag, ".")
    standardOutput = System.out
}

tasks.register<Exec>("runOnDocker") {
    commandLine("docker", "run", "-p", "8080:8080", dockerImageTag)
    standardOutput = System.out
}
