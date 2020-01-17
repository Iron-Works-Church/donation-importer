package org.ironworkschurch.tithely

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.*

@Suppress("unused")
class DonationImporter : RequestHandler<DonationImporter.Request, DonationImporter.Response> {
  private val application: App by lazy {
    val categoryMappings = getCategoryMappings()
    val tithelyService = buildTithelyService()
    val simpleChurchServiceFactory = buildSimpleChurchServiceFactory()
    val amazonProperties = getAmazonProperties()
    val donorLookup = DonorLookup(tithelyService, simpleChurchServiceFactory, amazonProperties.snsTopicArn)
    val givingCategoryLookup = GivingCategoryLookup(simpleChurchServiceFactory)
    val transactionResolver = TransactionResolver(
      simpleChurchServiceFactory,
      donorLookup,
      categoryMappings,
      givingCategoryLookup,
      tithelyService
    )

    App(tithelyService, transactionResolver, SimpleChurchDonationImporter(simpleChurchServiceFactory))
  }

  override fun handleRequest(input: Request?, context: Context?): Response {
    val importReport = application.run()
    context?.logger?.log("Imported ${importReport.transactions} transactions in ${importReport.batches} batches")
    return Response(importReport)
  }

  class Request

  data class Response(val importReport: SimpleChurchDonationImporter.ImportReport)
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

private fun getAmazonProperties(): AmazonProperties {
  val properties = getProperties()
  return AmazonProperties(
    snsTopicArn = properties.getProperty("sns.topicArn")
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

data class AmazonProperties(
  val snsTopicArn: String
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