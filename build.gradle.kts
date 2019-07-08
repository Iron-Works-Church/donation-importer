plugins {
  id("org.jetbrains.kotlin.jvm").version("1.3.20")
  application
}

repositories {
  jcenter()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("khttp:khttp:0.1.0")
  implementation("com.fasterxml.jackson.core:jackson-core:2.9.9")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")

  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
  mainClassName = "org.ironworkschurch.tithely.AppKt"
}
