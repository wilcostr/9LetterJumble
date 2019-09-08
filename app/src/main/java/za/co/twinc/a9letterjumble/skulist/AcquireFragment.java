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

package za.co.twinc.a9letterjumble.skulist;

import static com.android.billingclient.api.BillingClient.BillingResponseCode;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.RewardResponseListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;
import za.co.twinc.a9letterjumble.R;
import za.co.twinc.a9letterjumble.billing.BillingProvider;
import za.co.twinc.a9letterjumble.skulist.row.SkuRowData;
import za.co.twinc.a9letterjumble.skulist.row.UiManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a screen with various in-app purchase and subscription options
 */
public class AcquireFragment extends DialogFragment {
    private RecyclerView mRecyclerView;
    private SkusAdapter mAdapter;
    private UiManager uiManager;
    private View mLoadingView;
    private TextView mErrorTextView;
    private BillingProvider mBillingProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.acquire_fragment, container, false);
        mErrorTextView = root.findViewById(R.id.error_textview);
        mRecyclerView = root.findViewById(R.id.list);
        mLoadingView = root.findViewById(R.id.screen_wait);

        mAdapter = new SkusAdapter();
        uiManager = createUiManager(mAdapter, mBillingProvider);
        mAdapter.setUiManager(uiManager);

        if (mBillingProvider != null) {
            handleManagerAndUiReady();
        }
        // Setup a toolbar for this fragment
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.setTitle(R.string.button_store);

        return root;
    }

    /**
     * Refreshes this fragment's UI
     */
    public void refreshUI() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Notifies the fragment that billing manager is ready and provides a BillingProviders
     * instance to access it
     */
    public void onManagerReady(BillingProvider billingProvider) {
        mBillingProvider = billingProvider;
        if (mRecyclerView != null) {
            handleManagerAndUiReady();
        }
    }

    /**
     * Enables or disables "please wait" screen.
     */
    private void setWaitScreen(boolean set) {
        mRecyclerView.setVisibility(set ? View.GONE : View.VISIBLE);
        mLoadingView.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    /**
     * Executes query for SKU details at the background thread
     */
    private void handleManagerAndUiReady() {
        // If Billing Manager was successfully initialized - start querying for SKUs
        setWaitScreen(true);
        querySkuDetails();
    }

    private void displayAnErrorIfNeeded() {
        if (getActivity() == null || getActivity().isFinishing())
            return;

        mLoadingView.setVisibility(View.GONE);
        mErrorTextView.setVisibility(View.VISIBLE);
        int billingResponseCode = mBillingProvider.getBillingManager()
                .getBillingClientResponseCode();

        switch (billingResponseCode) {
            case BillingResponseCode.OK:
                // If manager was connected successfully, then show no SKUs error
                mErrorTextView.setText(getText(R.string.error_no_skus));
                break;
            case BillingResponseCode.BILLING_UNAVAILABLE:
                mErrorTextView.setText(getText(R.string.error_billing_unavailable));
                break;
            default:
                mErrorTextView.setText(getText(R.string.error_billing_default));
        }

    }

    /**
     * Queries for in-app and subscriptions SKU details and updates an adapter with new data
     */
    private void querySkuDetails() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            final List<SkuRowData> dataList = new ArrayList<>();

            // Filling the list with all the data to render subscription rows
            List<String> subscriptionsSkus = uiManager.getDelegatesFactory()
                    .getSkuList(SkuType.SUBS);
            addSkuRows(dataList, subscriptionsSkus, SkuType.SUBS, new Runnable() {
                @Override
                public void run() {
                    // Once we added all the subscription items, fill the in-app items rows below
                    List<String> inAppSkus = uiManager.getDelegatesFactory()
                            .getSkuList(SkuType.INAPP);
                    addSkuRows(dataList, inAppSkus, SkuType.INAPP, null);
                }
            });
        }
    }

    private void addSkuRows(final List<SkuRowData> inList, final List<String> skusList,
                            final @SkuType String billingType, final Runnable executeWhenFinished) {

        mBillingProvider.getBillingManager().querySkuDetailsAsync(billingType, skusList,
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {

                        if (billingResult.getResponseCode() == BillingResponseCode.OK
                                && skuDetailsList != null
                                && skuDetailsList.size() > 0) {

                            // If we successfully got SKUs fill all the rows
                            // If we got a full house of results, keep the order
                            if (skuDetailsList.size() == skusList.size()){
                                for (int i = 0; i < skusList.size(); i++) {
                                    for (SkuDetails details : skuDetailsList) {
                                        if (details.getSku().equals(skusList.get(i))) {
                                            inList.add(new SkuRowData(details, SkusAdapter.TYPE_NORMAL,
                                                    billingType));
                                            if (details.isRewarded()) {
                                                mBillingProvider
                                                        .getBillingManager()
                                                        .loadRewardedVideo(details,
                                                                rewardResponse());
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                for (SkuDetails details : skuDetailsList) {
                                    inList.add(new SkuRowData(details, SkusAdapter.TYPE_NORMAL,
                                            billingType));
                                }
                            }

                            if (inList.size() == 0) {
                                displayAnErrorIfNeeded();
                            }
                            else {
                                if (mRecyclerView.getAdapter() == null) {
                                    mRecyclerView.setAdapter(mAdapter);
                                    Context context = getContext();
                                    Resources res;
                                    if (context != null) {
                                        res = context.getResources();
                                        mRecyclerView.addItemDecoration(new CardsWithHeadersDecoration(
                                                mAdapter, (int) res.getDimension(R.dimen.header_gap),
                                                (int) res.getDimension(R.dimen.row_gap)));
                                        mRecyclerView.setLayoutManager(
                                                new LinearLayoutManager(getContext()));
                                    }
                                }
                                mAdapter.updateData(inList);
                                setWaitScreen(false);
                            }
                        }
                        if (executeWhenFinished != null) {
                            executeWhenFinished.run();
                        }
                    }
                });
    }

    private RewardResponseListener rewardResponse () {
        return new RewardResponseListener() {
            @Override
            public void onRewardResponse(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingResponseCode.OK) {
                    // Enable the reward product, or make
                    // any necessary updates to the UI.
                    System.out.println("'tis done...");
                    uiManager.enableRewardFromUiManager();
                }
                else
                    System.out.println(billingResult.getDebugMessage());
            }
        };
    }

    protected UiManager createUiManager(SkusAdapter adapter, BillingProvider provider) {
        return new UiManager(adapter, provider);
    }
}