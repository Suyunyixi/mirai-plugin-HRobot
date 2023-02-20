import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.9.2"
}

group = "com.happysnaker"
version = "3.4.1-SNAPSHOT"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies{
    implementation("com.alibaba:fastjson:1.2.76")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jfree:jfreechart:1.0.19")
    implementation("org.reflections:reflections:0.9.11")
    implementation("org.yaml:snakeyaml:1.25")
    implementation("org.jsoup:jsoup:1.11.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.quartz-scheduler:quartz:2.2.1")

}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
}