package org.ironworkschurch.tithely

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GivingCategoryLookupTest {
  @Test
  fun `test lookup with trailing space`() {
    val factory = buildFactory(GivingCategory(29, "Hope Town "))
    val givingCategoryId = GivingCategoryLookup(factory).lookupGivingCategoryId("Hope Town")
    assertThat(givingCategoryId).isEqualTo(29)
  }

  @Test
  fun `test lookup with leading space`() {
    val factory = buildFactory(GivingCategory(29, " Hope Town"))
    val givingCategoryId = GivingCategoryLookup(factory).lookupGivingCategoryId("Hope Town")
    assertThat(givingCategoryId).isEqualTo(29)
  }

  @Test
  fun `test lookup with trailing space in search`() {
    val factory = buildFactory(GivingCategory(29, "Hope Town"))
    val givingCategoryId = GivingCategoryLookup(factory).lookupGivingCategoryId("Hope Town ")
    assertThat(givingCategoryId).isEqualTo(29)
  }

  @Test
  fun `test lookup with leading space in search`() {
    val factory = buildFactory(GivingCategory(29, "Hope Town"))
    val givingCategoryId = GivingCategoryLookup(factory).lookupGivingCategoryId(" Hope Town")
    assertThat(givingCategoryId).isEqualTo(29)
  }

  @Test
  fun `test lookup with mismatched case (upper)`() {
    val factory = buildFactory(GivingCategory(29, "Hope Town"))
    val givingCategoryId = GivingCategoryLookup(factory).lookupGivingCategoryId("HoPe ToWn")
    assertThat(givingCategoryId).isEqualTo(29)
  }


  @Test
  fun `test lookup with mismatched case (lower)`() {
    val factory = buildFactory(GivingCategory(29, "HoPe ToWn"))
    val givingCategoryId = GivingCategoryLookup(factory).lookupGivingCategoryId("Hope Town")
    assertThat(givingCategoryId).isEqualTo(29)
  }

  private fun buildFactory(givingCategory: GivingCategory): SimpleChurchServiceFactory {
    val serviceStub = object : SimpleChurchService("", "", jacksonObjectMapper()) {
      override fun getGivingCategories(): List<GivingCategory> {
        return listOf(givingCategory)
      }
    }
    return object : SimpleChurchServiceFactory("", "", "") {
      override fun login() = serviceStub
    }
  }
}