# EU GDPR Consent String Parser Android Plugin for Godot

Google's enforcement of the EU's GDPR consent requirements begins at 16 January 2024. If you use AdMob in your Android game (or any Android project made in Godot), you need to start implementing the consent form.

Fortunately, setting up a consent form in AdMob is quite easy, and both major AdMob plugins support an easy implementation of the GDPR consent popup, and yet, things get a lot complicated when looking at user consent.

## The problem

When the AdMob consent is checked, the returned value is always "3" (consent obtained), regardless of what exactly the user consented to. 

The minimum requirements to show any ads at all, according to this [Google Support article](https://support.google.com/admob/answer/9760862?hl=en&sjid=2418371235660772237-AP), are as follows:

* **The end user grants Google consent to:**
    * Store and/or access information on a device (Purpose 1)
* **Legitimate interest (or consent, where a publisher configures their CMP to request it) is established for Google to:**
    * Select basic ads (Purpose 2)
    * Measure ad performance (Purpose 7)
    * Apply market research to generate audience insights (Purpose 9)
    * Develop and improve products (Purpose 10)

What the support article fails to mention however, is that under "**Vendor preferences**", the vendor named "**Google Advertising Products**" has to be given explicit consent for all this to work. (They passingly mention vendor consent [here](https://developers.google.com/admob/android/privacy/ad-serving-modes), but not in much detail, then fail to include it under requirements)

(*I have reached out to Google about whether legitimate interest should be enough, or explicit vendor consent is necessary, meanwhile the plugin will continue to treat it as mandatory.*)

To make things worse, when the user chooses to press the *Manage options* button on the consent popup, Both "Purpose 1" and explicit consent for "Google Advertising Products" will **default to NO**. If the user does not **mark these manually**, and opts for *Confirm choices*, **the obtained consent will not be sufficient**, and Google will show no ads at all. They very kindly call this serving "limited ads only", but in reality it means an Error code "3" (no ads in network), when trying to load ads from AdMob, **and no ads served**.

To make it ***even worse***, the "Google Advertising Products" vendor is far down on a non-alphabetical list of vendors.

Google seems reluctant to fix this, or even document it properly, despite repeated requests from developers for the last two years (as of January 2024), and developers using the AdMob library would never know what hit them, and their ad revenue, when someone in the EEA feels a little more curious and starts pressing buttons they probably should not. 

Of course, everything works just fine if the user presses the *Consent*, or *Accept all buttons*, on either screen, but you as a developer had no way to know what they chose (or even maybe explicitly denied consent, if that option is available)… 

**UNTIL NOW!*** (*drumroll*)

## The Solution

If you are a Godot developer, desperate to know what your users chose as consent options, maybe because you rely on Rewarded Ads to give them some extra items/lives/or just be able to continue playing, or have any other reasons to gently remind them that your game/app will not have all of its functionality with their current choices, this simple Android Plugin has you covered.

*(Currently only packaged for Godot 3.5. The .aar is exported for 4.2 as well, but I need help packaging with the new format.)*

## Installation

### Godot 3.5

* Download the latest release marked Godot 3.5, and extract the `aar` and `.gdap` files to `<project_root>/android/plugins/` where `<project_root>` is your project root (where the `project.godot` file is).
* Make sure you enable the plugin called "EU Consent String Parser in" the export preferences (*Project > Export...*)

And you're good to go.

### Godot 4.2 and above

A lot has changed in Godot, including the plugin system, and with v2 (of the plugin system) it has become a bit more complicated to package plugins. So, unfortunately, this will be a bit more involved…

*  Download the `zip` containing the `aar` files marked Godot 4.5, and…
*  See if you can package them to work in Godot. Instructions on how to do this are provided this [Godot docs page](https://docs.godotengine.org/en/4.2/tutorials/platform/android/android_plugin.html)
*  Don't forget to open a PR, so that others can enjoy the freshly packaged plugin for Godot 4.2

(Sorry, I currently have no Godot 4 projects, and my time is tight, so I cannot start fiddling with it myself just yet)


## Getting the singleton

Once installed, you should be able to obtain the singleton called `EUConsentStringParser` in any script, like this:

```
if Engine.has_singleton("EUConsentStringParser"):
	var consentParser = Engine.get_singleton("EUConsentStringParser")
```


##  Usage (short version)

### Boolean methods (simple checks)

- `consentParser.canShowAds()` : returns `true` if you can show at least non-personalised ads, or `false` if you can't show any ads.
- `consentParser.canShowPersonalizedAds()`: returns `true` if you can personalised ads, or `false` if you can't show personalised ads.

### Dictionary methods (read consent details)

The following methods will return dictionaries, in a format where the key represents the Purpose's number or "GV" for Google vendor;  while the value will be an `int[]`, where the first item of which represents the status of user consent (1-for granted, 0-for denied), and the second representing the status of legitimate interest (1-for granted, 0-for denied). 

E.g. if the user granted both consent and legitimate interest for "Purpose 2 - Select basic ads", the entry should look something like this:

```
{"2": [1, 1]}
```

- `consentParser.getRawConsentStatusForAllPurposes()`: The Dictionary will contain all 10 purposes, plus Google vendor consent
- `consentParser.getRawConsentStatusForSinglePurpose(int purpose_number)`: Will give you a Dictionary with a single key (same as you give it as an argument), with the status of the Purpose you want to get consent details about. `purpose_number` should either be
    - An integer number between 1-10, corresponding with the Purpose name (so e.g. `1` for Purpose 1), or
    - 0 to check the "Google Advertising Products" consent status (the key will be `GV` in that case)
    - If you give it a number larger than 10, the single key it returns will be `IDX_OOB_ERROR` (check for this in any case, before parsing)

The following method will give you a Dictionary with details of what purposes were denied, that might prevent showing ads. This is convenient to quickly check for problems without having to go through the full list by yourself:

- `consentParser.getConsentStatusIssuesList()`: the returned dictionary might have the following keys:
    - `"ADS_STATUS":` will tell you what types of ads can be served, if any. Possible values are:
        - `0`: **No ads** can be served
        - `1`: Only **non-personalised** ads can be served
        - `2`: **Personalised ads** can be served (In this case, no other keys will be present in the Dictionary)
    - `"MISSING_MANDATORY_CONSENT":` A list of purpose numbers, necessary for serving **any ads** where **consent was not given**
    - `"MISSING_PERSONALISED_CONSENT":` A list of purpose numbers, necessary for serving **personalised ads** where **consent was **not given**
    - `"MISSING_CONSENT_OR_LEGIT_INTEREST":` A list of purpose numbers, necessary for serving **any ads** where consent *or* legitimate interest was **not given**
    - `MISSING_VENDOR_CONSENT:` Always returns the value: `Google Advertising Products vendor consent and/or legitimate interest missing (both are needed)`, since it's meaningless to check which is missing

The following method will return a Dictionary with the values set as the (legally) defined Purpose names (only in English, sorry), and the Google vendor's name (with the key `GV`), matched to the keys other Dictionaries use :

- `consentParser.getFullPurposeNamesByKey()`

If this looks complicated, you can thank Google, the EU, and my own inability to make tings simpler when it's already overly convulted anyway. For a more detailed explanation for how to use the methods and read/parse the results (with relevant examples, read on)...


## Usage (detailed)


### Simple checks

You can call these two methods directly on the singleton, to know if ads are good to go:

```
consentParser.canShowAds() -> bool
```

Will return a simple true or false answer, letting you know **if the user has selected the absolute minimal options to show any kind of ads at all** (not necessarily personalised).

**IMPORTANT**: If this function returns `false`, AdMob will most likely ***not show any ads at all***. ("Limited ads" don't seem to work well in practice.)

```
consentParser.canShowPersonalizedAds() -> bool
```  

Will return a simple true or false answer, letting you know **if the user has selected the absolute minimal options to show personalised ads** (It does not check all available purposes, only the necessary ones).

If this function returns `false`, you might still be able to show non-personalised ads.

If the result of either of the above is false (especially if you cannot show any ads)


### Get consent details


#### Read info for all consent purposes
If either or both of the above methods have returned `false`, and you'd like to investigate why, the following method will return the user's choices in a human *and* machine readable format (although while machines would probably prefer this, humans might frown at it):

```
consentParser.getRawConsentStatusForAllPurposes() -> Dictionary
```

This method will return a dictionary with 11 keys, the numerical representation for each purpose (1-10), and "GV" for Google vendor.

For each key, the value will be an `int[]`, where the first item of which represents the status of user consent (1-for granted, 0-for denied), and the second representing the status of legitimate interest (1-for granted, 0-for denied).

For example, for "Purpose 2 - Select basic ads", you would see something like this:

```
# User granted both consent and legitimate interest:
{"2": [1, 1]}

# User denied consent but granted legitimate interest:
{"2": [0, 1]}

# User granted consent but denied legitimate interest:
{"2": [1, 0]}

# User denied both consent and legitimate interest:
{"2": [0, 0]}

```

The full returned Dictionary will look something like this when the user clicks "Consent" or "Accept All":

```
{"1": [1, 1],
"2": [1, 1],
"3": [1, 1],
"4": [1, 1],
"5": [1, 0],
"6": [1, 0],
"7": [1, 1],
"8": [1, 1],
"9": [1, 1],
"10": [1, 1],
"GV": [1, 1]}
```

Or like this, if the user has given the absolute minimal consent to show non-personalised ads:

```
{"1": [1, 1],
"2": [0, 1],
"3": [0, 0],
"4": [0, 0],
"5": [0, 0],
"6": [0, 0],
"7": [0, 1],
"8": [0, 1],
"9": [0, 1],
"10": [0, 1],
"GV": [1, 1]}
```

**Notes:**
- **Legitimate interest for Consent purposes 1,3 and for 4 is not currently applicable**, for simplicity's sake, those fields will be set to whatever the consent status is
- **Purposes 5, 6 and 8 are not taken into consideration** in showing either personalised and non-personalised ads, but I have included them here for completeness' sake, and for anyone interested in them for any reason.

You can parse this dictionary easily enough, so you can gently remind your users that some functionality (rewards, for example) will require them to change their consent, if necessary.

#### Read info for any single purpose

If you are only interested in a specific purpose's consent status, you can use the following function. It will return a Dictionary with a single key, corresponding to your query, in the same format as above. (*The original reason for using a Dictionary was the limitation on the plugin system's return data types, but it turned out to also be more concise this way*)

```
consentParser.getRawConsentStatusForSinglePurpose(int purpose_number) -> Dictionary
```

where `purpose_number` is the numerical part of the purpose name, so to check the status of e.g. "Purpose 2 - Select basic ads", you would call

```
# get consent status for Purpose 2 - Select basic ads
consentParser.getRawConsentStatusForSinglePurpose(2)

# Output for full consent and legitimate itnerest:
{"2": [1, 1]}
```

or if you are interested in the "Google Advertising Products" vendor consent status, you should pass 0 as the argument:

```
# get consent status for  "Google Advertising Products" vendor 
consentParser.getRawConsentStatusForSinglePurpose(0)

# Output for full consent and legitimate itnerest:
{"GV": [1, 1]}
```

If you pass a number larger than 10 as an argument, the Dictionary will return an Index Out of Bound error as the key. 

```
# get consent status for an invalud purpose number
consentParser.getRawConsentStatusForSinglePurpose(11)

# The output constainsan error key and a String as a value for details:
{"IDX_OOB_ERROR": "Index should be between 0 and 10: 0 for Google vendor consent, 1-10 for any named purposes"}
```

For sanity's sake, you should always check the returned dictionary for `IDX_OOB_ERROR` key, before parsing.

#### Convenience method for checking problems with consent

If you're not into parsing every single consent one by one, *or* prefer to do things the easy way, you can call the following method, and just read the list of missing purposes from the Dictionary it returns:

```
consentParser.getConsentStatusIssuesList() -> Dictionary
```

The returned Dictionary might or might not have the following keys:

- `"ADS_STATUS":` will tell you what types of ads can be served, if any. Possible values are:
    - `0`: **No ads** can be served
    - `1`: Only **non-personalised** ads can be served
    - `2`: **Personalised ads** can be served
- `"MISSING_MANDATORY_CONSENT":` A list of purpose numbers, necessary for serving **any ads** where **consent was not given**
- `"MISSING_PERSONALISED_CONSENT":` A list of purpose numbers, necessary for serving **personalised ads** where **consent was not given**
- `"MISSING_CONSENT_OR_LEGIT_INTEREST":` A list of purpose numbers, necessary for serving **any ads** where **consent *or* legitimate interest was not given**
- `MISSING_VENDOR_CONSENT:` Always has the value: `Google Advertising Products vendor consent and/or legitimate interest missing (both are needed)`, since it's meaningless to check which is missing (only present if applicable, of course)

If the `"ADS_STATUS"` key has the value of `2`, the Dictionary should have no other keys. In any other case, you will find at least one of the above, so it's worth checking all of them.

**In case you can't serve any ads (`ADS_STATUS: 0`)**:

In the very likely case that the user pressed the "Manage choices" button, then accepted them without specifying anything, the Dictionary will look like this:

```
{"ADS_STATUS": 0,
"MISSING_MANDATORY_CONSENT": [1],
"MISSING_PERSONALISED_CONSENT": [1, 3, 4],
"MISSING_VENDOR_CONSENT": Google Advertising Products vendor consent and/or legitimate interest missing (both are needed).}
```

If, for argument's sake (and to illustrate the point), the user goes as far as explicitly denying both consent and legitimate interest for "Purpose 2 - Select basic ads" and "Purpose 7 - Measure ad performance" while leaving other options unchanged, the output will have an extra key:

```
{"ADS_STATUS": 0,
"MISSING_MANDATORY_CONSENT": [1],
"MISSING_PERSONALISED_CONSENT": [1, 3, 4],
"MISSING_CONSENT_OR_LEGIT_INTEREST": [2, 7],
"MISSING_VENDOR_CONSENT": Google Advertising Products vendor consent and/or legitimate interest missing (both are needed).}
```

**In case you can't serve personalised ads (`ADS_STATUS: 1`)**:

If the user somehow managed to set non-personalised ads only (very unlikely under the current conditions with the official AdMob popup), the Dictionary will look like this:

```
{"ADS_STATUS": 1,
"MISSING_PERSONALISED_CONSENT": [3, 4]}
```

since the only real difference between personalised and non-personalised ad consent is having given consent to Purposes 3 and 4.

#### Convenience method for getting full purpose names

There is one more method that will return a Dictionary with the currently (legally) defined Purpose names (only in English, sorry), and the Google vendor's name as defined in the list, matched to the keys other Dictionaries use :

```
consentParser.getFullPurposeNamesByKey() -> Dictionary
```

will give you:

```
{"1": Purpose 1 - Store and/or access information on a device
"2": Purpose 2 - Select basic ads
"3": Purpose 3 - Create a personalised ads profile
"4": Purpose 4 - Select personalised ads
"5": Purpose 5 - Create a personalised content profile
"6": Purpose 6 - Select personalised content
"7": Purpose 7 - Measure ad performance
"8": Purpose 8 - Measure content performance
"9": Purpose 9 - Apply market research to generate audience insights
"10": Purpose 10 - Develop and improve products
"GV": Google Advertising Products vendor consent anf legitimate interest}
```

You can use this lists if you want to somehow include these in any form of communication, notice, FAQ, whatever you chose to gently remind your users that Google made both your and their lives exponentially harder when implementing the already overly-bloated EU law so badly.

## Help wanted

- The plugin is currently only packaged for Godot 3.5, as already mentioned above. if you have experience with Godot 4.2, and feel like helping out, please push a packaged version to the `builds` folder, and open a PR. 
- If you have suggestions on how to make this README more readable, don't hesitate to chime in with any constructive criticism, edits, etc. :)

Any help is most welcome.