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
    private Context context = this.getGodot().getContext();
    public ParseConsentString(Godot godot) {
        super(godot);
       }

    @NonNull
    @Override
    public String getPluginName() {
        return "EUConsentStringParser";
    }


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


    private boolean hasConsentForSinglePurpose(int index, String purposeConsent, boolean hasVendorConsent) {
        if (!hasAttribute(purposeConsent, index)) {
            Log.e(TAG, "hasConsentFor: denied for purpose #" + index );
            return false;
        }
        return hasVendorConsent;
    }

    private boolean hasConsentOrLegitimateInterestForSinglePurpose(int index, String purposeConsent, String purposeLI, boolean hasVendorConsent, boolean hasVendorLI){
        boolean purposeAndVendorLI = hasAttribute(purposeLI, index) && hasVendorLI;
        boolean purposeConsentAndVendorConsent = hasAttribute(purposeConsent, index) && hasVendorConsent;
        boolean isOk = purposeAndVendorLI || purposeConsentAndVendorConsent;
        if (!isOk){
            Log.e(TAG, "hasConsentOrLegitimateInterestFor: denied for #" + index);
            return false;
        }

        return true;
    }


    @UsedByGodot
    public boolean canShowAds(){
        try {
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
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
            return false;
        }

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

    @UsedByGodot
    public org.godotengine.godot.Dictionary getConsentStatusForAllPurposes(){
        org.godotengine.godot.Dictionary dict = new org.godotengine.godot.Dictionary();
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents","");
        String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","");
        String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","");

        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);
        boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);

        dict.put("1", hasConsentForSinglePurpose(1, purposeConsent, hasGoogleVendorConsent));
        dict.put("2", hasConsentOrLegitimateInterestForSinglePurpose(2, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI));
        dict.put("3", hasConsentForSinglePurpose(3, purposeConsent, hasGoogleVendorConsent));
        dict.put("4", hasConsentForSinglePurpose(4, purposeConsent, hasGoogleVendorConsent));
        dict.put("7", hasConsentOrLegitimateInterestForSinglePurpose(7, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI));
        dict.put("9", hasConsentOrLegitimateInterestForSinglePurpose(9, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI));
        dict.put("10", hasConsentOrLegitimateInterestForSinglePurpose(10, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI));

        return dict;
    }

    @UsedByGodot
    public boolean getConsentStatusForSinglePurpose(int index){
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String purposeConsent = prefs.getString("IABTCF_PurposeConsents", "");
        String vendorConsent = prefs.getString("IABTCF_VendorConsents","");
        int googleId = 755;
        boolean hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId);

        List<Integer> basicConsentIds = new ArrayList<>();
        basicConsentIds.add(1);
        basicConsentIds.add(3);
        basicConsentIds.add(4);

        List<Integer> consentOrLIIds = new ArrayList<>();
        consentOrLIIds.add(2);
        consentOrLIIds.add(7);
        consentOrLIIds.add(9);
        consentOrLIIds.add(10);

        if (basicConsentIds.contains(index)){
            return hasConsentForSinglePurpose(index, purposeConsent, hasGoogleVendorConsent);
        } else if (consentOrLIIds.contains(index)){
            String vendorLI = prefs.getString("IABTCF_VendorLegitimateInterests","");
            String purposeLI = prefs.getString("IABTCF_PurposeLegitimateInterests","");
            boolean hasGoogleVendorLI = hasAttribute(vendorLI, googleId);
            return hasConsentOrLegitimateInterestForSinglePurpose(index, purposeConsent, purposeLI, hasGoogleVendorConsent, hasGoogleVendorLI);
        } else {
            Log.e(TAG, "Invalid consent index");
            return false;
        }
    }

}
