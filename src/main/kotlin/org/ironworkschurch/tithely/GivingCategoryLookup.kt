package org.ironworkschurch.tithely

import java.util.concurrent.ConcurrentHashMap

class GivingCategoryLookup(
  private val simpleChurchServiceFactory: SimpleChurchServiceFactory
) {
  private val cache: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

  fun lookupGivingCategoryId(givingCategoryName: String): Int {
    if (cache.isEmpty()) {
      val givingCategories: List<GivingCategory> = simpleChurchServiceFactory.login().getGivingCategories()
      val givingCategoryMap = givingCategories.associate { it.name.normalizeSearchString() to it.id }
      cache.putAll(givingCategoryMap)
    }

    return cache.getValue(givingCategoryName.normalizeSearchString())
  }

  private fun String.normalizeSearchString() = trim().toLowerCase()
}