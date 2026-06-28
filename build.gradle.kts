plugins {
    java
    id("com.google.protobuf") version "0.9.4"
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("io.papermc.paperweight.userdev") version "1.7.6"
}

group = "com.chunkmc"
version = "1.0.0"
description = "ChunkMC UserTitle - Player title system plugin"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation("io.grpc:grpc-netty-shaded:1.68.1")
    implementation("io.grpc:grpc-protobuf:1.68.1")
    implementation("io.grpc:grpc-stub:1.68.1")
    implementation("com.google.protobuf:protobuf-java:4.28.3")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.3"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.1"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/java")
            srcDirs("build/generated/source/proto/main/grpc")
        }
    }
}

tasks {
    build { dependsOn("generateProto", "reobfJar") }
    jar { manifest { attributes["Main-Class"] = "com.chunkmc.usertitle.UserTitlePlugin" } }
    reobfJar { outputJar.set(layout.buildDirectory.file("libs/${project.name}-${project.version}.jar")) }
    processResources { filesMatching("plugin.yml") { expand("version" to project.version, "name" to project.name) } }
}
