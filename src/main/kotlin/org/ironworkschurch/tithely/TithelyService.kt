package org.ironworkschurch.tithely
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import khttp.structures.authorization.BasicAuthorization
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit.MILLISECONDS

class TithelyService(
  private val baseUrl: String,
  username: String,
  password: String,
  private val organizationName: String
) {
  private val credentials = BasicAuthorization(username, password)
  private val objectMapper = jacksonObjectMapper()

  fun getDepositedCharges(): Sequence<Charge> {
    return getCharges()
      .filter { !it.depositPending }
  }

  private fun getCharges(): Sequence<Charge> {
    val activeOrganization = getActiveOrganization()
    return getCharges(activeOrganization.organizationId)
  }

  fun getChargePaymentType(chargeId: String): String? {
    val resource = "charges/$chargeId"
    val url = baseUrl + resource
    val response = get(url, auth = credentials)
    val responseTree = objectMapper.readTree(response.text)
    return responseTree.at("/object/payment_method/pm_type").textValue()
  }

  private fun getActiveOrganization(): Organization {
    return getOrganizations()
      .single { it.name == organizationName }
  }

  private fun getOrganizations(): List<Organization> {
    val resource = "organizations-list"
    val url = baseUrl + resource
    val response = get(url, auth = credentials)
    val organizationResponse: OrganizationResponse = objectMapper.readValue(response.text)

    return organizationResponse.data
  }

  private fun getCharges(organization_id: String): Sequence<Charge> {
    var lastChargeId: String? = null
    val createdAfterTimestamp = getCreatedAfterTimestamp()
    return sequence {
      do {
        val charges = getCharges(
          organization_id,
          ending_before = lastChargeId,
          created_after = createdAfterTimestamp
        )
        yieldAll(charges)
        lastChargeId = charges.map { it.chargeId }.lastOrNull()
      } while (lastChargeId != null)
    }
  }

  private fun getCreatedAfterTimestamp(): String {
    return MILLISECONDS.toSeconds(Calendar.getInstance().apply {
      time = Date()
      add(Calendar.WEEK_OF_YEAR, -4)
    }.timeInMillis).toString()
  }

  private fun getCharges(organization_id: String,
                         ending_before: String?,
                         created_after: String?): List<Charge> {
    val resource = "charges-list"
    val url = baseUrl + resource
    val params = mutableMapOf(
      "organization_id" to organization_id,
      "limit" to "100")
    if (ending_before != null) {
      params["ending_before"] = ending_before
    }
    if (created_after != null) {
      params["created_after"] = created_after
    }
    val response = get(url, params = params, auth = credentials)

    val chargeListResponse: ChargeListResponse = objectMapper.readValue(response.text)
    return chargeListResponse.data
  }

  fun getAccount(donorId: String): TithelyAccount {
    val resource = "accounts/$donorId"
    val url = baseUrl + resource
    val response = get(url, auth = credentials)
    val accountResponse: TithelyAccountResponse = objectMapper.readValue(response.text)
    return accountResponse.account
  }
}
