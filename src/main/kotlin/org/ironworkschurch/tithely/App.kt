package org.ironworkschurch.tithely

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
