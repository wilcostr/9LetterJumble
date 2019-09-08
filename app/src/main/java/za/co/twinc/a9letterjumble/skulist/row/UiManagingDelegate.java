// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package za.co.twinc.a9letterjumble.skulist.row;

import android.widget.Button;
import android.widget.Toast;
import com.android.billingclient.api.BillingClient.SkuType;
import za.co.twinc.a9letterjumble.R;
import za.co.twinc.a9letterjumble.billing.BillingProvider;

/**
 * Implementations of this abstract class are responsible to render UI and handle user actions for
 * skulist rows to render RecyclerView with AcquireFragment's specific UI
 */
public abstract class UiManagingDelegate {

    final BillingProvider mBillingProvider;
    private Button rewardButton;

    public abstract @SkuType String getType();

    UiManagingDelegate(BillingProvider billingProvider) {
        mBillingProvider = billingProvider;
        rewardButton = null;
    }

    public void onBindViewHolder(SkuRowData data, RowViewHolder holder) {
        holder.description.setText(data.getDescription());
        holder.price.setText(data.getPrice());

        if (data.getSku().equals("reward")) {
            holder.button.setEnabled(false);
            holder.button.setAlpha(0.4f);
        }

        rewardButton = holder.button;
    }

    public void onButtonClicked(SkuRowData data) {
        mBillingProvider.getBillingManager().initiatePurchaseFlow(data.getSku());
    }

    void showAlreadyPurchasedToast() {
        Toast.makeText(mBillingProvider.getBillingManager().getContext(),
                R.string.alert_already_purchased, Toast.LENGTH_SHORT).show();
    }

    void enableRewardButton() {
        if (rewardButton != null) {
            rewardButton.setEnabled(true);
            rewardButton.setAlpha(1f);
        }
    }
}
