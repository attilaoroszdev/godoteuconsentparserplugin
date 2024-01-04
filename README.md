# EU GDPR Consent String Parser Android Plugin for Godot

Google's enforcement of the EU's GDPR consent requirements begins at 16 January 2024. If you use AdMob in your Android game (or any Android project made in Godot), you need to start implementing the consent form.

Fortunately, setting up a consent form in AdMob is quite easy, and both major AdMob plugins support an easy implementation.

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

What the support article fails to mention however, is that under "**Vendor preferences**", the vendor named "**Google Advertising Products**" has to be given explicit consent for all this to work.

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

## Usage

Once installed, you should be able to obtain the singleton called `EUConsentStringParser` in any script, like this:

```
if Engine.has_singleton("EUConsentStringParser"):
	var consentParser = Engine.get_singleton("EUConsentStringParser")
```

The singleton exposes four methods to Godot. Two for convenience, two for digging the data a bit deeper.

### Convenience methods

You can call these two methods directly on the singleton, to know if ads are good to go:

```
consentParser.canShowAds() -> bool
```

Will return a simple true or false answer, letting you know **if the user has selected the absolute minimal options to show any kind of ads at all** (not necessarily personalised)

```
consentParser.canShowPersonalizedAds() -> bool
```  

Will return a simple true or false answer, letting you know **if the user has selected the absolute minimal options to personalised ads** (It dpoes not check all available purposes, only the necessary ones)


### Consent string parser methods

If either or both of the above methods have returned False, and you'd like to investigate why, the following two methods will return the user's choices in a human readable format:

#### Reading the status of a **single** consent purpose

To read the current consent status for any purpose, identified by its number (see blow), you can use the following method:

```
consentParser.getRawConsentStatusForSinglePurpose(idx:int) -> Dictionary
```

Where `idx` is an integer `1..10`, for Purposes 1 to 10, or `0` for the "Google Advertising Products" vendor's status

The returned dictionary will have a single key value pair. (The reason for using a dictionary was that the Android Plugin System v1 supports very limited datatypes). 

The key is the `idx` you just passed, i.e. the Purposes index (1-10), and the value is an Object array, containing:
* the Purpose's full name, as defined by the law (again, see below), 
* a boolean marking whether explicit consent was given for that purpose, 
* and either another boolean where "legitimate interest" is applicable, or the string "N/A", where it is not.

If it sounds more complicated than it really is, that's entirely my fault, so here are two examples that will make more sense:


##### Example 1: A Purpose with *no* "legitimate interest" option
For purposes 1, 3 and 4, "legitimate interest" does not apply, so the dictionary will return with the name, a boolean (whetheror not the user consented), and the string N/A to show how meaningless asking for "legitimate interest" is for those purposes:

```
consentParser.getRawConsentStatusForSinglePurpose(4)
```

will return a dictionary like so, in case the user **consented to Purpose 4**:

```
{4:[Purpose 4 - Select personalised ads, True, N/A]}
```

or a dictionary like so, in case the user **did *not* consent to Purpose 4**:

```
{4:[Purpose 4 - Select personalised ads, False, N/A]}
```


##### Example 2: A Purpose *with* a "legitimate interest" option:

For all other purposes (2,5,6,7,8,9,10), "legitimate interest" is also an option besides granting full consent, and this is often also sufficient for ads to work. Note that for these types of Purposes, the dictionary now has a **second boolean** value, indicating the status of "legitimate interest":


```
consentParser.getRawConsentStatusForSinglePurpose(8)
```

will return a dictionary with a single key "8", meaning Purpose 8.

If  the user **allowed both "Consent", and "Legitimate interest"** for Purpose 8, the returned dictionary will look like this:

```
{8:[Purpose 8 - Measure content performance, True, True]}
```


In case **the user denied "Consent"**, but  **allowed "Legitimate interest"** for Purpose 8, the returned dictionary would look like this:

```
{8:[Purpose 8 - Measure content performance, False, True]}
```

 
In case the user **allowed "Consent"**, but **denied "Legitimate interest"** for Purpose 8,  the returned dictionary would look like this:

```
{8:[Purpose 8 - Measure content performance, True, False]}
```


In case the user **denied both "Consent", and "Legitimate interest"** for Purpose 8, the returned dictionary would look like this:

```
{8:[Purpose 8 - Measure content performance, False, False]}
```

Sounds complicated? Yeah, thank the EU lawmakers.… But wait, there is more (worse) to come.


#### Reading the status of the *Google Advertising Products* vendor

Kind of out of place among all the fancy purposes, but Google's sens self importance is high enough that this is kind of important to check, so here we go.

To check for the required vendor consent, you can use the same method as above, but pass `0` as an argument:

```
consentParser.getRawConsentStatusForSinglePurpose(0)
```

**If this method returns anything other than below, ads will *NOT* be served** (Has to be true for both consent and "legitimate interest")

```
 Google-vendor:[Google Advertising Products, True, True]
```

So yeah, basically "true, true" is good, anything else else is "bad, bad".


#### Reading the status **all** consent purposes **at once**

There is also a method that will return all the available purposes and their statuses, with some extra info. This is kind of a convenience method among these two, but not *as* convenient to use as the two that just told you everything was fine and dandy, so it's not listed there. (*In fact, it's not very convenient* at all *, but can be rather useful, still.*)

```
consentParser.getRawConsentStatusForAllPurposes()
```

will return a `Dictionary` that at least looks like its being a `Dictionary` at least makes *some* sense. Here's an example output:

```
{1:[Purpose 1 - Store and/or access information on a device, True, N/A], 
2:[Purpose 2 - Select basic ads, True, True], 
3:[Purpose 3 - Create a personalised ads profile, True, N/A], 
4:[Purpose 4 - Select personalised ads, True, N/A], 
5:[Purpose 5 - Create a personalised content profile, True, False], 
6:[Purpose 6 - Select personalised content, True, False], 
7:[Purpose 7 - Measure ad performance, True, True], 
8:[Purpose 8 - Measure content performance, True, True], 
9:[Purpose 9 - Apply market research to generate audience insights, True, True], 
10:[Purpose 10 - Develop and improve products, True, True], 
Google-vendor:[Google Advertising Products, True, True],
Info:This is raw data. For more convenient checks use one of the canShow...() methods}
```

The last entry `Info:This is raw data. For more convenient checks use one of the canShow...() methods` is just there to remind you