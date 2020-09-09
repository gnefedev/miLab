allprojects {
    group = "mi.lab"
    buildscript {
        repositories {
            mavenCentral()
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.6.1"
}
