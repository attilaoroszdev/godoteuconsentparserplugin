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

If either or both of the above methods have returned False, and you'd like to investigate why, the following two methods will return the user's choices in a human *and* machine readable format (although while machines would probably prefer this, humans might frown at it):

==***ToDo: Finish this***==

