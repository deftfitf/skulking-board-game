val ScalaBinary = "2.13"

dependencies {
    implementation(platform("com.typesafe.akka:akka-bom_$ScalaBinary:2.6.14"))
    implementation("com.typesafe.akka:akka-actor-typed_$ScalaBinary")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.11.4")
}