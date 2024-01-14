# How to use this wrapper script
### *(with Shin-Nil's AdMob plugin)*

The wrapper script should be annotated enough, but if you're new to this, or already feel confused enough about the while AdMob/consent situation (it *is* super-confusing), the below little guide will provide at east a good starting point.

The will be explained in context of using it with [Shin-Nil's AdMob plugin](htps://github.com/Shin-NiL/Godot-Android-Admob-Plugin), but it should be easy enough to apply any of this to any other method or plugin of pbtaining consent (as they should have the same or similar workflows).

## Prerequisites

This is not meant to be a complete "how to use admob" guide, focusing only the consent side of thigns, and especially on how this plugin can be used to make sense of it. There will be a number of pre-requisites, that I cannot currently help you with. (I might publish something more exhaustive in the future on some platform or another, but it's best to keep things on point here.)

- You have configured your AdMob account and set up the GDPR consent message correctly
- You have Shin-Nil's AdMob plugin installed and configured
- You have installed this plugin in your Godot project, and the singleton loads correctly
- You are auto-loading the same exact `GDPRTools.gd` found in this directory (or at least have not changed existing method names)

If all these are good, you can start using the wrapper script, and finally make sense of your users' GDPR choices… kind of.


**==DISCLAIMER: NON OF THIS IS LEGAL ADVICE, AND SHOULD NOT BE TAKEN AS SUCH. I AM AS CLUELESS ABOUT THE WHOLE GDPR THING'S TRUE LEGAL CONSEQUENCES AS THE NEXT PUNTER. ALL OF THIS TEXT IS MERELY A *TECHNICAL MANUAL* FOR THIS WHO WABT TO KNOW HOW TO IMPLEMENT STUFF==**

## Usage

### How to obtain consent with Shin-Nil's AdMob plugin

First, you need to connect the following two signals from the AdMob plugin (you can of course use the editor to do this, it's easier, but here's the code for completeness' sake):

```
func _ready():
    $AdMob.connect("consent_info_update_failure", self, "_on_AdMob_consent_info_update_failure")
    $AdMob.connect("consent_app_can_request_ad", self, "_on_AdMob_consent_app_can_request_ad")

...

func _on_AdMob_consent_info_update_failure(error_code, error_message):
    # Called if AdMob cannot veryfy the consent srtatus online, for any reason.
    # Note that if a previous consent exists (it gets saved on the user's debvice), Admob will use 
    # the information stored and just get on with things. You had, so far, no way to know either way
    # (More on this later)
    #
    # error_code will be an int. 
    # error_message will be a human readable reason for the failure
    pass
    
func _on_AdMob_consent_app_can_request_ad(consent_status):
    # This is called when AdMob verified the consent status, either by dospélaying the consent popup and
    # obtaining consent, verifying stuff online, or reading the consentz string from the user's device.
    #
    # consent_status will be an int, where the two interesting values are:
    # consent_status == 1 # Means that no consent is necessary, the user is outside of the EU/EEA, everythign will work
    # consent_status == 3 # means that consent from the user was obtained. 
    #
    # Unfortunately, eve if consent was obtained, we have no idea what tht means in practice, whether the user gave 
    # enough consent to display ads or not. So sure, "app can request ad", but there's no guarantee it will get any 
    # (This is Google's phrasing, don1t hate Snin-Nil for it.)
    pass
```

Once you've set that up, you can (and must) check for consent. Put the below code wherever you want the consent popup to appear in your app:

```
# Verify consent status, and display consent popup if necessary
$AdMob.request_consent_info_update()
```

If you've set everything up right, this will present EU/EEA users with the consent popup you've set up in your AdMob account, or just silently return a status 1 for non EU/EEA users.

**IMPORTANT:** As per Google's recommendation, you will need to actively check this every time the app starts, because some consents can and will expire with time

And that's all there is for obtaining consent from your users. But what can you do with it?

### Reading and parsing the consent messages

The  `GDPRTools.gd` script you have auto-loaded provides some easy-to-use methods to make sense of the consent message. 

***Yeah, but why do I need this?***
You might not. Or you just want to be sure. Or maybe you rely on rewarded video ads for some game functionality (continue after a level fail, get some exrta items, whatever.) Honestly, it was the latter case that made me write this whole plugin, because I had no idea how to handle EU user mishaps.

I have already outlined this in greater detail in the main README file, but here's a recap: It's very easy for the user to accidentally disable ads in the game. You would never know what happened to the ad revenue, and the players might get frustrated if things don1t work as expected.

***And what can I do with the information thus obtained***
Two things, mainly:

- Inform your users about how and how stuff might not work in the absence of consent, giving detailed information about the issue
- Conditionally change your game logic /flow in the absence of consent, providing the user with alternatives or
- Collect statistical data about consent. ==***CAREFUL***== now, this is treacherous ground. Whether you ***can*** collect any data is a very complex question. You are on you own with this one

Now that we got the obvious out of the way, let's start for real…

#### In case the consent check fails

If, for any reason, the AdMob plugin could not check the consent status online, it will fall back on any previously obtained consent, which is stored on the user's device, and use that. You might be interested to know this, so you can apply some gentle pressure (e.g. display some message about it) to your user.

Probably the best place to do this is in then the method `_on_AdMob_consent_info_update_failure` with the `GDPRToolsprevious_consent_string_exists()` method

```
func _on_AdMob_consent_info_update_failure(error_code, error_message):
    if not GDPRTools.previous_consent_string_exists():
        # Do something about it
```

***OK, but what*** **can** ***I do about it?***

- Check, if the user is in the EU/EEA, and tell them you need their consent
- Adjust your logic/flow to not load any ads

If you have access to the user's geo-location, this should be easy enough. If not, you can either request an approximate location and check (out of the scope of this writing), or, as a *very coarse checkup*, you can just check if the device's locale is in one of the affected countries. In the latter case, there are a number of issues (user's phone is set to the wrong country), they are abroad, etc, but it1s still better than nothing, if you don1t want to requets lcoation access for some reason:

```
const GDPR_COUNTRY_CODES:Array = ["AT", "BE", "BG", "CY",  "CH", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", 
 "GB", "GR", "HR", "HU", "IE","IS", "IT","LI", "LT", "LU", "LV", "MT", "NL","NO", 
 "PL", "PT", "RO", "SE", "SI", "SK"]

func _on_AdMob_consent_info_update_failure(error_code, error_message):
    if not GDPRTools.previous_consent_string_exists():        
        var raw_locale = OS.get_locale()
        var country_code:String = raw_locale.substr(raw_locale.find("_")+1,2)
		if country_code in GDPR_COUNTRY_CODES:
            # Do somethign about it
```

(This is not currently part of the script, because it's really a very rough workaround)

#### In case the consent was obtained, check if it was sufficient

If you do have consent, you can use the following methods, to check if you1re good to go for showing ads at all. You might or might not want to put these into `_on_AdMob_consent_app_can_request_ad`, but I'll be putting them there for simplicity's sake, since ti1s the first place most of it starts to make sense.

```
func _on_AdMob_consent_app_can_request_ad(consent_status):
    # Check if consent is even necessary
    if GDPRTools.is_consent_needed():
        # User is in the EU/EEA, consent is absolutely necessary.
        # Now let's see if you have it
        if GDPRTools.can_show_personalised_ads():
            # The user has consented to verything
        elif GDPRTools.can_show_any_ads():
            # You can show at least non-personalised ads, you're mostly good to go
            # but might see some no-fill isues
        else:
            # You don't have sufficient consent to show ads (more on this later)
    else:
        # User is outside of EU/EEA, consent requirements do not apply
```

Alternatively if you want to check for both with a single method, you can use:

```
func _on_AdMob_consent_app_can_request_ad(consent_status):
    # Check if consent is even necessary
    if GDPRTools.is_consent_needed():        
        var consent_status:Array = GDPRTools.is_consent_sufficient()
        if consent_status[1]:
            # You can show personalised ads
        elif consent_status[0]:
            # You can show at least non-personalised ads, you're mostly good to go
            # but might see some no-fill isues
        else:
            # You don't have enugh consent to show any ads
    else:
        # User is outside of EU/EEA, consent requirements do not apply
```

If you've got the green light here, or the user doesn't need to give consent, you can start loading ads as you normally would.

**Note: According to Google, you can still try to load ads, even if there's no sufficient consent,and they will try to serve "Limited ads", but that request is very unlikely to be filled by AdMob. Still, it might be worth a try. In that case, you might want to do these checks to where you catch the signal of ads failing to load, to know why it happened...**

#### In case the consent was obtained, but was not sufficient

There are a number of methods you can use to see what exact consent is missing, for which scenario.

[According to Google](https://developers.google.com/admob/android/privacy/ad-serving-modes), the minimum consent necessary for ads to show at all are: 

- **The end user grants Google consent to:**
    - Store and/or access information on a device (Purpose 1)
- **Legitimate interest (or consent, where a publisher configures their CMP to request it) is established for Google to:**
    - Select basic ads (Purpose 2)
    - Measure ad performance (Purpose 7)
    - Apply market research to generate audience insights (Purpose 9)
    - Develop and improve products (Purpose 10)

To be able to show personalised ads, in addition to the above:

- **The end user grants Google consent to:**
    - Create a personalized ads profile (Purposes 3)
    - Select personalized ads (Purposes 4)

***BUT, OF COURSE, THERE'S A CATCH!***

And this is only passingly mentioned in the above linked article (and not mentioned elsewhere at all.)

**Under *"Vendor preferences"*, the vendor named *"Google Advertising Products"* has to be given explicit consent for all this to work**, which is not  problem if the user consented to everything, but might prevent them to choose non-personalised ads only. (The way this is implemented looks almost like a deliberate attempt to prevent the user from doing just that...)

In the examples below, the purpose ID will be used a lot. by this I mean the number of the purpose, for example for Purpose 1, the ID will 1, for Purpose 2, the ID will be 2, etc.

Like so:

```
# ID for purpose Store and/or access information on a device (Purpose 1)
const id:int = 1
```

None of this is part of the script, there are ni constants or enums for this, simply because it's very easy to relate e.g. Purpose 1 with the integer 1. :)

***OK, but what*** **can** ***I do? how will I even know?***
Glad, you asked. This is the main purpose of this plugin, call it Purpose 0 if you will (OK, that was the worst dad-joke every and I will stop).

##### Get the missing consent IDs (categorised) to process in any way you like in your code

To extend the above examples, we will add some more methods you can use to read the consents (unnecessary checks are now expluded for brevity)

```
var missing_consent_ids:Array = []
var missing_consent_or_legit_interest_ids:Array = []
var missing_vendor_consent:bool = false
var missing_personalised_consents:Array = []

func _on_AdMob_consent_app_can_request_ad(consent_status):
    if GDPRTools.is_consent_needed():
       if not GDPRTools.can_show_any_ads():
            # You do not have sufficient consent to show any ads.
            # First, we need to check wether you have sufficient consent, where legitimate interest 
            # is not applicable. 
            # The returned Array will contain The returned int Array will contain the ID(s) of the mising purpose(s)
            # most like a single integer for Purpose 1: [1]
            missing_consent_ids = GDPRTools.get_denied_mandatory_consents() 
            
            # Next, we need to check for Purposes where consent OR at least legitimate interest is missing
            # the returned int Array will might contain any or all of the following: [2, 7, 9, 10]
            missing_consent_or_legit_interest_ids = GDPRTools.get_denied_consent_or_legit_interests()
            
            # Finally, we neef to know, is Google vendor consent was suigfficient. Sinc both consent
            # And legitimate interest is necessary, it will returns a simple boolean `true` means consent is missing
            missing_vendor_consent = GDPRTools.is_missing_vendor_consent()
       
       elif not GDPRTools.can_show_personalised_ads():
            # The user did not consent to personalised ads
            # In this case, we only need to know whether cnsent for Purpose 3, Purpose 4 or both
            # is misisng. The returned int Array will contain the ID(s) of the mising purpose(s)
            # (None of the other methods apply here, since we already checked those, but youmight need them if you do it a different way)
            missing_personalised_consents = GDPRTools.get_denied_personalised_consents()
```

Alternatively, you can obtain all of the above with a single method call:

```
var missing_consent_ids:Array = []
var missing_personalised_consents:Array = []
var missing_consent_or_legit_interest_ids:Array = []
var missing_vendor_consent:bool = false


func _on_AdMob_consent_app_can_request_ad(consent_status):
    if GDPRTools.is_consent_needed():
       if not (GDPRTools.can_show_any_ads() or GDPRTools.can_show_personalised_ads()):
           var missing_consents:Array = get_all_consent_issues() 
           missing_consent_ids = missing_consents[0]
           missing_personalised_consents = missing_consents[1]
           missing_consent_or_legit_interest_ids = missing_consents[2]
           missing_vendor_consent = missing_consents[3]
           
```

So now we have the IDs of any and all missing purposes. What do we do with them? Like, literally, whatever the law allows you. get the logs (if you legally can), or display some user messages, etc. You can manually parse all of these, if you want or just use one of the helper methods that'll do this for you:


##### Easily build human-readable messages for your users

If you want to easily construct informative messages for your users, telling them what exactly went wrong, the following methods have your back. If that's all you want to do, this is all you need. All the above methods were focused on returning consent data in a machine readable way, bit these ones are for human eyes mostly:

```
const user_message String = "Sorry mate, I cannot show you any ads, and stuff like rewards might not work. Here's why:\n%s"

func _on_AdMob_consent_app_can_request_ad(consent_status):
    if GDPRTools.is_consent_needed():
        # Can't show ads for any reason
        # of course, you coudl fine tune it to show doifferent messages in either case, 
        # This shoudl be enough to give you an diea how it worksbut 
        if not (GDPRTools.can_show_any_ads() or GDPRTools.can_show_personalised_ads()):
            tell_my_user_wassup()
  
# Construct a user mesasge String
func tell_my_user_wassup():
    # This method will return all the missing consents's full names, plus what's missing, 
    # as an Array of simple Strings
    # For example, if the user pressed "Manage my choices", the just accepted the default,
    # the Array would contain the following Strings:
    #
    # ["Purpose 1 - Store and/or access information on a device: missing consent",
    # "Purpose 3 - Create a personalised ads profile: missing consent for personalised ads",
    # "Purpose 4 - Select personalised ads: missing consent for personalised ads",  
    # "Google Advertising Products: missing vendor consent"]
    var consent_issues:Array = GDPRTools.get_all_consent_issues_as_text()
    
    # Now use that array to build and show your user message
    var combined_issues_message:String = ""
    for issue in consent_issues:
        combined_issues_message += issue +"\n"
    
    show_dialogue(user_message % combined_issues_message)


func show_dialogue(message_text:String):
    # Do whatever you need to do
    ...
```

There are other methods to tailor these messages, such as:

```
# If you don't care about why personalised ads are not showing, 
# but you want to inform your user why NO ADS are showing
GDPRTools.get_limited_consent_issues_as_text() 

# If you don't care about why personalised ads are not showing, 
# but you wan to inform your user why NO ADS are showing,
# and you want it formatted with bb_code
GDPRTools.get_limited_consent_issues_as_bbcode_text()

# If you ONLY care about why personalised ads are not showing, 
# and you want to inform your user why
GDPRTools.get_personalised_consent_issues_as_text()
```

these methods will return similar String Arrays you can directly use.

An this concludes the most basic use-cases.

### Other methods (ToDo)

There are plenty of other methods in the script, which I will cover at some point, later on. For now, I really need to focus on implementing this all in my own game, and finally publishing an update.