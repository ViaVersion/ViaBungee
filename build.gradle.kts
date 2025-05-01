plugins {
    id("com.gradleup.shadow") version "8.3.6"
    id("java")
}

group = "com.viaversion.viabungee"
version = "0.3.0-SNAPSHOT"

dependencies {
    compileOnly("com.viaversion:viaversion-common:5.3.2")
    compileOnly("com.viaversion:viabackwards-common:5.3.0")
    compileOnly("com.viaversion:viarewind-common:4.0.6")
    compileOnly("com.viaversion:viaaprilfools-common:4.0.1")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.3-SNAPSHOT")
    implementation("net.lenni0451:Reflect:1.4.0")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveFileName.set("ViaBungee-${project.version}.jar")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
