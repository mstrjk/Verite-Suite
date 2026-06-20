plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.teacommontea"

version = "1.0.0"

java {

    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")

    compileOnly(fileTree("server-libs") { include("paper-*.jar") })
    compileOnly(fileTree("server-libs/libraries") { include("**/*.jar") })

    implementation("org.tukaani:xz:1.9")

    implementation("com.maxmind.geoip2:geoip2:4.2.0")

    implementation(files("libs/eve.jar"))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.shadowJar {
    archiveBaseName.set("Verite")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
