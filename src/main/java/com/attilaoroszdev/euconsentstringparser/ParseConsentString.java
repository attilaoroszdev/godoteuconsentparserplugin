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
    private SharedPreferences prefs;
    private List<Integer> deniedPurposes = new ArrayList<>();
    private List<Integer> deniedFlexiblePurposes = new ArrayList<>();

//    Works in plugin v1, not in v2
    private Context context = this.getGodot().getContext();
//    In case you want to export it for v2 (Godot 4.2)
//    private Context context = this.getActivity();
    public ParseConsentString(Godot godot) {
        super(godot);
        prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "EUConsentStringParser";
    }


    /******************************************
     *                                        *
     *  Private methods used to check things  *
     *                                        *
     ******************************************/


    /**
     * ToDo
     * @param input
     * @param index
     * @return
     */
    private boolean hasAttribute(String input, int index) {
        if (input == null) return false;
        return input.length() >= index && input.charAt(index-1) == '1';
    }

    /**
     * ToDo
     * @param indexes
     * @param purposeConsent
     * @param hasVendorConsent
     * @return
     */
    private boolean hasConsentFor(List<Integer> indexes, String purposeConsent, boolean hasVendorConsent) {
        for (Integer p: indexes) {
            if (!hasAttribute(purposeConsent, p)) {
                deniedPurposes.add(p);
                Log.e(TAG, "hasConsentFor: denied for purpose #" + p );
            }
        }
        if (deniedPurposes.size() > 0) return false;
        return hasVendorConsent;
    }

    /**
     * These methods will always assume that Google  has consent and legitimate interest set, in the vendor list
     * since it's part of the minimum requirements for showing ads. Great for simple yes/no checks
     */

    /**
     * ToDo
     * @param indexes
     * @param purposeConsent
     * @param purposeLI
     * @param hasVendorConsent
     * @param hasVendorLI
     * @return
     */
    private boolean hasConsentOrLegitimateInterestFor(List<Integer> indexes, String purposeConsent, String purposeLI, boolean hasVendorConsent, boolean hasVendorLI){
        boolean isOK = true;
        for (Integer n: indexes) {

            boolean hasPurposeLI = hasAttribute(purposeLI, n);
            boolean hasPurposeConsent = hasAttribute(purposeConsent, n);
            boolean purposeAndVendorLI = hasPurposeLI && hasVendorLI;
            boolean purposeConsentAndVendorConsent = hasPurposeConsent && hasVendorConsent;

            if (!(hasPurposeConsent || hasPurposeLI)){
                Log.e(TAG, "Added to deniedFlexibles: #" + n);
                deniedFlexiblePurposes.add(n);
            }

            if (!(purposeAndVendorLI || purposeConsentAndVendorConsent)) {
                isOK = false;
                Log.e(TAG, "hasConsentOrLegitimateInterestFor: denied for #" + n);
            }

        }

        return isOK;
    }

    /**
     * ToDo
     * @param index
     * @param purposeConsent
     * @return
     */
    private int hasConsentForSinglePurpose(int index, String purposeConsent){
        return hasAttribute(purposeConsent, index) ? 1 : 0;

    }

    /**
     * ToDo
     * @param index
     * @param purposeLI
     * @return
     */
    private int hasLIForSinglePurpose(int index, String purposeLI){
        return hasAttribute(purposeLI, index) ? 1 : 0;
    }

    /**
     * ToDo
     * Can't do stream().mapToInt on the List, because minSDK < 24
     * @param list
     * @return
     */
    private int[] listToIntArray(List<Integer> list){
        int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++){
            result[i] = list.get(i);
        }
        return result;
    }



    /*************************************
     *                                   *
     *  Public methods exposed to Godot  *
     *                                   *
     *************************************/


    /**
     * ToDo
     * @return
     */
    @UsedByGodot
    public boolean consentIsNeeded(){

        int gdprApplies = prefs.getInt("IABTCF_gdprApplies", 0);
        return gdprApplies == 1;
    }

    /**
     * ToDo
     * @return
     */
    @UsedByGodot
    public boolean canShowAds(){
        deniedPurposes.clear();
        deniedFlexiblePurposes.clear();
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

        boolean consentOK = hasConsentFor(indexes, purposeConsent, hasGoogleVendorConsent);
        boolean legitimateInterestOK = hasConsentOrLegitimateInterestFor(indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI);

        if (!consentIsNeeded()) return true;
        return consentOK && legitimateInterestOK;
    }

    /**
     * ToDo
     * @return
     */
    @UsedByGodot
    public boolean canShowPersonalizedAds(){
        deniedPurposes.clear();
        deniedFlexiblePurposes.clear();
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

        boolean consentOK = hasConsentFor(indexes, purposeConsent, hasGoogleVendorConsent);
        boolean legitimateInterestOK = hasConsentOrLegitimateInterestFor(indexesLI, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI);

        if (!consentIsNeeded()) return true;
        return consentOK && legitimateInterestOK;
    }


    /**
     * ToDo
     */
    @UsedByGodot
    public org.godotengine.godot.Dictionary getRawConsentStatusForAllPurposes() {
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents", "");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "");

        int googleId = 755;
        int hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId) ? 1 : 0;
        int hasGoogleVendorLI = hasAttribute(vendorLI, googleId) ? 1 : 0;

        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();
        dict.put("1", new int[]{hasConsentForSinglePurpose(1, purposeConsent), hasConsentForSinglePurpose(1, purposeConsent)});
        dict.put("3", new int[]{hasConsentForSinglePurpose(3, purposeConsent), hasConsentForSinglePurpose(3, purposeConsent)});
        dict.put("4", new int[]{hasConsentForSinglePurpose(4, purposeConsent), hasConsentForSinglePurpose(4, purposeConsent)});
        dict.put("2", new int[]{hasConsentForSinglePurpose(2, purposeConsent), hasLIForSinglePurpose(2, purposeLI)});
        dict.put("5", new int[]{hasConsentForSinglePurpose(5, purposeConsent), hasLIForSinglePurpose(5, purposeLI)});
        dict.put("6", new int[]{hasConsentForSinglePurpose(6, purposeConsent), hasLIForSinglePurpose(6, purposeLI)});
        dict.put("7", new int[]{hasConsentForSinglePurpose(7, purposeConsent), hasLIForSinglePurpose(7, purposeLI)});
        dict.put("8", new int[]{hasConsentForSinglePurpose(8, purposeConsent), hasLIForSinglePurpose(8, purposeLI)});
        dict.put("9", new int[]{hasConsentForSinglePurpose(9, purposeConsent), hasLIForSinglePurpose(9, purposeLI)});
        dict.put("10", new int[]{hasConsentForSinglePurpose(10, purposeConsent), hasLIForSinglePurpose(10, purposeLI)});
        dict.put("GV", new int[]{hasGoogleVendorConsent, hasGoogleVendorLI});
        return dict;
    }


    /**
     * ToDo
     */
    @UsedByGodot
    public org.godotengine.godot.Dictionary getRawConsentStatusForSinglePurpose(int index) {
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

        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();

        if (basicConsentIds.contains(index)) {
            dict.put(String.valueOf(index), new int[]{hasConsentForSinglePurpose(index, purposeConsent), hasConsentForSinglePurpose(index, purposeConsent)});
        } else if (consentOrLIIds.contains(index)) {
            String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests", "");
            dict.put(String.valueOf(index), new int[]{hasConsentForSinglePurpose(index, purposeConsent), hasLIForSinglePurpose(index, purposeLI)});
        } else if (index == 0) {
            String vendorConsent = prefs.getString("IABTCF_VendorConsents", "");
            String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "");
            int googleId = 755;
            int hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId) ? 1 : 0;
            int hasGoogleVendorLI = hasAttribute(vendorLI, googleId) ? 1 : 0;
            dict.put("GV", new int[]{hasGoogleVendorConsent, hasGoogleVendorLI});
        } else {
            Log.e(TAG, "Invalid consent index");
            dict.put("IDX_OOB_ERROR", "Index should be between 0 and 10: 0 for Google vendor consent, 1-10 for any named purposes");
        }
        return dict;
    }

    /**
     * ToDo
     * @return
     */
    @UsedByGodot
    public org.godotengine.godot.Dictionary getConsentStatusIssuesList(){
        String vendorConsent = prefs.getString("IABTCF_VendorConsents", "");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests", "");

        int googleId = 755;
        int hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId) ? 1 : 0;
        int hasGoogleVendorLI = hasAttribute(vendorLI, googleId) ? 1 : 0;
        boolean basicAds = true;
        boolean personalisedAds = true;

        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();

        if (!consentIsNeeded()) {
            dict.put("ADS_STATUS", 3);
            return dict;
        }

        if (!canShowAds()){
            basicAds = false;
            if ((hasGoogleVendorConsent + hasGoogleVendorLI) < 2) {
                dict.put("MISSING_VENDOR_CONSENT", "Google Advertising Products vendor consent and/or legitimate interest missing (both are needed).");
            }

            if (deniedPurposes.size() > 0){
                dict.put("MISSING_MANDATORY_CONSENT", listToIntArray(deniedPurposes));
            }

            if (deniedFlexiblePurposes.size() > 0){
                dict.put("MISSING_CONSENT_OR_LEGIT_INTEREST", listToIntArray(deniedFlexiblePurposes));
            }
        }

        if (!canShowPersonalizedAds()){
            personalisedAds = false;
            if (deniedPurposes.size() > 0){
                dict.put("MISSING_PERSONALISED_CONSENT", listToIntArray(deniedPurposes));
            }
        }

        if (basicAds && personalisedAds){
            dict.put("ADS_STATUS", 2);
        } else if (basicAds) {
            dict.put("ADS_STATUS", 1);
        } else {
            dict.put("ADS_STATUS", 0);
        }

        return dict;
    }

    /**
     * ToDo
     * @return
     */
    @UsedByGodot
    public org.godotengine.godot.Dictionary getFullPurposeNamesByKey(){
        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();
        dict.put("1", "Purpose 1 - Store and/or access information on a device");
        dict.put("2", "Purpose 2 - Select basic ads");
        dict.put("3", "Purpose 3 - Create a personalised ads profile");
        dict.put("4", "Purpose 4 - Select personalised ads");
        dict.put("5", "Purpose 5 - Create a personalised content profile");
        dict.put("6", "Purpose 6 - Select personalised content");
        dict.put("7", "Purpose 7 - Measure ad performance");
        dict.put("8", "Purpose 8 - Measure content performance");
        dict.put("9", "Purpose 9 - Apply market research to generate audience insights");
        dict.put("10", "Purpose 10 - Develop and improve products");
        dict.put("GV", "Google Advertising Products vendor consent anf legitimate interest");
        return dict;
    }

}
