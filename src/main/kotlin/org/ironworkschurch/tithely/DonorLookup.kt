package org.ironworkschurch.tithely

import java.util.concurrent.ConcurrentHashMap

class DonorLookup(
  private val tithelyService: TithelyService,
  private val simpleChurchService: SimpleChurchServiceFactory)
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