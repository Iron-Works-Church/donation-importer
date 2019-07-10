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
  fun run(): SimpleChurchDonationImporter.ImportReport {
    val depositedCharges = tithelyService.getDepositedCharges()
    val newDonations = transactionResolver.resolveImportedDonations(depositedCharges)
    return simpleChurchDonationImporter.importDonations(newDonations)
  }

}
