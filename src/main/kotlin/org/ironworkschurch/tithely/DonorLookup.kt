package org.ironworkschurch.tithely

import java.util.concurrent.ConcurrentHashMap

class DonorLookup(
  val tithelyService: TithelyService,
  val simpleChurchService: SimpleChurchServiceFactory)
{
  val cacheByDonorId: ConcurrentHashMap<String, Int?> = ConcurrentHashMap()
  val cacheByEmail: ConcurrentHashMap<String, Int?> = ConcurrentHashMap()

  fun getSimpleChurchIdByTithelyDonorId(donorId: String): Int? {
    return cacheByDonorId.computeIfAbsent(donorId) {
      val accountInfo = tithelyService.getAccount(donorId)
      getSimpleChurchIdByEmail(accountInfo.email) ?: getSimpleChurchIdByName(accountInfo)
    }
  }

  private fun getSimpleChurchIdByName(accountInfo: TithelyAccount): Int? {
    return simpleChurchService.getGiverIdByName(accountInfo.firstName, accountInfo.lastName)
  }

  fun getSimpleChurchIdByEmail(email: String): Int? {
    return cacheByEmail.computeIfAbsent(email) {
      simpleChurchService.getGiverIdByEmail(email)
    }
  }
}