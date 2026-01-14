plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
}

// Version aus Gradle Property oder Fallback
val pluginVersion: String = project.findProperty("version")?.toString() ?: "dev"

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("WhitelistPlugin")
    archiveVersion.set(pluginVersion)

    from("src/main/resources")
}
