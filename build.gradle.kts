plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "com.viaversion.viabungee"
version = "1.0.0"

dependencies {
    compileOnly("com.viaversion:viaversion-common:5.0.2-SNAPSHOT")
    compileOnly("com.viaversion:viabackwards-common:5.0.0-SNAPSHOT")
    compileOnly("com.viaversion:viarewind-common:4.0.0-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.20-R0.3-SNAPSHOT")
    implementation("net.lenni0451:Reflect:1.3.2")
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
