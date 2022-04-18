plugins {
    java
    `java-library`
    `maven-publish`
    signing
}

group = "com.botdiril"
version = "0.2.3"


tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.4.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

sourceSets {
    java {
        val modelSet = create("test-model-botdiril") {
            java.srcDirs("test-model-botdiril/java")

            val mainSet = sourceSets.main.get()
            compileClasspath += mainSet.compileClasspath + mainSet.output
        }

        test {
            compileClasspath += modelSet.compileClasspath + modelSet.output
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "Vega"
            url = uri("https://vega.botdiril.com/")
            credentials {
                val vegaUsername: String? by project
                val vegaPassword: String? by project

                username = vegaUsername
                password = vegaPassword
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

dependencies {
    api("org.jetbrains", "annotations", "23.0.0")

    api("com.mchange", "c3p0", "0.9.5.5")
    api("mysql", "mysql-connector-java", "8.0.22")

    api("org.apache.commons", "commons-lang3", "3.11")

    implementation("org.ow2.asm", "asm", "9.2")

    implementation("org.apache.logging.log4j", "log4j-core", "2.16.0")
    implementation("org.apache.logging.log4j", "log4j-api", "2.16.0")

    implementation("com.google.jimfs", "jimfs", "1.2")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
