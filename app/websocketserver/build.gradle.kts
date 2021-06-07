val ScalaBinary = "2.13"

plugins {
    id("org.springframework.boot") version "2.4.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.lightbend.akka.grpc.gradle") version "2.0.0"
}

group = "aws.spring"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":libs:gamedomain"))
    implementation(project(":libs:gamegrpc"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation(platform("com.typesafe.akka:akka-bom_$ScalaBinary:2.6.14"))
    implementation("com.typesafe.akka:akka-actor-typed_$ScalaBinary")
    implementation("com.typesafe.akka:akka-protobuf_$ScalaBinary")

    implementation("org.webjars:webjars-locator-core")
    implementation("org.webjars:sockjs-client:1.0.2")
    implementation("org.webjars:stomp-websocket:2.3.3")
    implementation("org.webjars:bootstrap:3.3.7")
    implementation("org.webjars:jquery:3.1.1-1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val dockerImageTag = "websocketserver/$version".toLowerCase()

tasks.register<Exec>("buildDockerfile") {
    commandLine("docker", "build", "-t", dockerImageTag, ".")
    standardOutput = System.out
}

tasks.register<Exec>("runOnDocker") {
    commandLine("docker", "run", "-p", "8080:8080", dockerImageTag)
    standardOutput = System.out
}