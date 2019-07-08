package org.ironworkschurch.tithely

import java.util.Properties

class App(val tithelyService: TithelyService) {
  fun run() {
    tithelyService.getDepositedCharges().forEach { println(it) }
  }
}

fun main(args: Array<String>) {
  val tithelyService = buildTithelyService()

  App(tithelyService).run()
}

private fun buildTithelyService(): TithelyService {
  return with (getTithelyProperties()) {
    TithelyService(baseUrl, userName, password, organizationName)
  }
}

private fun getTithelyProperties(): TithelyProperties {
  val properties = getProperties()
  return TithelyProperties(
    baseUrl = properties.getProperty("baseurl"),
    userName = properties.getProperty("username"),
    password = properties.getProperty("password"),
    organizationName = properties.getProperty("organizationName")
  )
}

data class TithelyProperties (
  val baseUrl: String,
  val userName: String,
  val password: String,
  val organizationName: String
)

private fun getProperties(): Properties {
  val fileContent = App::class.java.getResource("/credentials-live.properties")
  return Properties().apply {
    fileContent.openStream().use { load(it) }
  }
}
