plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("maven-publish")
}

group = "org.nevertouchgrass"
version = "0.0.6"


tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "org.nevertouchgrass"
            artifactId = "SpringFX"
            this.version = project.version.toString()
        }
    }
}

tasks.jar {
    archiveBaseName.set("SpringFX")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
