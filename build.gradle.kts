plugins {
    java
    `java-library`
    `maven-publish`
    signing
}

group = "org.muxi"
version = "0.1.0-preview"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("MUXI Java SDK")
                description.set("Java SDK for MUXI AI platform")
                url.set("https://github.com/muxi-ai/muxi-java")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("muxi")
                        name.set("MUXI AI")
                        email.set("support@muxi.ai")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/muxi-ai/muxi-java.git")
                    developerConnection.set("scm:git:ssh://github.com/muxi-ai/muxi-java.git")
                    url.set("https://github.com/muxi-ai/muxi-java")
                }
            }
        }
    }
}
