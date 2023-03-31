import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.8.0"
}

val userHome = System.getProperty("user.home")
val vpPluginsDir = "${userHome}\\AppData\\Roaming\\VisualParadigm\\plugins"
val vpPluginName = project.name
val vpPluginDir = "$vpPluginsDir/$vpPluginName"

group = "com.vygovskiy.vpplugins"
version = "1.0"


repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(files("lib/openapi.jar"))
    //implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
    testImplementation("junit", "junit", "4.12")
    implementation("com.miglayout:miglayout:3.7.4")

}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


tasks.register("lvTest") {
    val uh = System.getProperty("user.home")
    println("")
}

tasks.register("vpCopyPlugin") {
    group = "Visual Paradigm"
    dependsOn(
            "build",
            "vpDeletePluginDir",
            "vpCopyClasses",
            "vpCopyDependenciesJars"
    )
}

tasks.register<Delete>("vpDeletePluginDir") {
    group = "Visual Paradigm"

    delete(vpPluginDir)
}


tasks.register<Copy>("vpCopyClasses") {
    group = "Visual Paradigm"

    dependsOn("build")
    from(
            "$buildDir/classes/kotlin/main",
            "$buildDir/resources/main"
    )
    into(vpPluginDir)

    doLast {
        copyPluginXml()
    }
}

tasks.register<Copy>("vpCopyDependenciesJars") {
    group = "Visual Paradigm"

    from(
            getJarDependencies()
                    .map { it.absoluteFile }
                    .toTypedArray()
    )
    into("$vpPluginDir/lib")
}

/**
 * Return collection of JAR dependencies
 */
fun getJarDependencies(): FileCollection {
    return configurations.runtimeClasspath.get()
            .filter { !it.name.startsWith("openapi") }
            .filter { it.name.endsWith("jar")}

}

fun copyPluginXml() {
    val runtimeSection = getJarDependencies().joinToString(
        separator = "\n",
        prefix = "\n  <runtime>\n",
        postfix = "\n  </runtime>\n"
    ) {"    <library path='lib/${it.name}' relativePath='true'/>"}


    val pluginXmlFile = File("$buildDir/resources/main/plugin.xml").readText()
    val content = pluginXmlFile.replace("<runtime/>", runtimeSection, false)
    File("$vpPluginDir/plugin.xml").writeText(content)
}