/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.twinc.a9letterjumble;

import android.content.SharedPreferences;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import za.co.twinc.a9letterjumble.billing.BillingManager.BillingUpdatesListener;
import za.co.twinc.a9letterjumble.skulist.row.CP2Delegate;
import za.co.twinc.a9letterjumble.skulist.row.CP3Delegate;
import za.co.twinc.a9letterjumble.skulist.row.CP4Delegate;
import za.co.twinc.a9letterjumble.skulist.row.CP5Delegate;
import za.co.twinc.a9letterjumble.skulist.row.PremiumDelegate;
import za.co.twinc.a9letterjumble.skulist.row.RewardDelegate;

import java.util.List;


import static za.co.twinc.a9letterjumble.MainActivity.MAIN_PREFS;

/**
 * Handles control logic of the MainActivity
 */
class MainViewController {
    private final UpdateListener mUpdateListener;
    private MainActivity mActivity;

    // Tracks if we currently own premium
    private boolean mIsPremium;

    MainViewController(MainActivity activity) {
        mUpdateListener = new UpdateListener();
        mActivity = activity;
        loadData();
    }

    UpdateListener getUpdateListener() {
        return mUpdateListener;
    }

    boolean isPremiumPurchased() {
        return mIsPremium;
    }

    /**
     * Handler to billing updates
     */
    private class UpdateListener implements BillingUpdatesListener {
        @Override
        public void onBillingClientSetupFinished() {
            mActivity.onBillingManagerSetupFinished();
        }

        @Override
        public void onConsumeFinished(String token, BillingResult billingResult) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Successfully consumed, so we apply the effects of the item

                switch (getAndDeleteConsumableToken(token)){
                    case CP2Delegate.SKU_ID:
                        gainClues(14);
                        break;
                    case CP3Delegate.SKU_ID:
                        gainClues(18);
                        mIsPremium = true;
                        saveData();
                        break;
                    case CP4Delegate.SKU_ID:
                        gainClues(36);
                        mIsPremium = true;
                        saveData();
                        break;
                    case CP5Delegate.SKU_ID:
                        gainClues(52);
                        break;
                    case RewardDelegate.SKU_ID:
                        gainClues(1);
                        saveData();
                        break;
                }
            }
        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchaseList) {
            for (Purchase purchase : purchaseList) {
                switch (purchase.getSku()) {
                    case PremiumDelegate.SKU_ID:
                        mIsPremium = true;
                        saveData();
                        if (!purchase.isAcknowledged()){
                            mActivity.getBillingManager().acknowledgePurchase(purchase);
                        }
                        break;
                    default:
                        // Everything else is consumable and will be processed in onConsumeFinished
                        saveConsumableToken(purchase.getPurchaseToken(), purchase.getSku());
                        mActivity.getBillingManager().consumeAsync(purchase.getPurchaseToken());
                        break;
                }
            }
            mActivity.showRefreshedUi();
        }
    }

    //TODO: The powers that be recommend you save data in a secure way to prevent tampering
    private void saveData() {
        // Save to shared preferences
        SharedPreferences main_log = mActivity.getSharedPreferences(MAIN_PREFS, 0);
        SharedPreferences.Editor editor = main_log.edit();
        editor.putBoolean("premium", mIsPremium);
        editor.apply();
    }

    private void loadData() {
        // Load from shared preferences
        SharedPreferences main_log = mActivity.getSharedPreferences(MAIN_PREFS, 0);
        mIsPremium = main_log.getBoolean("premium", false);
    }

    private void gainClues(int count){
        SharedPreferences main_log = mActivity.getSharedPreferences(MAIN_PREFS, 0);
        int clue_count = main_log.getInt("clue_count", 0) + count;
        SharedPreferences.Editor editor = main_log.edit();
        editor.putInt("clue_count", clue_count);
        editor.apply();

        Toast.makeText(mActivity, mActivity.getString(R.string.clue_gained, count), Toast.LENGTH_LONG).show();
    }

    private void saveConsumableToken(String token, String sku){
        SharedPreferences main_log = mActivity.getSharedPreferences(MAIN_PREFS, 0);
        SharedPreferences.Editor editor = main_log.edit();
        editor.putString(token, sku);
        editor.apply();
    }

    private String getAndDeleteConsumableToken(String token){
        SharedPreferences main_log = mActivity.getSharedPreferences(MAIN_PREFS, 0);
        String sku = main_log.getString(token, "error");
        SharedPreferences.Editor editor = main_log.edit();
        editor.remove(token);
        editor.apply();
        return sku;
    }
}