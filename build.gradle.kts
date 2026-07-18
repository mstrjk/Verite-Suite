plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.teacommontea"

version = "1.1.1"

java {

    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")

    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

    compileOnly("io.netty:netty-all:4.1.87.Final")

    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("net.luckperms:api:5.4")

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
