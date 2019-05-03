import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    groovy
    kotlin("jvm") version "1.3.31"
    kotlin("kapt") version "1.3.31"
    id("com.gradle.plugin-publish") version "0.10.1"
    `maven-publish`
    `java-gradle-plugin`
}

group = "no.synth.kotlin.plugins"
version = "0.2-SNAPSHOT"

repositories{
	jcenter()
}

sourceSets {
    create("functionalTest") {
        withConvention(GroovySourceSet::class) {
            groovy {
                srcDir(file("src/functionalTest/groovy"))
            }
        }
        resources {
            srcDir(file("src/functionalTest/resources"))
        }
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<Test>("functionalTest") {
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
}

tasks.check { dependsOn(tasks["functionalTest"]) }

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-model")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    implementation("com.google.auto.service:auto-service:1.0-rc5")
    kapt("com.google.auto.service:auto-service:1.0-rc5")

    implementation("org.apache.logging.log4j:log4j-api:2.11.2")
    implementation("org.apache.logging.log4j:log4j-core:2.11.2")

    "functionalTestImplementation"("org.spockframework:spock-core:1.1-groovy-2.4") {
        exclude(module = "groovy-all")
    }
}

gradlePlugin {
    plugins {
        create(project.name) {
            displayName = "Kotlin Really All Open compiler plugin"
            description = "Removes final restriction from all Kotlin classes"
            id = "${project.group}.${project.name}"
            implementationClass = "no.synth.kotlin.plugins.reallyallopen.ReallyAllOpenGradlePlugin"
        }
    }
    testSourceSets(sourceSets["functionalTest"])
}

pluginBundle {
    website = "https://github.com/henrik242/kotlin-really-allopen"
    vcsUrl = "https://github.com/henrik242/kotlin-really-allopen.git"
    tags = listOf("kotlin")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kapt {
    includeCompileClasspath = false
}