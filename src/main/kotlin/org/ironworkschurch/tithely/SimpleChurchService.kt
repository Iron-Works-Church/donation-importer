package org.ironworkschurch.tithely

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import khttp.post
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

open class SimpleChurchServiceFactory(
  private val baseUrl: String,
  private val username: String,
  private val password: String
) {
  private val objectMapper = jacksonObjectMapper()

  open fun login(): SimpleChurchService {
    val response = post(baseUrl + "user/login", params = mapOf(
      "username" to username,
      "password" to password
    ))

    val loginResponse: LoginResponse = objectMapper.readValue(response.text)
    val loginSession = loginResponse.data ?: throw RuntimeException("Could not login: ${loginResponse.error}")
    return SimpleChurchService(baseUrl, loginSession.session_id, objectMapper)
  }

  fun getGiverIdByEmail(email: String): Int? {
    val loginContext = login()
    val searchResponse = loginContext.searchByEmail(email) ?: return null

    return loginContext.getGiverId(searchResponse)
  }

  fun getGiverIdByName(firstName: String, lastName: String): Int? {
    val loginContext = login()
    val searchResponse = loginContext.searchByName(firstName, lastName) ?: return null

    return loginContext.getGiverId(searchResponse)
  }

  private fun SimpleChurchService.getGiverId(searchResponse: SimpleChurchPeopleSearchIndividual): Int {
    val individual: SimpleChurchIndividual = getIndividualDetails(searchResponse.uid)
    val selfFamilyMember = individual.family.singleOrNull { it.uid == searchResponse.uid }

    return when {
      selfFamilyMember == null -> individual.uid
      selfFamilyMember.givesWithFamily -> selfFamilyMember.primaryUid
      else -> selfFamilyMember.uid
    }
  }
}

open class SimpleChurchService(
  private val baseUrl: String,
  private val sessionId: String,
  private val objectMapper: ObjectMapper
) {
  fun searchByEmail(email: String): SimpleChurchPeopleSearchIndividual? {
    val response = get(baseUrl + "people/search", params = mapOf(
      "mail" to email,
      "session_id" to sessionId
    ))

    val searchResponse: SimpleChurchPeopleSearchResponse = objectMapper.readValue(response.text)
    return searchResponse.data.firstOrNull()
  }

  fun searchByName(firstName: String, lastName: String): SimpleChurchPeopleSearchIndividual? {
    val response = get(baseUrl + "people/search", params = mapOf(
      "fname" to "%" + firstName.trim().substringBefore(' ') + "%",
      "lname" to "%" + lastName.trimEnd().substringAfterLast(' ') + "%",
      "session_id" to sessionId
    ))

    val searchResponse: SimpleChurchPeopleSearchResponse = objectMapper.readValue(response.text)
    return searchResponse.data.singleOrNull()
  }

  fun getIndividualDetails(uid: Int): SimpleChurchIndividual {
    val response = get(baseUrl + "people/$uid", params = mapOf(
      "session_id" to sessionId
    ))

    val personResponse: SimpleChurchPersonResponse = objectMapper.readValue(response.text)
    return personResponse.data
  }

  fun getBatch(batch: SimpleChurchBatch): SimpleChurchBatch {
    val response = get(baseUrl + "/giving/batch/${batch.id}", params = mapOf(
      "session_id" to sessionId
    ))

    val batchesResponse: SimpleChurchBatchResponse = objectMapper.readValue(response.text)
    return batchesResponse.data
  }

  fun getBatches(): List<SimpleChurchBatch> {
    val response = get("$baseUrl/giving/batches", params = mapOf(
      "session_id" to sessionId
    ))

    val batchesResponse: SimpleChurchBatchesResponse = objectMapper.readValue(response.text)
    return batchesResponse.data
  }

  fun save(donation: Donation): Int {
    val response = post(
      url = "$baseUrl/giving",
      params = mapOf("session_id" to sessionId),
      data = mapOf(
        "uid" to donation.simpleChurchId,
        "amount" to donation.amount.toString(),
        "date" to donation.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
        "categoryId" to donation.givingCategoryId,
        "method" to "import",
        "transactionId" to donation.transactionId
      )
    )

    val givingResponse: GivingResponse = objectMapper.readValue(response.text)
    return givingResponse.data.id
  }

  fun createBatch(dateReceived: LocalDate,
                  name: String,
                  expectedTotal: BigDecimal,
                  givingIds: List<Int>) {

    val response = post(
      url = "$baseUrl/giving/batch",
      params = mapOf("session_id" to sessionId),
      data = mapOf(
        "dateReceived" to dateReceived.format(DateTimeFormatter.ISO_LOCAL_DATE),
        "name" to name,
        "expectedTotal" to expectedTotal.toDouble(),
        "givingIds" to givingIds.joinToString(separator = ",")
      ))

    check(response.statusCode < 400)
  }

  open fun getGivingCategories(): List<GivingCategory> {
    val response = get("$baseUrl/giving/categories", params = mapOf(
      "session_id" to sessionId
    ))

    val batchesResponse: GivingCategoriesResponse = objectMapper.readValue(response.text)
    return batchesResponse.data
  }

  data class GivingResponse(
    val success: Boolean,
    val statusCode: Int,
    val data: GivingId
  )

  data class GivingId(val id: Int)

}

data class SimpleChurchPeopleSearchResponse(
  val success: Boolean,
  val statusCode: Int,
  val data: List<SimpleChurchPeopleSearchIndividual>
)

data class SimpleChurchPeopleSearchIndividual(
  val fname: String,
  val lname: String,
  val uid: Int
)

data class SimpleChurchPersonResponse(
  val success: Boolean,
  val statusCode: Int,
  val data: SimpleChurchIndividual
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimpleChurchIndividual(
  val uid: Int,
  val mail: String,
  val fname: String,
  val lname: String,
  val family: List<SimpleChurchFamilyMember>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SimpleChurchFamilyMember(
  val primaryUid: Int,
  val uid: Int,
  val givesWithFamily: Boolean,

  val relationship: String
)

data class SimpleChurchBatchesResponse(
  val success: Boolean,
  val statusCode: Int,
  val data: List<SimpleChurchBatch>
)

data class SimpleChurchBatchResponse(
  val success: Boolean,
  val statusCode: Int,
  val data: SimpleChurchBatch
)

data class GivingCategoriesResponse(
  val success: Boolean,
  val statusCode: Int,
  val data: List<GivingCategory>
)

@JsonDeserialize(using = SimpleChurchBatchDeserializer::class)
data class SimpleChurchBatch(
  val id: Int,
  val created: Long,
  val updated: Long,
  val name: String,
  val expectedTotal: BigDecimal,
  val dateReceived: LocalDate,
  //val sfoSynced: Boolean,
  //val cnSynced: String,
  //val preferredName: String,
  val dateReceivedDisplay: String,
  val expectedTotalDisplay: String,
  val createdDate: String,
  //val user: SimpleChurchPeopleSearchIndividual,
  val entries: List<SimpleChurchBatchItem> = listOf(),
  val currentTotal: BigDecimal?
)

class SimpleChurchBatchDeserializer : JsonDeserializer<SimpleChurchBatch>() {
  @JsonIgnoreProperties(ignoreUnknown = true)
  data class SimpleChurchBatchRaw(
    val id: Int,
    val created: Long,
    val updated: Long,
    val name: String,
    val expectedTotal: BigDecimal,
    val dateReceived: String,
    val dateReceivedDisplay: String,
    val expectedTotalDisplay: String,
    val createdDate: String,
    val entries: List<SimpleChurchBatchItem> = listOf(),
    val currentTotal: BigDecimal?
  )

  override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): SimpleChurchBatch {
    val root = p.readValueAs(SimpleChurchBatchRaw::class.java)
    return SimpleChurchBatch(
      id = root.id,
      created = root.created,
      updated = root.updated,
      name = root.name,
      expectedTotal = root.expectedTotal.setScale(2, RoundingMode.UNNECESSARY),
      dateReceived = LocalDate.parse(root.dateReceived),
      dateReceivedDisplay = root.dateReceivedDisplay,
      expectedTotalDisplay = root.expectedTotalDisplay,
      createdDate = root.createdDate,
      entries = root.entries,
      currentTotal = root.currentTotal?.setScale(2, RoundingMode.UNNECESSARY)
    )
  }

}

@JsonDeserialize(using = SimpleChurchBatchItemDeserializer::class)
data class SimpleChurchBatchItem(
  val amount: BigDecimal,
  val date: LocalDate,
  val id: String,
  val created: String,
  val updated: String,
  val person: SimpleChurchPeopleSearchIndividual,
  val category: GivingCategory
)

class SimpleChurchBatchItemDeserializer : JsonDeserializer<SimpleChurchBatchItem>() {
  data class SimpleChurchBatchItemRaw(
    val amount: BigDecimal,
    val date: String,
    val id: String,
    val created: String,
    val updated: String,
    val person: SimpleChurchPeopleSearchIndividual,
    val category: GivingCategory
  )

  override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): SimpleChurchBatchItem {
    val root = p.readValueAs(SimpleChurchBatchItemRaw::class.java)
    return SimpleChurchBatchItem(
      amount = root.amount.setScale(2, RoundingMode.UNNECESSARY),
      date = LocalDate.parse(root.date),
      id = root.id,
      created = root.created,
      updated = root.updated,
      person = root.person,
      category = root.category
    )
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GivingCategory(
  val id: Int,
  val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginSession(val session_id: String)

data class LoginResponse(
  val success: Boolean,
  val statusCode: Int,
  val data: LoginSession?,
  val error: String?)
