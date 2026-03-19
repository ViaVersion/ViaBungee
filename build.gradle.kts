plugins {
    id("com.gradleup.shadow") version "8.3.6"
    id("java")
}

group = "com.viaversion.viabungee"
version = "0.3.0"

dependencies {
    compileOnly("com.viaversion:viaversion-common:5.7.2")
    compileOnly("com.viaversion:viabackwards-common:5.7.2")
    compileOnly("com.viaversion:viarewind-common:4.0.15")
    compileOnly("com.viaversion:viaaprilfools-common:4.0.9")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
    implementation("net.lenni0451:Reflect:1.6.2")
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
