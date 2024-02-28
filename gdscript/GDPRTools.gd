extends Node

var consentParser:JNISingleton
var denied_mandatory_consents:Array = []
var denied_personalised_consents:Array = []
var denied_consent_or_legit_interests:Array = []
var missing_vendor_consent = false


func _ready():
	if Engine.has_singleton("EUConsentStringParser"):
		consentParser = Engine.get_singleton("EUConsentStringParser")
	else:
		consentParser = null
		push_error("EUConsentStringParser singleton not found")


# Checks if the user has provideed consent previously. Especially useful if
# the consent check fails for soem reason,(dMob will use the existing consent string 
# that case, and now you have a way to confirm if there is one
func previous_consent_string_exists() -> bool:
	return consentParser.consentStringExists()


# To check if GDPR applies to the user at all
func is_consent_needed() -> bool:
	return consentParser.consentIsNeeded()


# Use this to quickly check if the absolute minimum requirements for showing any ads (personalised 
# or not) were given
func can_show_any_ads() -> bool:
	return consentParser.canShowAds()


# Use this to quickly check if the absolute minimum requirements for showing 
# personalised ads were given
func can_show_personalised_ads() -> bool:
	return consentParser.canShowPersonalizedAds()


# Combines the above two checks into a single method
# Returns a boolean array, where 
# index [0] - is for basic (or any) ads
# index [1] - is for personalised ads
# So basically:
# [true, true] - means personalised ads can be served
# [true, false] - means only non-personaised ads can be served
# [false, false] - means no ads will be served
# If no consent is necessary, it always returns [true, true]
func is_consent_sufficient() -> Array:
	if is_consent_needed():
		return [consentParser.canShowAds(), consentParser.canShowPersonalizedAds()]
	else:
		return [true, true]


# Checks for any issues with consent, and stores them in the appropriate variables
# the variables can be accessed raw, or you can use one of the methods below.
# The returned boolean is only false if personalised ads can be served (it's used by other functions, internally)
func has_consent_issues() -> bool:
	var issues:Dictionary = consentParser.getConsentStatusIssuesList()
	
	if issues["ADS_STATUS"] > 1:
		return false
	if issues.has("MISSING_MANDATORY_CONSENT"):
		denied_mandatory_consents = issues["MISSING_MANDATORY_CONSENT"]
	if issues.has("MISSING_PERSONALISED_CONSENT"):
		denied_personalised_consents = issues["MISSING_PERSONALISED_CONSENT"]
	if issues.has("MISSING_CONSENT_OR_LEGIT_INTEREST"):
		denied_consent_or_legit_interests = issues["MISSING_CONSENT_OR_LEGIT_INTEREST"]
	if issues.has("MISSING_VENDOR_CONSENT"):
		missing_vendor_consent = true

	return true


# Returns an Array of all known issues. 
# The Arrays in indices [0], [1], and [2] will be empty if no issues are found
# index [3] will be be false (a single boolean, not an array) if no vendor consent issue is found
func get_all_consent_issues()-> Array:
	if has_consent_issues():
		return [denied_mandatory_consents, denied_personalised_consents, denied_consent_or_legit_interests, missing_vendor_consent]
	else:
		return []


# Get the ids of Purpses where consent was denied
# resulting in not being able to show ANY ads
func get_denied_mandatory_consents() -> Array:
	if has_consent_issues():
		return denied_mandatory_consents
	return []


# Get the ids of Purpses where consent was denied
# resulting in not being able to show PERSONALISED ads
func get_denied_personalised_consents() -> Array:
	if has_consent_issues():
		return denied_personalised_consents
	return []


# Get the ids of Purpses where consent OR legitimate interest was denied
# resulting in not being able to show ANY ads
func get_denied_consent_or_legit_interests() -> Array:
	if has_consent_issues():
		return denied_consent_or_legit_interests
	return []


# If false, Google Advertising Products vendor does not have sufficient consent
# (it needs both consent and legitimate interest to be graned), and NO ADS can be served
func is_missing_vendor_consent() -> bool:
	if has_consent_issues():
		return missing_vendor_consent
	else:
		return false


# Gets the raw statuses for all Purposes in its raw Dictionary format
# Keys for purposes are the Purpose ids represented by their number (1-10)
# The key for the Google Advertising Products vendor is "GV"
# The value for each key will be an int Array, where index [0] is for consent and index [1] is for legitimate interest
# For example:
# {"2": [1,1]} - means that for Purpose 2, both consent and legitimate interest were GRANTED
# {"2": [0,1]} - means that for Purpose 2, consent was DENIED but legitimate interest was GRANTED
# {"2": [1,0]} - means that for Purpose 2, consent was GRANTED and legitimate interest was DENIED
# {"2": [0,0]} - means that for Purpose 2, both consent and legitimate interest were DENIED
func get_raw_conset_statuses() -> Dictionary:
	return consentParser.getRawConsentStatusForAllPurposes()


# Translates the statuses of all Purposes into a two dimensional Array or bool Arrays
# Status of a specific purpose can be obtained by the Array index, which should correspond to the 
# Purpose's id, represented by their number (1-10)
# The index for the Google Advertising Products vendor is "0"
# Array on each index will be an bool Array, where  index [0] is for consent and index [1] is for legitimate interest
# For example:
#
# [true,true]   - means that both consent and legitimate interest were GRANTED for the corresponding  purpose
# [false,true]  - means that consent was DENIED but legitimate interest was GRANTED for the corresponding  purpose
# [true,false]  - means that consent was GRANTED and legitimate interest was DENIED for the corresponding  purpose
# [false,false] - means that both consent and legitimate interest were DENIED for the corresponding  purpose
#
# So, for example:
# 	if translated_statuses[2] == [true, true]: 
# 		print("Both consent and legitimate interest were GRANTED for Purpose 2")
func get_conset_statuses() -> Array:
	var raw_statuses:Dictionary = consentParser.getRawConsentStatusForAllPurposes()
	var translated_statuses:Array = []
	translated_statuses.resize(11)
	
	for key in raw_statuses:
		if key != "GV":
			var id:int = int(key)
			translated_statuses[id] = get_consent_status_by_id(id)
		else:
			translated_statuses[0] = get_consent_status_by_id(0)
	
	return translated_statuses


# Gets the statuses for a single Purposes in its raw Dictionary format
# purpose_id should be the Purpose id represented by its number (1-10)
# To get consent status for Google Advertising Products vendor, purpose_id should be 0
#
# The returned Dictionary has one entry, where the key is the Purpose id, or 
# "GV" for Google Advertising Products vendor 
#
# The value for the single key will be an int Array, where index [0] is for consent and index [1] is for legitimate interest
# For example:
# {"2": [1,1]} - means that for Purpose 2, both consent and legitimate interest were GRANTED
# {"2": [0,1]} - means that for Purpose 2, consent was DENIED but legitimate interest was GRANTED
# {"2": [1,0]} - means that for Purpose 2, consent was GRANTED and legitimate interest was DENIED
# {"2": [0,0]} - means that for Purpose 2, both consent and legitimate interest were DENIED
func get_raw_consent_status_by_id(purpose_id) -> Dictionary:
	if purpose_id > 10:
		push_error("EUConsentStringParser: Consent index out of bounds")
		return {}
	return consentParser.getRawConsentStatusForSinglePurpose(purpose_id)


# Returns the consent status of a single purpose as a bool Array where index [0] is for consent and index [1] is for legitimate interest
#
# purpose_id should be the  Purpose id represented by its number (1-10)
# To get consent status for Google Advertising Products vendor, purpose_id should be 0
#
# For example:
# [true,true]   - means that both consent and legitimate interest were GRANTED for the corresponding  purpose
# [false,true]  - means that consent was DENIED but legitimate interest was GRANTED for the corresponding  purpose
# [true,false]  - means that consent was GRANTED and legitimate interest was DENIED for the corresponding  purpose
# [false,false] - means that both consent and legitimate interest were DENIED for the corresponding  purpose
func get_consent_status_by_id(purpose_id) -> Array:
	var key:String
	if purpose_id > 10:
		push_error("EUConsentStringParser: Consent index out of bounds")
		return []
	
	if purpose_id == 0:
		key = "GV"
	else:
		key = str(purpose_id)
	
	var raw_consent_data:Dictionary = consentParser.getRawConsentStatusForSinglePurpose(purpose_id)
	var consent_status = bool(raw_consent_data[str(key)][0])
	var legitimate_interest_status = bool(raw_consent_data[str(key)][1])
	
	return [consent_status, legitimate_interest_status]


# Gets the currently defined purpose names from the singleton, as defined by law at
# the time of release (in English only)
func get_purpose_names() -> Dictionary:
	return consentParser.getFullPurposeNamesByKey()


# Build a String Array of all purposes where consent was denied, in a human readable format
# Can be used for messaging, or informing the user
func get_all_consent_issues_as_text() -> Array:
	var issues_texts:Array = []
	var purpose_names_dict:Dictionary = get_purpose_names()
	
	if has_consent_issues():
		for id in denied_mandatory_consents:
			var consent_name = purpose_names_dict[str(id)] + ": missing consent"
			issues_texts.append(consent_name)
		for id in denied_personalised_consents:
			var consent_name = purpose_names_dict[str(id)] + ": missing consent for personalised ads"
			issues_texts.append(consent_name)
		for id in denied_consent_or_legit_interests:
			var consent_name = purpose_names_dict[str(id)] + ": missing consent or legitimate interest"
			issues_texts.append(consent_name)
		if missing_vendor_consent:
			if not get_consent_status_by_id(0)[0]:
				issues_texts.append("Google Advertising Products: missing vendor consent")
			if not get_consent_status_by_id(0)[1]:
				issues_texts.append("Google Advertising Products: vendor missing legitmate interest")
	
	return issues_texts


# Same as above, only formatted with bbcode (to be used with RichTextLabel)
func get_all_consent_issues_as_bbcode_text() -> Array:
	var issues_texts:Array = []
	var purpose_names_dict:Dictionary = get_purpose_names()

	if has_consent_issues():
		for id in denied_mandatory_consents:
			var consent_name = purpose_names_dict[str(id)] + ":\n[b]missing consent[/b]\n"
			issues_texts.append(consent_name)
		for id in denied_consent_or_legit_interests:
			var consent_name = purpose_names_dict[str(id)] + ":\n[b]missing consent or legitimate interest[/b]\n"
			issues_texts.append(consent_name)
		if missing_vendor_consent:
			if not get_consent_status_by_id(0)[0]:
				issues_texts.append("Google Advertising Products:\n[b]missing vendor consent[/b]\n")
			if not get_consent_status_by_id(0)[1]:
				issues_texts.append("Google Advertising Products:\n[b]vendor missing legitmate interest[/b]\n")

	return issues_texts



# Same as above, only without basic consent details, only the issues preventing
# you from showing PERSONALISED ads
func get_personalised_consent_issues_as_text() -> Array:
	var issues_texts:Array = []
	var purpose_names_dict:Dictionary = get_purpose_names()
	
	if has_consent_issues() and not can_show_personalised_ads():
		for id in denied_personalised_consents:
			var consent_name = purpose_names_dict[str(id)] + ": missing consent for personalised ads"
			issues_texts.append(consent_name)
		for id in denied_consent_or_legit_interests:
			var consent_name = purpose_names_dict[str(id)] + ": missing consent or legitimate interest"
			issues_texts.append(consent_name)
		if missing_vendor_consent:
			if not get_consent_status_by_id(0)[0]:
				issues_texts.append("Google Advertising Products: missing vendor consent")
			if not get_consent_status_by_id(0)[0]:
				issues_texts.append("Google Advertising Products: vendor missing legitmate interest")
	
	return issues_texts
