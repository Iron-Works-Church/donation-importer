package org.ironworkschurch.tithely

import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import java.time.Month
import org.ironworkschurch.tithely.ChargeDeserializer.Companion.toLocalDate
import kotlin.test.Test

class TithelyDataModelTest {
  @Test
  fun testToLocalDate() {
    val input = 1561334400L
    val expected = LocalDate.of(2019, Month.JUNE, 23)
    assertThat(input.toLocalDate()).isEqualTo(expected)
  }
}