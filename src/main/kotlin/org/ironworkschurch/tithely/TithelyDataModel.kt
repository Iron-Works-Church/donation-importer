package org.ironworkschurch.tithely

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

data class Organization(
  val organization_id: String,
  val widget_id: String,
  val account_id: String,
  val created_date: String,
  val name: String,
  val phone_number: String,
  val website: String,
  val address: Address,
  val giving_types: List<String>,
  val giving_types_full: List<GivingType>,
  val bank: Bank,
  val legal: LegalInfo
)

data class Address (
  val street_address: String,
  val city: String,
  val state: String,
  val postal: String,
  val country: String
)

data class GivingType (
  val id: String,
  val name: String,
  val status: String
)

data class Bank (
  val account_number_last4: String,
  val name: String,
  val country: String,
  val currency: String
)

data class LegalInfo (
  val entity_type: String,
  val first_name: String?,
  val last_name: String?,
  val date_of_birth: String?
)

data class Charge (
  val charge_id: String,
  val charge_status: String,
  val amount: String,
  val net_amount: String,
  val fees: String,
  val currency: String,
  val giving_type: String,
  val charge_date: String,
  val deposit_date: String,
  val payment_method: String?,
  val organization: String,
  val donor_account: String,
  val donor_email: String?,
  val donor_first_name: String?,
  val donor_last_name: String?
)