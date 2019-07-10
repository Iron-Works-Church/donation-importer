plugins {
  id("org.jetbrains.kotlin.jvm").version("1.3.20")
}

repositories {
  jcenter()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("khttp:khttp:0.1.0")
  implementation("com.fasterxml.jackson.core:jackson-core:2.9.9")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9")
  implementation("com.amazonaws:aws-lambda-java-core:1.2.0")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
  testImplementation("org.assertj:assertj-core:3.12.2")
}

tasks {
  val buildZip by creating(Zip::class) {
    from(compileKotlin)
    from(processResources)
    into("lib") {
      from(configurations.compileClasspath)
    }
  }

  assemble {
    dependsOn(buildZip)
  }
}