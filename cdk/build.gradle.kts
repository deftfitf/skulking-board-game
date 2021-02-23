val awsCdkVersion = "1.90.1"

plugins {
    application
    `java-library`
}

application {
    mainClass.set("cdk.stacks.Stacks")
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.16")
    annotationProcessor("org.projectlombok:lombok:1.18.16")

    implementation("software.amazon.awscdk:core:${awsCdkVersion}")
//    implementation "software.amazon.awscdk:apigateway:${awsCdkVersion}"
//    implementation "software.amazon.awscdk:lambda:${awsCdkVersion}"
//    implementation "software.amazon.awscdk:lambda-event-sources:${awsCdkVersion}"
    implementation("software.amazon.awscdk:dynamodb:${awsCdkVersion}")
    implementation("software.amazon.awscdk:ecs:${awsCdkVersion}")
    implementation("software.amazon.awscdk:ecs-patterns:${awsCdkVersion}")
//    implementation("software.amazon.awscdk:sqs:${awsCdkVersion}")
//    implementation("software.amazon.awscdk:rds:${awsCdkVersion}")
//    implementation("software.amazon.awscdk:cloudfront:${awsCdkVersion}")
    implementation("software.amazon.awscdk:s3-deployment:${awsCdkVersion}")
    testImplementation("org.assertj:assertj-core:3.18.0")
}