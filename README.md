# EU GDPR Consent String Parser Android Plugin for Godot

Google's enforcement of the EU's GDPR consent requirements begins at 16 January 2024. If you use AdMob in your Android game (or any Android project made in Godot), you need to start implementing the consent form.

Fortunately, setting up a consent form in AdMob is quite easy, and both working AdMob plugins support an easy implementation of the GDPR consent popup and yet, things get a lot complicated when we're looking at user consent.

## The problem

When the AdMob consent is checked, the returned value is always "3" (consent obtained), regardless of what exactly the user consented to. 

The minimum requirements to show any ads at all, according to this [Google Support article](https://support.google.com/admob/answer/9760862?hl=en&sjid=2418371235660772237-AP), are:

* **The end user grants Google consent to:**
    * Store and/or access information on a device (Purpose 1)
* **Legitimate interest (or consent, where a publisher configures their CMP to request it) is established for Google to:**
    * Select basic ads (Purpose 2)
    * Measure ad performance (Purpose 7)
    * Apply market research to generate audience insights (Purpose 9)
    * Develop and improve products (Purpose 10)

What the support article fails to mention however, is that under "**Vendor preferences**", the vendor named "**Google Advertising Products**" has to be given explicit consent for all this to work. (They passingly mention vendor consent [here](https://developers.google.com/admob/android/privacy/ad-serving-modes), but not in much detail, then fail to include it under the requirements.)

(*I have reached out to Google about this and they clarified that explicit consent is always necessary for the "Google Advertising Products" vendor, the default legitimate interest is not enough.*)

To make things worse, when the user chooses to press the *Manage options* button on the consent popup, Both "Purpose 1" and explicit consent for "Google Advertising Products" will **default to NO  CONSENT**. If the user **does not enable *both* of these manually**, and opts for *Confirm choices*, **the obtained consent will not be sufficient**, and Google will show no ads at all. They very kindly call this serving "limited ads only", but in reality it means an Error code "3" (no ads in network), when trying to load ads from AdMob, **and no ads served**.

To make it ***even worse***, the "Google Advertising Products" vendor is far down on a long, non-alphabetical list of vendors.

Google seems reluctant to fix this, or even document it properly, despite repeated requests from developers for the last two years (as of January 2024), and developers using the AdMob library would never know what hit them and their ad revenue, when someone in the EEA feels a little more curious and starts pressing buttons they probably should not. 

Of course, everything works just fine if the user presses the *Consent*, or *Accept all buttons*, on either screen, but you as a developer had no way to know what they chose (or even maybe explicitly denied consent, if that option is available)… 

**UNTIL NOW!** 

(*drumroll*)

## The Solution

If you are a Godot developer, desperate to know what your users chose as consent options, maybe because you rely on Rewarded Ads to give them some extra items/lives/or just be able to continue playing, or have any other reasons to gently remind them that your game/app will not have all of its functionality with their current choices, this simple Android Plugin has you covered.

*(Currently only packaged for Godot 3.5. The .aar is exported for 4.2 as well, but I need help packaging with the new format.)*

## Installation

### Godot 3.5

* Download the latest release marked Godot 3.5, and extract the `aar` and `.gdap` files to `<project_root>/android/plugins/` where `<project_root>` is your project root (where the `project.godot` file is).
* Make sure you enable the plugin called "EU Consent String Parser" in the export preferences (*Project > Export...*)

And you're good to go.

### Godot 4.2 and above

A lot has changed in Godot, including the plugin system, and with v2 (of the plugin system) it has become a bit more complicated to package plugins. So, unfortunately, this will be a bit more involved…

*  Download the `zip` containing the `aar` files marked Godot 4.2, and…
*  See if you can package them to work in Godot. Instructions on how to do this are provided on [this Godot docs page](https://docs.godotengine.org/en/4.2/tutorials/platform/android/android_plugin.html)
*  Don't forget to open a PR, so that others can enjoy the freshly packaged plugin for Godot 4.2

(Sorry, I currently have no Godot 4 projects, and my time is tight, so I cannot start fiddling with it myself just yet)


## Getting the singleton

Once installed, you should be able to obtain the singleton called `EUConsentStringParser` in any script, like this:

```
#You can name the variable whatever you like, but should you, really?
if Engine.has_singleton("EUConsentStringParser"):
	var consentParser = Engine.get_singleton("EUConsentStringParser")
```


##  Public methods exposed to Godot

- `consentParser.consentIsNeeded()`: Returns `true` for users for whom GDPR applies (i.e. inside the EEA), and `false` in every other case
- `consentParser.canShowAds()` : returns `true` if you can show at least non-personalised ads, or `false` if you can't show any ads.
- `consentParser.canShowPersonalizedAds()`: returns `true` if you can show personalised ads, or `false` if you can't show personalised ads.
- `consentParser.getRawConsentStatusForSinglePurpose(int purpose_number)`: Will give you a Dictionary with a single key (the key is the same number as you give it as an argument, or "GV", see below), with the status of the Purpose you want to get consent details about. `purpose_number` should either be:
    - An integer between 1-10, corresponding with the Purpose name (so e.g. `1` for Purpose 1), or
    - 0 to check the "Google Advertising Products" consent status (the key will be `GV` in that case)
    - If you give it a number larger than 10, the single key it returns will be `IDX_OOB_ERROR` (check for this in any case, before parsing)
- `consentParser.getRawConsentStatusForAllPurposes()`: The Dictionary will contain all 10 purposes, plus the "Google Advertising Products" vendor consent. Purposes are represented by their number as the key (see above), the "Google Advertising Products" vendor has the key `GV`.
- `consentParser.getConsentStatusIssuesList()`: returns a Dictionary with details of what purposes were denied, that might prevent showing ads. The Dictionary might (or might not) have the following keys:
    - `"ADS_STATUS":` Always present. It will tell you what types of ads can be served, if any. Possible values are:
        - `0`: **No ads** can be served
        - `1`: Only **non-personalised** ads can be served
        - `2`: **Personalised ads** can be served (In this case, no other keys will be present in the Dictionary)
        - `3`: **User outside of EEA**, EU consent is not applicable. (*No other keys will be present. If you are showing the GDPR consent to users outside of the EEA for any weird reason, you need to parse the consent statuses manually, using* `consentParser.getRawConsentStatusForAllPurposes()`)
    - `"MISSING_MANDATORY_CONSENT":` A list of purpose numbers, necessary for serving **any ads** where **consent was not given**
    - `"MISSING_PERSONALISED_CONSENT":` A list of purpose numbers, necessary for serving **personalised ads** where **consent was not given**
    - `"MISSING_CONSENT_OR_LEGIT_INTEREST":` A list of purpose numbers, necessary for serving **any ads** where **consent *or* legitimate interest was not given**
    - `MISSING_VENDOR_CONSENT:` Always has the value: `Google Advertising Products vendor consent and/or legitimate interest missing (both are needed)`
- `consentParser.getFullPurposeNamesByKey()`: will return a Dictionary with the values set as the (legally) defined Purpose names (only in English, sorry), and the Google vendor's name (with the key `GV`), matched to the keys other Dictionaries use

If this looks complicated, you can thank Google, the EU, and my own inability to make things simpler when it's already overly complicated anyway. For a more detailed explanation for how to use the methods and read/parse the results (with relevant examples, read on)...


## Usage (detailed)

### Godot wrapper script
If you don't want to use the singleton directly, have a look at the Godot wrapper script in the `/gdscript` folder, which makes things more Godot-friendly and, occasionally, user friendly. The Godot code is (badly) annotated and (kind of ) self explanatory. You will find a separate README file there, with more detailed instructios for the script. (Still, it might not hurt to read the more details stuff below, too).

You can use the script as is, even AutoLoad it, or modify it, copy/paste parts of it, use it as an example, inspiration, or even toilet paper if you print it out.

If you want to use the singleton directly, or want to understand better what's happening in the plugin and where (any why), read on...

### Simple checks

First of all, you might want to check if GDPR even applies to the user:

```
consetnParser.consentIsNeeded() -> bool 
```

Returns `true` for users within the EEA (or the EU and GB, or however Google implemented it), to whom GDPR applies (i.e. inside the EEA), and `false` in every other case. The Google Mobile Ads SDK or UMP SDK would already tell you this when checking consent status, but it does not hurt being able to manually check from here as well. 

**Note**: *You can theoretically configure the GDPR popup to show outside of the EEA, even though it makes little sense to do so. If you opt to do that, some methods might not work as expected.* `consentParser.canShowAds()` *and* `consentParser.canShowPersonalizedAds()` *will always return*  `true`, *since the law does not care about the non-EEA user's choices, and* `consentParser.getConsentStatusIssuesList()` *will be not very helpful either. If, for some weird reason, you want to restrict your own ad revenue even further, and allow your users to opt out of some or all advertising, you can still use* `consentParser.getRawConsentStatusForAllPurposes()` *and parse the returned consent information manually.* 

If the above method returned `true`, you can call these two methods directly on the singleton, to know if ads are good to go:

```
consentParser.canShowAds() -> bool
```

Will return a simple `true` or `false` answer, letting you know **if the user has selected the absolute minimal options to show any kind of ads at all** (not necessarily personalised).

**Note**: This function always returns `true` for users outside of the EEA, where EU GDPR laws don't apply.

**IMPORTANT**: If this function returns `false`, AdMob will most likely ***not show any ads at all***. ("Limited ads" don't seem to work well in practice.)

```
consentParser.canShowPersonalizedAds() -> bool
```  

Will return a simple `true` or `false` answer, letting you know **if the user has selected the absolute minimum options to show personalised ads** (It does not check all available purposes, only the necessary ones).

**Note**: This function always returns `true` for users outside of the EEA, where EU GDPR laws don't apply.

If this function returns `false`, you might still be able to show non-personalised ads.

If the result of either of the above is `false` (especially if you cannot show any ads), you can check for issues with the following methods:


### Get consent details

#### Easily check for problems with consent

The following method will help identifying which Purposes are missing consent and/or legitimate interest, where applicable:

```
consentParser.getConsentStatusIssuesList() -> Dictionary
```

The returned Dictionary might (or might not) have the following keys:

- `"ADS_STATUS":` will tell you what types of ads can be served, if any. This key is always present. Possible values are:
    - `0`: **No ads** can be served
    - `1`: Only **non-personalised** ads can be served
    - `2`: **Personalised ads** can be served
    - `3`: **User outside of EEA**, EU consent is not applicable.
- `"MISSING_MANDATORY_CONSENT":` A list of any purpose numbers as an `int` `Array`, necessary for serving **any ads**, where **consent was not given**
- `"MISSING_PERSONALISED_CONSENT":` A list of any purpose numbers as an `int` `Array`, necessary for serving **personalised ads**, where **consent was not given**
- `"MISSING_CONSENT_OR_LEGIT_INTEREST":` A list of any purpose numbers as an `int` `Array`, necessary for serving **any ads**, where **consent *or* legitimate interest was not given**. (These are the same for personalised and non-personalised ads.)
- `MISSING_VENDOR_CONSENT:` **Always has the value**: `Google Advertising Products vendor consent and/or legitimate interest missing (both are needed)`, since it's pointless to check which is missing (only present if applicable, of course)

If the `"ADS_STATUS"` key has the value of `2` or `3`, the Dictionary should have **no other keys**. In any other case, you will find at least one of the above, so it's worth checking all of them, but only applicable ones will be present in the Dictionary (so no keys with empty Arrays.)

**Note**: This function will not show missing consent info for users outside of the EEA, where EU GDPR laws don't apply.

Examples:

**In case you can't serve any ads (`ADS_STATUS: 0`)**:

In the very likely case that the user pressed the "Manage choices" button, then accepted their choices without specifying anything, the Dictionary will look like this:

```
{"ADS_STATUS": 0,
"MISSING_MANDATORY_CONSENT": [1],
"MISSING_PERSONALISED_CONSENT": [1, 3, 4],
"MISSING_VENDOR_CONSENT": Google Advertising Products vendor consent and/or legitimate interest missing (both are needed).}
```

You will probably see this a lot. All flexible purposes (2,7,9,10) default to legitimate interest, but the user has to manually select at least Purpose 1 (Also for Purposes 3 and 4 for personalised ads), **and** provide explicit consent to the "Google Advertising Products" vendor, which is among the worst UX fails in history, but there's very little we can do about it until Google chooses to fix it.

If, for argument's sake (and to illustrate the point), the user goes as far as explicitly denying both consent and legitimate interest for e.g. "Purpose 2 - Select basic ads" and "Purpose 7 - Measure ad performance" while leaving other options unchanged, the Dictionary will have an extra key:

```
{"ADS_STATUS": 0,
"MISSING_MANDATORY_CONSENT": [1],
"MISSING_PERSONALISED_CONSENT": [1, 3, 4],
"MISSING_CONSENT_OR_LEGIT_INTEREST": [2, 7],
"MISSING_VENDOR_CONSENT": Google Advertising Products vendor consent and/or legitimate interest missing (both are needed).}
```

That about covers the worst case scenarios, i.e. no ads at all.

**In case you can't serve personalised ads (`ADS_STATUS: 1`)**:

If the user somehow managed to set consent non-personalised ads only (very unlikely under the current conditions with the official AdMob popup), the Dictionary will look like this:

```
{"ADS_STATUS": 1,
"MISSING_PERSONALISED_CONSENT": [3, 4]}
```

since the only real difference between personalised and non-personalised ad consent is having given consent to Purposes 3 and 4.



#### Read info for all consent purposes
If are interested in knowing in greater detail what purpose has what kind of consent, including those that are not included in the above checks (Purposes 5, 6 and 8) the following method will return the user's choices in a human *and* machine readable format (although while machines would probably prefer this, humans might frown at it):

```
consentParser.getRawConsentStatusForAllPurposes() -> Dictionary
```

This method will return a dictionary with 11 keys, which are the numerical representation for each purpose (1-10), and "GV" for Google vendor.

For each key, the value will be an `int` `Array`, where the first value (0th index) represents the status of user consent (1-for granted, 0-for denied), and the second value (1st index) represents the status of legitimate interest (1-for granted, 0-for denied).

For example, for "Purpose 2 - Select basic ads", you might see something like this, depending on the user's choice:

```
# User granted both consent and legitimate interest:
"2": [1, 1]

# User denied consent but granted legitimate interest:
"2": [0, 1]

# User granted consent but denied legitimate interest:
"2": [1, 0]

# User denied both consent and legitimate interest:
"2": [0, 0]

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

Or like this if the user has somehow managed to give the absolute minimal consent to show non-personalised ads:

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
- **Legitimate interest for Consent purposes 1,3 and for 4 is not currently applicable**. For simplicity's sake, those fields will be set to whatever the consent status is.
- **Purposes 5, 6 and 8 are not taken into consideration** in showing either personalised and non-personalised ads, but I have included them here for completeness' sake, and for anyone interested in them for any reason.


#### Read info for any single purpose

If you are only interested in a specific purpose's consent status, you can use the following function. It will return a Dictionary with a single key, corresponding to your query, in the same format as above. 

(*The original reason for using a Dictionary was the limitation on the plugin system's return data types, but it turned out to also be more concise this way.*)

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
# get consent status for "Google Advertising Products" vendor 
consentParser.getRawConsentStatusForSinglePurpose(0)

# Output for full consent and legitimate itnerest:
{"GV": [1, 1]}
```

If you pass a number larger than 10 as an argument, the Dictionary will return an Index Out of Bounds error as the key. 

```
# get consent status for an invalud purpose number
consentParser.getRawConsentStatusForSinglePurpose(11)

# The output constains an error key and a String as a value for details:
{"IDX_OOB_ERROR": "Index should be between 0 and 10: 0 for Google vendor consent, 1-10 for any named purposes"}
```

For sanity's sake, you should always check the returned dictionary for `IDX_OOB_ERROR` key, before parsing.


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

- The plugin is currently only packaged for Godot 3.5, as already mentioned above. If you have experience with Godot 4.2, and feel like helping out, please push a packaged version to the `builds` folder, and open a PR. 
- If you have suggestions on how to make this README more readable, don't hesitate to chime in with any constructive criticism, edits, etc. :)

Any help is most welcome.