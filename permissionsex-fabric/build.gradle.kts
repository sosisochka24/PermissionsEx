
import ca.stellardrift.permissionsex.gradle.applyCommonSettings
import ca.stellardrift.permissionsex.gradle.setupPublication
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    id("fabric-loom") version "0.2.5-SNAPSHOT"
    id("com.github.johnrengelman.shadow")
}

applyCommonSettings()
setupPublication()

minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    shade(project(":permissionsex-core")) {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
    }

    shade(project(":impl-blocks:permissionsex-hikari-config"))
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1") { isTransitive=false }

    minecraft("com.mojang:minecraft:1.14.4")
    mappings("net.fabricmc:yarn:1.14.4+build.13")
    modCompile("net.fabricmc:fabric-loader:0.6.3+build.167")

    listOf("net.fabricmc.fabric-api:fabric-api:0.4.0+build.240-1.14",
            "net.fabricmc:fabric-language-kotlin:1.3.50+build.3").forEach {
        modCompile(it)
        include(it)
    }
}

tasks.processResources {
    expand("project" to project)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += arrayOf("-Xjvm-default=enable")
}


val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    configurations = listOf(shade)
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }
    archiveClassifier.set("dev-all")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("org.checkerframework:checker-qual:.*"))
        exclude(dependency("com.google.code.findbugs:jsr305:.*"))
    }

    listOf("com.zaxxer", "com.github.benmanes",
        "com.sk89q.squirrelid", "com.typesafe.config",
        "ninja.leaping.configurate", "org.slf4j", "org.json",
        "org.antlr", "org.yaml", "org.apache.logging.slf4j").forEach {
        relocate(it, "$relocateRoot.$it")
    }
}

val remapShadowJar = tasks.register<RemapJarTask>("remapShadowJar") {
    dependsOn(shadowJar)
    archiveClassifier.set("all")
    input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
}


tasks.assemble {
    dependsOn(shadowJar)
}

tasks.build {
   dependsOn(remapShadowJar)
}
