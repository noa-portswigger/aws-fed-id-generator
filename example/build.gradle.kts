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
    implementation("com.resare:aws-fed-id-generator:0.0.1")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

application {
    mainClass.set("Example")
}
