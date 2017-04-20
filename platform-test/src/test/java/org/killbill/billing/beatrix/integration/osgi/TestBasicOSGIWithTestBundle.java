/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.killbill.billing.beatrix.integration.osgi.util.ExternalBusTestEvent;
import org.killbill.billing.beatrix.integration.osgi.util.SetupBundleWithAssertion;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import static org.awaitility.Awaitility.await;

/**
 * Basic OSGI test that relies on the 'test' bundle (org.killbill.billing.osgi.bundles.test.TestActivator)
 * <p/>
 * The test checks that the bundle:
 * - gets started
 * - can make API call
 * - can listen to KB events
 * - can register a service (PaymentPluginApi) that this test calls
 * - can write in the DB using the DataSource (this is how the assertion work)
 */
public class TestBasicOSGIWithTestBundle extends TestOSGIBase {

    // Magic name, see org.killbill.billing.osgi.bundles.test.TestActivator
    private static final String TEST_PLUGIN_NAME = "test";
    private static final String BUNDLE_TEST_RESOURCE = "killbill-osgi-bundles-test-beatrix";

    @BeforeClass(groups = "slow")
    public void beforeClass() throws Exception {
        super.beforeClass();

        final String killbillVersion = System.getProperty("killbill.version");
        final SetupBundleWithAssertion setupTest = new SetupBundleWithAssertion(BUNDLE_TEST_RESOURCE, osgiConfig, killbillVersion);
        setupTest.setupJavaBundle();
    }

    @Test(groups = "slow")
    public void testBundleTest() throws Exception {
        // At this point test bundle should have been started already
        final TestActivatorWithAssertion assertTor = new TestActivatorWithAssertion(dbi);
        assertTor.assertPluginInitialized();

        // Send an event and expect the test bundle to listen to KB events and write the account id in its table
        final ExternalBusTestEvent event = new ExternalBusTestEvent();
        externalBus.post(event);
        assertTor.assertPluginReceivedEvent(event.getAccountId().toString());

        // Retrieve the PaymentPluginApi that the test bundle registered
        final PaymentPluginApi paymentPluginApi = getTestApi(paymentPluginApiOSGIServiceRegistration, TEST_PLUGIN_NAME);

        // Make a payment and expect the test bundle to correctly write in its table the input values
        final UUID paymentId = UUID.randomUUID();
        final UUID transactionId = UUID.randomUUID();
        final UUID paymentMethodId = UUID.randomUUID();
        final BigDecimal paymentAmount = new BigDecimal("14.32");
        final Currency currency = Currency.USD;
        final PaymentTransactionInfoPlugin PaymentTransactionInfoPlugin = paymentPluginApi.purchasePayment(event.getAccountId(), paymentId, transactionId, paymentMethodId, paymentAmount, currency, ImmutableList.<PluginProperty>of(), callContext);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getKbPaymentId(), paymentId);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getKbTransactionPaymentId(), transactionId);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getAmount().compareTo(paymentAmount), 0);
        Assert.assertEquals(PaymentTransactionInfoPlugin.getCurrency(), currency);
        assertTor.assertPluginCreatedPayment(paymentId, paymentMethodId, paymentAmount);
    }

    private static final class TestActivatorWithAssertion {

        private final IDBI dbi;

        public TestActivatorWithAssertion(final IDBI dbi) {
            this.dbi = dbi;
        }

        public void assertPluginInitialized() {
            assertWithCallback(new AwaitCallback() {
                @Override
                public boolean isSuccess() {
                    return isPluginInitialized();
                }
            }, "Plugin did not complete initialization");
        }

        public void assertPluginReceivedEvent(final String expectedExternalKey) {
            assertWithCallback(new AwaitCallback() {
                @Override
                public boolean isSuccess() {
                    return isValidAccountExternalKey(expectedExternalKey);
                }
            }, "Plugin did not receive event");
        }

        public void assertPluginCreatedPayment(final UUID expectedPaymentId, final UUID expectedPaymentMethodId, final BigDecimal expectedAmount) {
            assertWithCallback(new AwaitCallback() {
                @Override
                public boolean isSuccess() {
                    return isValidPayment(expectedPaymentId, expectedPaymentMethodId, expectedAmount);
                }
            }, "Plugin did not create the payment");
        }

        private void assertWithCallback(final AwaitCallback callback, final String error) {
            try {
                await().atMost(15, TimeUnit.SECONDS).until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return callback.isSuccess();
                    }
                });
            } catch (final Exception e) {
                Assert.fail(error, e);
            }
        }

        private boolean isValidPayment(final UUID expectedPaymentId, final UUID expectedPaymentMethodId, final BigDecimal expectedAmount) {
            final TestModel test = getTestModelFirstRecord();
            return expectedPaymentId.equals(test.getPaymentId()) &&
                   expectedPaymentMethodId.equals(test.getPaymentMethodId()) &&
                   expectedAmount.compareTo(test.getAmount()) == 0;
        }

        private boolean isPluginInitialized() {
            final TestModel test = getTestModelFirstRecord();
            return test.isStarted();
        }

        private boolean isValidAccountExternalKey(final String expectedExternalKey) {
            final TestModel test = getTestModelFirstRecord();
            return expectedExternalKey.equals(test.getAccountExternalKey());
        }

        private TestModel getTestModelFirstRecord() {
            return dbi.inTransaction(new TransactionCallback<TestModel>() {
                @Override
                public TestModel inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                    final Query<Map<String, Object>> q = conn.createQuery("SELECT is_started, external_key, payment_id, payment_method_id, payment_amount FROM test_bundle WHERE record_id = 1;");
                    return q.map(new TestMapper()).first();
                }
            });
        }
    }

    private static final class TestModel {

        private final Boolean isStarted;
        private final String accountExternalKey;
        private final UUID paymentId;
        private final UUID paymentMethodId;
        private final BigDecimal amount;

        private TestModel(final Boolean started, final String accountExternalKey, final UUID paymentId, final UUID paymentMethodId, final BigDecimal amount) {
            isStarted = started;
            this.accountExternalKey = accountExternalKey;
            this.paymentId = paymentId;
            this.paymentMethodId = paymentMethodId;
            this.amount = amount;
        }

        public Boolean isStarted() {
            return isStarted;
        }

        public String getAccountExternalKey() {
            return accountExternalKey;
        }

        public UUID getPaymentId() {
            return paymentId;
        }

        public UUID getPaymentMethodId() {
            return paymentMethodId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

    }

    private static final class TestMapper implements ResultSetMapper<TestModel> {

        @Override
        public TestModel map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {

            final Boolean isStarted = r.getBoolean("is_started");
            final String externalKey = r.getString("external_key");
            final UUID paymentId = r.getString("payment_id") != null ? UUID.fromString(r.getString("payment_id")) : null;
            final UUID paymentMethodId = r.getString("payment_method_id") != null ? UUID.fromString(r.getString("payment_method_id")) : null;
            final BigDecimal amount = r.getBigDecimal("payment_amount");
            return new TestModel(isStarted, externalKey, paymentId, paymentMethodId, amount);
        }
    }

    private interface AwaitCallback {

        boolean isSuccess();
    }
}
