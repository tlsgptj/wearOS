// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}
buildscript {
    repositories {
        google()  // 추가
        mavenCentral()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:8.1.4")
        classpath ("com.google.gms:google-services:4.4.2") // Google services plugin 추가
    }
}
