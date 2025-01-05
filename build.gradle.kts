plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("java")
}

group = "com.viaversion.viabungee"
version = "0.2.0"

dependencies {
    compileOnly("com.viaversion:viaversion-common:5.2.1")
    compileOnly("com.viaversion:viabackwards-common:5.2.1")
    compileOnly("com.viaversion:viarewind-common:4.0.5")
    compileOnly("com.viaversion:viaaprilfools-common:4.0.0")
    compileOnly("net.md-5:bungeecord-api:1.20-R0.3-SNAPSHOT")
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
