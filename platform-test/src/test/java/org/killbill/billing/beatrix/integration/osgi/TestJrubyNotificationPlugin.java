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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.killbill.billing.ObjectType;
import org.killbill.billing.beatrix.integration.osgi.util.ExternalBusTestEvent;
import org.killbill.billing.beatrix.integration.osgi.util.SetupBundleWithAssertion;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import org.awaitility.Awaitility;

public class TestJrubyNotificationPlugin extends TestOSGIBase {

    private static final String BUNDLE_TEST_RESOURCE_PREFIX = "killbill-notification-test";
    private static final String BUNDLE_TEST_RESOURCE = BUNDLE_TEST_RESOURCE_PREFIX + ".tar.gz";
    private static final Path MAGIC_FILE_PATH = Paths.get(FileSystems.getDefault().getSeparator() + "var", "tmp", "killbill-notification-test.txt");

    @BeforeClass(groups = "slow", enabled = false)
    public void beforeClass() throws Exception {
        super.beforeClass();

        final String killbillVersion = System.getProperty("killbill.version");
        final SetupBundleWithAssertion setupTest = new SetupBundleWithAssertion(BUNDLE_TEST_RESOURCE, osgiConfig, killbillVersion);
        setupTest.setupJrubyBundle();
    }

    @BeforeMethod(groups = "slow", enabled = false)
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();

        try {
            cleanupMagicFile();
        } catch (final NoSuchFileException ignored) {
        }
    }

    @AfterMethod(groups = "slow", enabled = false)
    @Override
    public void afterMethod() throws Exception {
        super.afterMethod();

        Assert.assertFalse(Files.exists(MAGIC_FILE_PATH));
    }

    @Test(groups = "slow", enabled = false)
    public void testOnEventForAccountCreation() throws Exception {
        final UUID objectId = UUID.randomUUID();
        final UUID accountId = UUID.randomUUID();
        final UUID tenantId = UUID.randomUUID();

        // Post ACCOUNT_CREATION event
        final ExternalBusTestEvent firstEvent = new ExternalBusTestEvent(objectId, ObjectType.ACCOUNT, ExtBusEventType.ACCOUNT_CREATION, accountId, tenantId, null, 0L, 1L, UUID.randomUUID());
        externalBus.post(firstEvent);

        // The plugin should have created a TAG_CREATION event
        checkThePluginGotTheEvent(firstEvent, ExtBusEventType.TAG_CREATION);

        // Post ACCOUNT_CHANGE event
        final ExternalBusTestEvent secondEvent = new ExternalBusTestEvent(objectId, ObjectType.ACCOUNT, ExtBusEventType.ACCOUNT_CHANGE, accountId, tenantId, null, 0L, 1L, UUID.randomUUID());
        externalBus.post(secondEvent);

        // The plugin should have created a TAG_DELETION event
        checkThePluginGotTheEvent(secondEvent, ExtBusEventType.TAG_DELETION);
    }

    private void checkThePluginGotTheEvent(final ExtBusEvent extBusEvent, final ExtBusEventType expectedEventType) throws Exception {
        Awaitility.await()
                  .until(new Callable<Boolean>() {
                      @Override
                      public Boolean call() throws Exception {
                          return Files.exists(MAGIC_FILE_PATH);
                      }
                  });

        final String actualContent = com.google.common.io.Files.toString(new File(MAGIC_FILE_PATH.toUri()), Charsets.UTF_8);
        final String expectedContent = String.format("%s-%s-%s-%s-%s%n", expectedEventType, ObjectType.ACCOUNT, extBusEvent.getObjectId(), extBusEvent.getAccountId(), extBusEvent.getTenantId());
        Assert.assertEquals(actualContent, expectedContent);

        cleanupMagicFile();
    }

    private void cleanupMagicFile() throws IOException {
        Files.delete(MAGIC_FILE_PATH);
    }
}
