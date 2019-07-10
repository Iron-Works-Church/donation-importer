package org.ironworkschurch.tithely

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.Properties

class App(
  private val tithelyService: TithelyService,
  private val transactionResolver: TransactionResolver,
  private val simpleChurchDonationImporter: SimpleChurchDonationImporter
)
{
  fun run() {
    val depositedCharges = tithelyService.getDepositedCharges()
    val newDonations = transactionResolver.resolveImportedDonations(depositedCharges)
    simpleChurchDonationImporter.importDonations(newDonations)
  }

}

fun main(args: Array<String>) {
  val categoryMappings = getCategoryMappings()
  val tithelyService = buildTithelyService()
  val simpleChurchServiceFactory = buildSimpleChurchServiceFactory()
  val donorLookup = DonorLookup(tithelyService, simpleChurchServiceFactory)
  val givingCategoryLookup = GivingCategoryLookup(simpleChurchServiceFactory)
  val transactionResolver = TransactionResolver(simpleChurchServiceFactory, donorLookup, categoryMappings, givingCategoryLookup)

  App(tithelyService, transactionResolver, SimpleChurchDonationImporter(simpleChurchServiceFactory)).run()
}

fun buildSimpleChurchServiceFactory(): SimpleChurchServiceFactory {
  return with(getSimpleChurchProperties()) {
    SimpleChurchServiceFactory(baseUrl, userName, password)
  }
}

private fun buildTithelyService(): TithelyService {
  return with (getTithelyProperties()) {
    TithelyService(baseUrl, userName, password, organizationName)
  }
}

private fun getSimpleChurchProperties(): SimpleChurchProperties {
  val properties = getProperties()
  return SimpleChurchProperties(
    baseUrl = properties.getProperty("simplechurch.baseurl"),
    userName = properties.getProperty("simplechurch.username"),
    password = properties.getProperty("simplechurch.password")
  )
}

private fun getTithelyProperties(): TithelyProperties {
  val properties = getProperties()
  return TithelyProperties(
    baseUrl = properties.getProperty("tithely.baseurl"),
    userName = properties.getProperty("tithely.username"),
    password = properties.getProperty("tithely.password"),
    organizationName = properties.getProperty("tithely.organizationName")
  )
}

data class TithelyProperties (
  val baseUrl: String,
  val userName: String,
  val password: String,
  val organizationName: String
)

data class SimpleChurchProperties (
  val baseUrl: String,
  val userName: String,
  val password: String
)

private fun getProperties(): Properties {
  val fileContent = App::class.java.getResource("/credentials-live.properties")
  return Properties().apply {
    fileContent.openStream().use { load(it) }
  }
}

private fun getCategoryMappings(): Map<String, String> {
  val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
  val jsonNode = yamlMapper.readTree(App::class.java.getResource("/CategoryMapping.yml"))
  val givingCategoriesNode = jsonNode.get("givingCategories")
  return givingCategoriesNode.fields()
    .asSequence()
    .associate { it.key to it.value.asText() }
}