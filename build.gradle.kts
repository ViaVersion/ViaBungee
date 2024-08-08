plugins {
    id("com.gradleup.shadow") version "8.3.0"
    id("java")
}

group = "com.viaversion.viabungee"
version = "0.1.1"

dependencies {
    compileOnly("com.viaversion:viaversion-common:5.0.3")
    compileOnly("com.viaversion:viabackwards-common:5.0.3")
    compileOnly("com.viaversion:viarewind-common:4.0.2")
    compileOnly("net.raphimc:viaaprilfools-common:3.0.1")
    compileOnly("net.md-5:bungeecord-api:1.20-R0.3-SNAPSHOT")
    implementation("net.lenni0451:Reflect:1.3.4")
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
