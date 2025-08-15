plugins {
    java
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.resare"
version = "0.0.1"
description = "A Java library for generating tokens to prove your AWS identity"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("software.amazon.awssdk:auth:2.32.20")
    api("software.amazon.awssdk:core:2.32.20")

    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("software.amazon.awssdk:sso:2.32.20")
    implementation("software.amazon.awssdk:ssooidc:2.32.20")
    implementation("software.amazon.awssdk:sdk-core:2.32.20")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    dependsOn("spotlessCheck")
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Generate aws-fed-id tokens")
                description.set(project.description)
                url.set("https://github.com/noa-portswigger/aws-fed-id-generator")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("nresare")
                        name.set("Noa Resare")
                        email.set("noa@resare.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/noa-portswigger/aws-fed-id-generator.git")
                    developerConnection.set("scm:git:ssh://github.com/noa-portswigger/aws-fed-id-generator.git")
                    url.set("https://github.com/noa-portswigger/aws-fed-id-generator")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
