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
    implementation(project(":"))
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

application {
    mainClass.set("Example")
}