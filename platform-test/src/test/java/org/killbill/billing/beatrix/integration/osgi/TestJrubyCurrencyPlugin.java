/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.beatrix.integration.osgi;

import java.math.BigDecimal;
import java.util.Set;

import org.joda.time.DateTime;
import org.killbill.billing.beatrix.integration.osgi.util.SetupBundleWithAssertion;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.currency.api.Rate;
import org.killbill.billing.currency.plugin.api.CurrencyPluginApi;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TestJrubyCurrencyPlugin extends TestOSGIBase {

    private static final String BUNDLE_TEST_RESOURCE_PREFIX = "killbill-currency-plugin-test";
    private static final String BUNDLE_TEST_RESOURCE = BUNDLE_TEST_RESOURCE_PREFIX + ".tar.gz";

    @BeforeClass(groups = "slow", enabled = false)
    public void beforeClass() throws Exception {
        super.beforeClass();

        final String killbillVersion = System.getProperty("killbill.version");
        final SetupBundleWithAssertion setupTest = new SetupBundleWithAssertion(BUNDLE_TEST_RESOURCE, osgiConfig, killbillVersion);
        setupTest.setupJrubyBundle();
    }

    @Test(groups = "slow", enabled = false)
    public void testCurrencyApis() throws Exception {
        final CurrencyPluginApi api = getTestApi(currencyPluginApiOSGIServiceRegistration, BUNDLE_TEST_RESOURCE_PREFIX);

        final Set<Currency> currencies = api.getBaseCurrencies();
        assertEquals(currencies.size(), 1);
        assertEquals(currencies.iterator().next(), Currency.USD);

        final DateTime res = api.getLatestConversionDate(Currency.USD);
        assertNotNull(res);

        final Set<Rate> rates = api.getCurrentRates(Currency.USD);
        assertEquals(rates.size(), 1);
        final Rate theRate = rates.iterator().next();
        assertEquals(theRate.getBaseCurrency(), Currency.USD);
        assertEquals(theRate.getCurrency(), Currency.BRL);
        Assert.assertTrue(theRate.getValue().compareTo(new BigDecimal("12.3")) == 0);
    }
}
