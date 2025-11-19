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
import java.util.Collections;
import java.util.UUID;

import org.killbill.billing.beatrix.integration.osgi.util.SetupBundleWithAssertion;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.osgi.api.PaymentPluginApiWithTestControl;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestPaymentOSGIWithTestPaymentBundle extends TestOSGIBase {

    // Magic name, see org.killbill.billing.osgi.bundles.test.PaymentActivator
    private static final String TEST_PLUGIN_NAME = "osgi-payment-plugin";
    private static final String BUNDLE_TEST_RESOURCE = "killbill-osgi-bundles-test-payment";

    @BeforeClass(groups = "slow")
    public void beforeClass() throws Exception {
        super.beforeClass();

        final String killbillVersion = System.getProperty("killbill.version");
        final SetupBundleWithAssertion setupTest = new SetupBundleWithAssertion(BUNDLE_TEST_RESOURCE, osgiConfig, killbillVersion);
        setupTest.setupJavaBundle();
    }

    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws Exception {
        super.beforeMethod();
        ((PaymentPluginApiWithTestControl) getTestApi(paymentPluginApiOSGIServiceRegistration, TEST_PLUGIN_NAME)).resetToNormalBehavior();
    }

    @Test(groups = "slow")
    public void testBasicProcessPaymentOK() throws Exception {
        final PaymentPluginApi paymentPluginApi = getTestApi(paymentPluginApiOSGIServiceRegistration, TEST_PLUGIN_NAME);
        final UUID paymentId = UUID.randomUUID();
        final BigDecimal amount = BigDecimal.TEN;
        final Currency currency = Currency.USD;
        final PaymentTransactionInfoPlugin PaymentTransactionInfoPlugin = paymentPluginApi.purchasePayment(UUID.randomUUID(), paymentId, UUID.randomUUID(), UUID.randomUUID(), amount, currency, Collections.emptyList(), callContext);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getKbPaymentId(), paymentId);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getAmount().compareTo(amount), 0);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getCurrency(), currency);
    }

    @Test(groups = "slow")
    public void testBasicProcessPaymentWithPaymentPluginApiException() throws Exception {
        final PaymentPluginApiWithTestControl paymentPluginApi = (PaymentPluginApiWithTestControl) getTestApi(paymentPluginApiOSGIServiceRegistration, TEST_PLUGIN_NAME);

        final String errorType = "test-error";
        final String errorMessage = "foo";
        final PaymentPluginApiException exception = new PaymentPluginApiException(errorType, errorMessage);

        paymentPluginApi.setPaymentPluginApiExceptionOnNextCalls(exception);
        try {
            paymentPluginApi.purchasePayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, Currency.USD, Collections.emptyList(), callContext);
            Assert.fail("Expected to fail with " + exception.toString());
        } catch (final PaymentPluginApiException e) {
            Assert.assertEquals(e.getErrorType(), errorType);
            Assert.assertEquals(e.getErrorMessage(), errorMessage);
        }
    }

    @Test(groups = "slow")
    public void testBasicProcessPaymentWithRuntimeException() throws Exception {
        final PaymentPluginApiWithTestControl paymentPluginApi = (PaymentPluginApiWithTestControl) getTestApi(paymentPluginApiOSGIServiceRegistration, TEST_PLUGIN_NAME);

        final String message = "test-error";
        final RuntimeException exception = new RuntimeException(message);

        paymentPluginApi.setPaymentRuntimeExceptionOnNextCalls(exception);
        try {
            paymentPluginApi.purchasePayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, Currency.USD, Collections.emptyList(), callContext);
            Assert.fail("Expected to fail with " + exception.toString());
        } catch (final RuntimeException e) {
            Assert.assertEquals(e.getMessage(), message);
        }
    }
}
