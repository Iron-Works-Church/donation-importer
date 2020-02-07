package org.ironworkschurch.tithely

import com.amazonaws.services.sns.AmazonSNSClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

class SimpleChurchDonationImporter(
  private val simpleChurchServiceFactory: SimpleChurchServiceFactory,
  private val snsTopicArn: String) {
  fun importDonations(newDonations: Collection<Donation>): ImportReport {
    val chargesByBatch = newDonations
      .filterNot { it.isEndOfYearDonation() }
      .groupBy { BatchKey(it.date, it.paymentMethod) }
    for ((batchKey, donations) in chargesByBatch) {
      donations
        .toBatchRequest(batchKey.date, batchKey.paymentMethod)
    }

    return ImportReport(chargesByBatch.size, newDonations.size)
  }

  /*
  * Donations at the end of one year or beginning of the next are at risk of getting miscategorized, so we will just
  * skip these and let the staff take care of them manually.
  */
  private fun Donation.isEndOfYearDonation() : Boolean {
    return (date.month == Month.DECEMBER && date.dayOfMonth >= 20) ||
            (date.month == Month.JANUARY && date.dayOfMonth <= 15)
  }

  data class ImportReport(
    val batches: Int,
    val transactions: Int
  )

  private data class BatchKey(val date: LocalDate, val paymentMethod: String?)

  private fun List<Donation>.toBatchRequest(donationDate: LocalDate, paymentMethod: String?) {
    val simpleChurchService = simpleChurchServiceFactory.login()
    val givingIds = this
      .filter { it.simpleChurchId != null }
      .map { simpleChurchService.save(it) }
    val expectedTotal = getExpectedTotal()
    val batchTitle = getBatchTitle(donationDate, paymentMethod)
    simpleChurchService.createBatch(
      dateReceived = donationDate,
      name = batchTitle,
      expectedTotal = expectedTotal,
      givingIds = givingIds
    )
  }

  private fun alertIfExpectedTotalMismatch(batchTitle: String): Int? {
    val amazonSNS = AmazonSNSClient.builder().build();
    amazonSNS.publish(snsTopicArn, """An error occurred while importing donations from Tithely to SimpleChurch.
| The total donation amound for $batchTitle in SimpleChurch did not match the expected total.""".trimMargin())
    return null;
  }

  private fun getBatchTitle(donationDate: LocalDate, paymentMethod: String?): String {
    return "Tithely ${paymentMethod?.capitalize()} ${donationDate.format(DateTimeFormatter.ofPattern("M.d.yy"))}"
  }

  private fun List<Donation>.getExpectedTotal(): BigDecimal {
    return map { it.amount }
      .fold(BigDecimal.ZERO) { acc, elem -> acc.add(elem) }
  }
}
