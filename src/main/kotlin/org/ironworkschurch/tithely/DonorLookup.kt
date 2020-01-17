package org.ironworkschurch.tithely

import com.amazonaws.services.sns.AmazonSNSClient
import java.util.concurrent.ConcurrentHashMap

class DonorLookup(
  private val tithelyService: TithelyService,
  private val simpleChurchService: SimpleChurchServiceFactory,
  private val snsTopicArn: String)
{
  private val cacheByDonorId: ConcurrentHashMap<String, Int?> = ConcurrentHashMap()
  private val cacheByEmail: ConcurrentHashMap<String, Int?> = ConcurrentHashMap()

  fun getSimpleChurchIdByTithelyDonorId(donorId: String): Int? {
    return cacheByDonorId.computeIfAbsent(donorId) {
      val accountInfo = tithelyService.getAccount(donorId)
      getSimpleChurchIdByEmail(accountInfo.email)
        ?: getSimpleChurchIdByName(accountInfo)
        ?: alertIfNoSimpleChurchMatch(accountInfo)
    }
  }

  private fun alertIfNoSimpleChurchMatch(accountInfo: TithelyAccount): Int? {
    val amazonSNS = AmazonSNSClient.builder().build();
    amazonSNS.publish(snsTopicArn, """An error occurred while importing donations from Tithely to SimpleChurch.
| ${accountInfo.firstName} ${accountInfo.lastName} (${accountInfo.email}) was not found in SimpleChurch.
| Please add the user to SimpleChurch and import the transaction manually.""".trimMargin())
    return null;
  }

  private fun getSimpleChurchIdByName(accountInfo: TithelyAccount): Int? {
    return simpleChurchService.getGiverIdByName(accountInfo.firstName, accountInfo.lastName)
  }

  private fun getSimpleChurchIdByEmail(email: String): Int? {
    return cacheByEmail.computeIfAbsent(email) {
      simpleChurchService.getGiverIdByEmail(email)
    }
  }
}