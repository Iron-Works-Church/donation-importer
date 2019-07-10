package org.ironworkschurch.tithely

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import java.io.IOException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.util.JSONPObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*

data class OrganizationResponse(
  val status: String,
  val type: String,
  val data: List<Organization>
)

data class ChargeListResponse(
  val status: String,
  val type: String,
  val data: List<Charge>
)

data class TithelyAccountResponse(
  val status: String,
  val account_id: String,
  val type: String,
  @JsonProperty("object") val account: TithelyAccount)

data class TithelyChargeResponse(
  val status: String,
  val type: String,
  @JsonProperty("charge_id") val chargeId: String,
  @JsonProperty("object") val charge: Object
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TithelyAccount(
  @JsonProperty("email") val email: String,
  @JsonProperty("first_name") val firstName: String,
  @JsonProperty("last_name") val lastName: String,
  @JsonProperty("phone_number") val phoneNumber: String,
  @JsonProperty("created_date") val createdDate: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Organization(
  @JsonProperty("organization_id") val organizationId: String,
  val name: String
)

@JsonDeserialize(using = ChargeDeserializer::class)
data class Charge (
  val chargeId: String,
  val amount: BigDecimal,
  val currency: String,
  val givingType: String,
  val depositDate: LocalDate?,
  val depositPending: Boolean,
  val donorAccount: String,
  val donorEmail: String?
)

internal class ChargeDeserializer : JsonDeserializer<Charge>() {
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class ChargeRaw (
    @JsonProperty("charge_id") val chargeId: String,
    @JsonProperty("amount") val amount: Long,
    @JsonProperty("currency") val currency: String,
    @JsonProperty("giving_type") val givingType: String,
    @JsonProperty("deposit_date") val depositDate: String,
    @JsonProperty("donor_account") val donorAccount: String,
    @JsonProperty("donor_email") val donorEmail: String?
  )

  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Charge {
    val root = jp.readValueAs(ChargeRaw::class.java)

    return Charge (
      chargeId = root.chargeId,
      amount = root.amount.toBigDecimal().movePointLeft(2),
      currency = root.currency,
      givingType = root.givingType,
      depositDate = root.depositDate.toLongOrNull()?.toLocalDate(),
      depositPending = root.depositDate == "pending",
      donorAccount = root.donorAccount,
      donorEmail = root.donorEmail
    )
  }

  companion object {
    fun Long.toLocalDate(): LocalDate {
      val ofEpochSecond = LocalDateTime.ofEpochSecond(this, 0, ZoneOffset.UTC)

      val zoneOffset = ZoneId.of("America/New_York").rules.getOffset(ofEpochSecond)
      return ofEpochSecond.plusSeconds(zoneOffset.totalSeconds.toLong()).toLocalDate()
    }
  }
}