package org.ironworkschurch.tithely

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class App(
  val tithelyService: TithelyService,
  val simpleChurchServiceFactory: SimpleChurchServiceFactory,
  val donorLookup: DonorLookup,
  val givingCategories: Map<String, String>)
{
  fun run() {
    val depositedCharges = tithelyService.getDepositedCharges()
      //.filter { it.chargeId == "ch_HYPFilaZWCy_23001531" } // FIXME Remove this filter
    val newDonations = removeImportedDonations(depositedCharges)
    newDonations.forEach { println(it) }
  }

  private fun removeImportedDonations(depositedCharges: Sequence<Charge>): Collection<Charge> {
    val transactionsInRange = getTransactionsInRange(depositedCharges)
      .associateBy {
        TransactionLookupKey (
          it.date,
          it.amount.setScale(2),
          it.person.uid,
          it.category.name
        )
      }

    return depositedCharges.filterNot { isImported(it, transactionsInRange) }.toList()
  }

  private fun getTransactionsInRange(depositedCharges: Sequence<Charge>): List<SimpleChurchBatchItem> {
    val simpleChurchService = simpleChurchServiceFactory.login()
    val earliestDepositDate = depositedCharges.map { it.depositDate!! }.min()
    return simpleChurchService.getBatches()
      .filterNot { it.dateReceived.isBefore(earliestDepositDate) }
      .map { simpleChurchService.getBatch(it) }
      .flatMap { it.entries }
  }


  data class TransactionLookupKey (
    val date: LocalDate,
    val amount: BigDecimal,
    val donorId: Int,
    val givingCategory: String
  )

  private fun isImported(charge: Charge, transactionsInRange: Map<TransactionLookupKey, SimpleChurchBatchItem>): Boolean {
    val lookupKey = lookupKey(charge) ?: return false

    return transactionsInRange.containsKey(lookupKey)
  }

  private fun lookupKey(charge: Charge): TransactionLookupKey? {
    val simpleChurchId = donorLookup.getSimpleChurchIdByTithelyDonorId(charge.donorAccount) ?: return null

    return TransactionLookupKey(
      date = charge.depositDate!!,
      amount = charge.amount.setScale(2),
      donorId = simpleChurchId,
      givingCategory = givingCategories[charge.givingType] ?: charge.givingType
    )
  }
}

fun main(args: Array<String>) {
  val categoryMappings = getCategoryMappings()
  val tithelyService = buildTithelyService()
  val simpleChurchServiceFactory = buildSimpleChurchServiceFactory()
  val donorLookup = DonorLookup(tithelyService, simpleChurchServiceFactory)

  App(tithelyService, simpleChurchServiceFactory, donorLookup, categoryMappings).run()
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