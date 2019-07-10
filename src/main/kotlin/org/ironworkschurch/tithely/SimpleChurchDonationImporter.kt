package org.ironworkschurch.tithely

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SimpleChurchDonationImporter(
  private val simpleChurchServiceFactory: SimpleChurchServiceFactory) {
  fun importDonations(newDonations: Collection<Donation>) {
    val chargesByDepositDate = newDonations.groupBy { it.date }
    for ((depositDate, donations) in chargesByDepositDate) {
      donations.toBatchRequest(depositDate)
    }
  }


  private fun List<Donation>.toBatchRequest(donationDate: LocalDate) {
    val simpleChurchService = simpleChurchServiceFactory.login()
    val givingIds = this.map { simpleChurchService.save(it) }
    simpleChurchService.createBatch(
      dateReceived = donationDate,
      name = getBatchTitle(donationDate),
      expectedTotal = getExpectedTotal(),
      givingIds = givingIds
    )
  }

  private fun getBatchTitle(donationDate: LocalDate) = "Tithely Bank ${donationDate.format(DateTimeFormatter.ofPattern("M.d.yy"))}"

  private fun List<Donation>.getExpectedTotal(): BigDecimal {
    return map { it.amount }
      .fold(BigDecimal.ZERO) { acc, elem -> acc.add(elem) }
  }
}
