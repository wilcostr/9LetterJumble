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
package za.co.twinc.a9letterjumble.billing;

import com.android.billingclient.api.BillingClient;
import java.util.Arrays;
import java.util.List;

/**
 * Static fields and methods useful for billing
 */
public final class BillingConstants {
    // SKUs for our products: the premium upgrade (non-consumable) and clue-packs (consumable)
    public static final String SKU_PREMIUM = "premium";
    public static final String SKU_CP1 = "cp1";
    public static final String SKU_CP2 = "cp2";
    public static final String SKU_CP3 = "cp3";

    private static final String[] IN_APP_SKUS = {SKU_CP1, SKU_PREMIUM, SKU_CP2, SKU_CP3};

    private BillingConstants(){}

    /**
     * Returns the list of all SKUs for the billing type specified
     */
    public static final List<String> getSkuList(@BillingClient.SkuType String billingType) {
        return Arrays.asList(IN_APP_SKUS);
    }
}

