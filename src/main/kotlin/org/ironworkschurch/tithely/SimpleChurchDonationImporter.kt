package org.ironworkschurch.tithely

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SimpleChurchDonationImporter(
  private val simpleChurchServiceFactory: SimpleChurchServiceFactory) {
  fun importDonations(newDonations: Collection<Donation>) {
    val chargesByBatch = newDonations.groupBy { BatchKey(it.date, it.paymentMethod) }
    for ((batchKey, donations) in chargesByBatch) {
      donations.toBatchRequest(batchKey.date, batchKey.paymentMethod)
    }
  }

  private data class BatchKey(val date: LocalDate, val paymentMethod: String?)

  private fun List<Donation>.toBatchRequest(donationDate: LocalDate, paymentMethod: String?) {
    val simpleChurchService = simpleChurchServiceFactory.login()
    val givingIds = this.map { simpleChurchService.save(it) }
    simpleChurchService.createBatch(
      dateReceived = donationDate,
      name = getBatchTitle(donationDate, paymentMethod),
      expectedTotal = getExpectedTotal(),
      givingIds = givingIds
    )
  }

  private fun getBatchTitle(donationDate: LocalDate, paymentMethod: String?): String {
    return "Tithely ${paymentMethod?.capitalize()} ${donationDate.format(DateTimeFormatter.ofPattern("M.d.yy"))}"
  }

  private fun List<Donation>.getExpectedTotal(): BigDecimal {
    return map { it.amount }
      .fold(BigDecimal.ZERO) { acc, elem -> acc.add(elem) }
  }
}
