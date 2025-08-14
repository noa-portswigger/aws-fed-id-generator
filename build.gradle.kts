plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("software.amazon.awssdk:auth:2.32.20")
    implementation("software.amazon.awssdk:core:2.32.20")
    implementation("software.amazon.awssdk:sso:2.32.20")
    implementation("software.amazon.awssdk:ssooidc:2.32.20")
    implementation("software.amazon.awssdk:sdk-core:2.32.20")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")

}

application {
    mainClass.set("com.resare.aws_fed_id.generator.Example")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}