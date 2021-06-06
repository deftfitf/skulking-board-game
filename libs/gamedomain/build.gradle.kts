val ScalaBinary = "2.13"

dependencies {
    implementation(platform("com.typesafe.akka:akka-bom_$ScalaBinary:2.6.14"))
    implementation("com.typesafe.akka:akka-cluster-tools_$ScalaBinary")
    implementation("com.typesafe.akka:akka-serialization-jackson_$ScalaBinary")
}
