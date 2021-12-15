plugins {
    java
    `java-library`
}

group = "com.botdiril"
version = "5.0.0"


tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = "7.3"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains", "annotations", "20.1.0")

    implementation("com.mchange", "c3p0", "0.9.5.5")
    implementation("mysql", "mysql-connector-java", "8.0.22")

    implementation("org.apache.logging.log4j", "log4j-core", "2.16.0")
    implementation("org.apache.logging.log4j", "log4j-api", "2.16.0")

    implementation("org.apache.commons", "commons-lang3", "3.11")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}