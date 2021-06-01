group = "gameserver"
version = "0.0.1-SNAPSHOT"
val ScalaBinary = "2.13"

dependencies {
    implementation(platform("com.typesafe.akka:akka-bom_$ScalaBinary:2.6.14"))
    implementation("com.typesafe.akka:akka-persistence-typed_$ScalaBinary")
    implementation("com.typesafe.akka:akka-cluster-sharding-typed_$ScalaBinary")
    implementation("com.typesafe.akka:akka-serialization-jackson_$ScalaBinary")

    implementation("ch.qos.logback:logback-classic:1.2.3")

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
