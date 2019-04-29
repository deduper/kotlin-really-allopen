import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    kotlin("kapt") version "1.3.31"
    `maven-publish`
    `java-gradle-plugin`
    groovy
}

group = "no.synth.kotlin.plugins"
version = "0.1-SNAPSHOT"

val artifactName = "kotlin-really-allopen"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-model")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    implementation("com.google.auto.service:auto-service:1.0-rc5")
    kapt("com.google.auto.service:auto-service:1.0-rc5")

    testImplementation("net.bytebuddy:byte-buddy:1.9.12")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
}

gradlePlugin {
    plugins {
        create(artifactName) {
            id = artifactName
            implementationClass = "no.synth.kotlin.plugins.reallyallopen.ReallyAllOpenGradlePlugin"
        }
    }
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

tasks.register("createBuildInfo") {
    doLast {
        File("$buildDir/resources/main").also {
            it.mkdirs()
            File(it, "/build.properties").writeText("""
                groupId=${project.group}
                artifactId=${project.name}
                version=${project.version}
            """.trimIndent())
        }
    }
}

tasks.named("assemble") {
    dependsOn("createBuildInfo")
}
