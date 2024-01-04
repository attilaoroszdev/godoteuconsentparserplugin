package com.attilaoroszdev.euconsentstringparser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.UsedByGodot;
import java.util.ArrayList;
import java.util.List;

public class ParseConsentString extends org.godotengine.godot.plugin.GodotPlugin {
    private static final String TAG = "ParseConsentString";
    // Name string for purpose cases
    private final String[] purposeTitles =
            new String[]{"Misc: Google vendor consent",
                    "Purpose 1 - Store and/or access information on a device",
                    "Purpose 2 - Select basic ads",
                    "Purpose 3 - Create a personalised ads profile",
                    "Purpose 4 - Select personalised ads",
                    "Purpose 5 - Create a personalised content profile",
                    "Purpose 6 - Select personalised content",
                    "Purpose 7 - Measure ad performance",
                    "Purpose 8 - Measure content performance",
                    "Purpose 9 - Apply market research to generate audience insights",
                    "Purpose 10 - Develop and improve products"
            };

//    Works in plugin v1, not in v2
    private Context context = this.getGodot().getContext();
//    In case you want to export it for v2 (Godot 4.2)
//    private Context context = this.getActivity();
    public ParseConsentString(Godot godot) {
        super(godot);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "EUConsentStringParser";
    }

    /**
     * The original methods from https://itnext.io/android-admob-consent-with-ump-personalized-or-non-personalized-ads-in-eea-3592e192ec90
     * adapted to be used as a Godot Android plugin
     */
    private boolean hasAttribute(String input, int index) {
        if (input == null) return false;
        return input.length() >= index && input.charAt(index-1) == '1';
    }

    private boolean hasConsentFor(List<Integer> indexes, String purposeConsent, boolean hasVendorConsent) {
        for (Integer p: indexes) {
            if (!hasAttribute(purposeConsent, p)) {
                Log.e(TAG, "hasConsentFor: denied for purpose #" + p );
                return false;
            }
        }
        return hasVendorConsent;
    }

    /**
     * These methods will always assume that Google  has consent and legitimate interest set, in the vendor list
     * since it's part of the minimum requirements for showing ads. Great for simple yes/no checks
     */

    private boolean hasConsentOrLegitimateInterestFor(List<Integer> indexes, String purposeConsent, String purposeLI, boolean hasVendorConsent, boolean hasVendorLI){
        for (Integer p: indexes) {
            boolean purposeAndVendorLI = hasAttribute(purposeLI, p) && hasVendorLI;
            boolean purposeConsentAndVendorConsent = hasAttribute(purposeConsent, p) && hasVendorConsent;
            boolean isOk = purposeAndVendorLI || purposeConsentAndVendorConsent;
            if (!isOk){
                Log.e(TAG, "hasConsentOrLegitimateInterestFor: denied for #" + p);
                return false;
            }
        }
        return true;
    }


    @UsedByGodot
    public boolean canShowAds(){
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents","");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        List<Integer> indexes = new ArrayList<>();
        indexes.add(1);

        List<Integer> indexesLI = new ArrayList<>();
        indexesLI.add(2);
        indexesLI.add(7);
        indexesLI.add(9);
        indexesLI.add(10);

        return hasConsentFor(indexes, purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI);

    }

    @UsedByGodot
    public boolean canShowPersonalizedAds(){
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents","");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        List<Integer> indexes = new ArrayList<>();
        indexes.add(1);
        indexes.add(3);
        indexes.add(4);

        List<Integer> indexesLI = new ArrayList<>();
        indexesLI.add(2);
        indexesLI.add(7);
        indexesLI.add(9);
        indexesLI.add(10);

        return hasConsentFor(indexes, purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI);

    }


    /**
     * New additions for obtaining raw consent info for all 10 main purposes
     * Google vendor consent + LI checks were removed to always return the correct status, even if
     * vendor consent or LI are false
     */

    private boolean hasConsentForSinglePurpose(int index, String purposeConsent){
        return hasAttribute(purposeConsent, index);
    }

    private boolean hasLIForSinglePurpose(int index, String purposeLI){
        return hasAttribute(purposeLI, index);
    }

    /**
     * Creates a Godot compatible dictionary from all purposes, with the addition of the Google vendor flags
     * Legitimate interest is not applicable for purposes 1, 3 and 4
     *
     * @return org.godotengine.godot.Dictionary, where the key is the purpose's index, followed by a
     * simple Object array of the Purposes full name, consent status (boolean), and LI status (boolean where applicable)
     * The last entries are for informational purposes
     */
    @UsedByGodot
    public org.godotengine.godot.Dictionary getRawConsentStatusForAllPurposes(){

        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents","");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();
        dict.put("1", new Object[]{purposeTitles[1], hasConsentForSinglePurpose(1, purposeConsent), "N/A"});
        dict.put("3", new Object[]{purposeTitles[3], hasConsentForSinglePurpose(3, purposeConsent), "N/A"});
        dict.put("4", new Object[]{purposeTitles[4], hasConsentForSinglePurpose(4, purposeConsent), "N/A"});
        dict.put("2", new Object[]{purposeTitles[2], hasConsentForSinglePurpose(2, purposeConsent), hasLIForSinglePurpose(2, purposeLI)});
        dict.put("5", new Object[]{purposeTitles[5], hasConsentForSinglePurpose(5, purposeConsent), hasLIForSinglePurpose(5, purposeLI)});
        dict.put("6", new Object[]{purposeTitles[6], hasConsentForSinglePurpose(6, purposeConsent), hasLIForSinglePurpose(6, purposeLI)});
        dict.put("7", new Object[]{purposeTitles[7], hasConsentForSinglePurpose(7, purposeConsent), hasLIForSinglePurpose(7, purposeLI)});
        dict.put("8", new Object[]{purposeTitles[8], hasConsentForSinglePurpose(8, purposeConsent), hasLIForSinglePurpose(8, purposeLI)});
        dict.put("9", new Object[]{purposeTitles[9], hasConsentForSinglePurpose(9, purposeConsent), hasLIForSinglePurpose(9, purposeLI)});
        dict.put("10", new Object[]{purposeTitles[10], hasConsentForSinglePurpose(10, purposeConsent), hasLIForSinglePurpose(10, purposeLI)});
        dict.put("Google-vendor", new Object[]{purposeTitles[0], hasGoogleVendorConsent, hasGoogleVendorLI});
        dict.put("Info", "This is raw data. For more convenient checks use one of the canShow...() methods");

        if (!hasGoogleVendorConsent){
            dict.put("Consent-warning", "Since Google vendor consent was not obtained, no ads will be shown regardless of other consent settings");
        }
        if (!hasGoogleVendorLI){
            dict.put("LI-warning", "Since Google vendor Legitimate Interest was not obtained, no ads will be shown regardless of other consent settings");
        }

        return dict;

    }

    /**
     * Creates and returns a Godot compatible dictionary with a single entry for a single purpose, by index, where index is 1-10
     * Additionally, the Google vendor purpose statuses can be requested with a value of 0.
     * If the index is out of range, the dictionary will contain an error message
     *
     * Legitimate interest is not applicable for purposes 1, 3 and 4
     *
     * (The reason for using Dictionary is that the Godot Android plugin system supports very limited return datatypes
     * @param index The numerical index of the Purpose, 1-10, or 0 for Google vendor status
     * @return org.godotengine.godot.Dictionary with one key, where the key is the purpose's index, followed by a
     * simple Object array of the Purposes full name, consent status (boolean), and LI status (boolean where applicable)
     */
    @UsedByGodot
    public org.godotengine.godot.Dictionary getRawConsentStatusForSinglePurpose(int index) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");

        List<Integer> basicConsentIds = new ArrayList<>();
        basicConsentIds.add(1);
        basicConsentIds.add(3);
        basicConsentIds.add(4);

        List<Integer> consentOrLIIds = new ArrayList<>();
        consentOrLIIds.add(2);
        consentOrLIIds.add(5);
        consentOrLIIds.add(6);
        consentOrLIIds.add(7);
        consentOrLIIds.add(8);
        consentOrLIIds.add(9);
        consentOrLIIds.add(10);

        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();
        if (basicConsentIds.contains(index)) {
            dict.put(String.valueOf(index), new Object[]{purposeTitles[index], hasConsentForSinglePurpose(index, purposeConsent), "N/A"});
        } else if (consentOrLIIds.contains(index)) {
            String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "");
            dict.put(String.valueOf(index), new Object[]{purposeTitles[8], hasConsentForSinglePurpose(index, purposeConsent), hasLIForSinglePurpose(index, purposeLI)});
        } else if (index == 0) {
            String vendorConsent = prefs.getString("IABTCF_VendorConsents", "");
            String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "");
            int googleId = 755;
            boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
            boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);
            dict.put("google-vendor", new Object[]{purposeTitles[0], hasGoogleVendorConsent, hasGoogleVendorLI});
        } else {
            Log.e(TAG, "Invalid consent index");
            dict.put("Invalid-index", "Index should be between 0 and 10: 0 for Google vendor consent, 1-10 for named purposes");
        }
        return dict;
    }

}
