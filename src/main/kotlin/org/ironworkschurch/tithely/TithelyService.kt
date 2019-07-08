package org.ironworkschurch.tithely
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import khttp.structures.authorization.BasicAuthorization
import java.util.*
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
      .filter {
        it.deposit_date != "pending"
      }
  }

  private fun getCharges(): Sequence<Charge> {
    val activeOrganization = getActiveOrganization()
    return getCharges(activeOrganization.organization_id)
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
        lastChargeId = charges.map { it.charge_id }.lastOrNull()
      } while (lastChargeId != null)
    }
  }

  private fun getCreatedAfterTimestamp(): String {
    return MILLISECONDS.toSeconds(Calendar.getInstance().apply {
      time = Date()
      add(Calendar.MONTH, -1)
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
}