plugins {
    id("com.gradleup.shadow") version "9.4.0"
    id("java")
}

group = "com.viaversion.viabungee"
version = "0.4.0"

dependencies {
    compileOnly("com.viaversion:viaversion-common:5.9.0")
    compileOnly("com.viaversion:viabackwards-common:5.9.0")
    compileOnly("com.viaversion:viarewind-common:4.1.0")
    compileOnly("com.viaversion:viaaprilfools-common:4.2.0")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
    implementation("net.lenni0451:Reflect:1.6.3")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveFileName.set("ViaBungee-${project.version}.jar")
    }
    processResources {
        val projectVersion = project.version
        filesMatching("bungee.yml") {
            expand(mapOf("version" to projectVersion))
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
