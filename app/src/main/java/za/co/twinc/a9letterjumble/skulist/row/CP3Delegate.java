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
package za.co.twinc.a9letterjumble.skulist.row;

import com.android.billingclient.api.BillingClient.SkuType;

import za.co.twinc.a9letterjumble.R;
import za.co.twinc.a9letterjumble.billing.BillingProvider;

/**
 * Handles Ui specific to "clue-pack" - consumable in-app purchase row
 */
public class CP3Delegate extends UiManagingDelegate {
    public static final String SKU_ID = "cp3";

    CP3Delegate(BillingProvider billingProvider) {
        super(billingProvider);
    }

    @Override
    public @SkuType String getType() {
        return SkuType.INAPP;
    }


    @Override
    public void onBindViewHolder(SkuRowData data, RowViewHolder holder) {
        super.onBindViewHolder(data, holder);
        holder.button.setText(R.string.button_buy);
        holder.skuIcon.setImageResource(R.drawable.cp_star);
    }
}

