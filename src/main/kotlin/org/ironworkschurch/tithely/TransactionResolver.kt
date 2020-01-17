package org.ironworkschurch.tithely

import java.math.BigDecimal
import java.time.LocalDate

class TransactionResolver (
  private val simpleChurchServiceFactory: SimpleChurchServiceFactory,
  private val donorLookup: DonorLookup,
  private val givingCategories: Map<String, String>,
  private val givingCategoryLookup: GivingCategoryLookup,
  private val tithelyService: TithelyService
) {
  internal fun resolveImportedDonations(depositedCharges: Sequence<Charge>): List<Donation> {
    val transactionsInRange = getTransactionsInRange(depositedCharges)
      .associateBy {
        TransactionLookupKey (
          it.date,
          it.amount.setScale(2),
          it.person.uid,
          it.category.name
        )
      }

    return depositedCharges
      .mapNotNull { it.toDonationLookup() }
      .filterNot { isImported(it, transactionsInRange) }
      .map { it.toDonation() }
      .toList()
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

  private fun isImported(donation: DonationLookup,
                         transactionsInRange: Map<TransactionLookupKey, SimpleChurchBatchItem>): Boolean {
    if (donation.simpleChurchId == null) {
      return false;
    }

    val lookupKey = donation.toLookupKey()

    return transactionsInRange.containsKey(lookupKey)
  }

  private fun DonationLookup.toLookupKey(): TransactionLookupKey {
    return TransactionLookupKey(
      date = date,
      amount = amount,
      donorId = simpleChurchId!!,
      givingCategory = givingCategoryName
    )
  }

  private fun Charge.toDonationLookup(): DonationLookup? {
    val simpleChurchId = donorLookup.getSimpleChurchIdByTithelyDonorId(donorAccount)

    val givingCategoryName = givingCategories[givingType] ?: givingType
    return DonationLookup(
      date = depositDate!!,
      amount = amount.setScale(2),
      simpleChurchId = simpleChurchId,
      givingCategoryName = givingCategoryName,
      givingCategoryId = lookupGivingCategoryId(givingCategoryName),
      transactionId = this.chargeId
    )
  }

  private fun DonationLookup.toDonation(): Donation {
    return Donation(
      simpleChurchId = simpleChurchId,
      amount = amount,
      date = date,
      givingCategoryId = givingCategoryId,
      givingCategoryName = givingCategoryName,
      transactionId = transactionId,
      paymentMethod = tithelyService.getChargePaymentType(transactionId) ?: "unknown"
    )
  }

  private fun lookupGivingCategoryId(givingCategoryName: String): Int {
    return givingCategoryLookup.lookupGivingCategoryId(givingCategoryName)
  }
}

data class DonationLookup(
  val simpleChurchId: Int?,
  val amount: BigDecimal,
  val date: LocalDate,
  val givingCategoryId: Int,
  val givingCategoryName: String,
  val transactionId: String
)

data class Donation(
  val simpleChurchId: Int?,
  val amount: BigDecimal,
  val date: LocalDate,
  val givingCategoryId: Int,
  val givingCategoryName: String,
  val transactionId: String,
  val paymentMethod: String?
)
