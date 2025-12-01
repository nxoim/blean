//noinspection UseTomlInstead

plugins {
    `kotlin-dsl`
}

dependencies {
    // compile only so does not conflict with the one specified in the toml
    compileOnly(libs.kotlinGradlePlugin)
    compileOnly(libs.androidBuildTools)
    compileOnly(libs.composeGradlePlugin)
//    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
//    compileOnly(files(configuration.javaClass.superclass.protectionDomain.codeSource.location))
}