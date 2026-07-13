plugins {
    java
    `java-library`
    id("com.vanniktech.maven.publish") version "0.28.0"
}

group = "org.muxi"
version = "1.20260713.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    
    coordinates(group.toString(), "muxi-java", version.toString())
    
    pom {
        name.set("MUXI Java SDK")
        description.set("Java SDK for MUXI AI platform")
        url.set("https://github.com/muxi-ai/muxi-java")
        inceptionYear.set("2024")
        
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

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
