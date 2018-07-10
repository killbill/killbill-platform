/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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

package org.killbill.billing.platform.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

import com.google.common.collect.ImmutableMap;

public class TestDefaultKillbillConfigSource {

    private static final String ENABLE_JASYPT_PROPERTY = "org.killbill.server.enableJasypt";
    private static final String JASYPT_ENCRYPTOR_PASSWORD_PROPERTY = "JASYPT_ENCRYPTOR_PASSWORD";
    private static final String JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY = "JASYPT_ENCRYPTOR_ALGORITHM";
    private static final String JASYPT_PASSWORD = "top_secret!";
    private static final String JASYPT_ALGORITHM = "PBEWITHMD5ANDDES";
    private static final String ENCRYPTED_PROPERTY_1 = "test.encrypted.property1";
    private static final String ENCRYPTED_PROPERTY_2 = "test.encrypted.property2";

    @BeforeMethod(groups = "fast")
    public void setup() {
        // Clean out the properties we set in the tests,
        // this is only necessary because the DefaultKillBillConfigSource constructor we're using ends up
        // setting this.properties to System.getProperties(), which doesn't automatically get reset between tests.
        System.clearProperty(ENABLE_JASYPT_PROPERTY);
        System.clearProperty(JASYPT_PASSWORD);
        System.clearProperty(JASYPT_ALGORITHM);
        System.clearProperty(ENCRYPTED_PROPERTY_1);
        System.clearProperty(ENCRYPTED_PROPERTY_2);
    }

    @Test(groups = "fast")
    public void testJasyptDisabledByDefault() throws IOException, URISyntaxException {
        DefaultKillbillConfigSource configSource = new DefaultKillbillConfigSource();

        String enableJasyptString = configSource.getString(ENABLE_JASYPT_PROPERTY);

        Assert.assertFalse(Boolean.parseBoolean(enableJasyptString));
    }

    @Test(groups = "fast")
    public void testDecyptionExplicitlyDisabled() throws IOException, URISyntaxException {
        String unencryptedValue = "myPropertyValue";
        String encryptedValue = encString(unencryptedValue);

        Map<String, String> properties = ImmutableMap.of(ENABLE_JASYPT_PROPERTY, "false",
                                                         ENCRYPTED_PROPERTY_1, encryptedValue,
                                                         JASYPT_ENCRYPTOR_PASSWORD_PROPERTY, JASYPT_PASSWORD,
                                                         JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY, JASYPT_ALGORITHM);

        DefaultKillbillConfigSource configSource = new DefaultKillbillConfigSource(properties);

        String actualValue = configSource.getString(ENCRYPTED_PROPERTY_1);

        Assert.assertEquals(encryptedValue, actualValue);
    }

    @Test(groups = "fast", expectedExceptions = IllegalArgumentException.class)
    public void testDecryptEmptyPassword() throws IOException, URISyntaxException {
        String encryptedValue = encString("myPropertyValue");

        Map<String, String> properties = ImmutableMap.of(ENABLE_JASYPT_PROPERTY, "true",
                                                         ENCRYPTED_PROPERTY_1, encryptedValue,
                                                         JASYPT_ENCRYPTOR_PASSWORD_PROPERTY, "",
                                                         JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY, JASYPT_ALGORITHM);

        new DefaultKillbillConfigSource(properties);
    }

    @Test(groups = "fast", expectedExceptions = IllegalArgumentException.class)
    public void testDecryptEmptyAlgorithm() throws IOException, URISyntaxException {
        String encryptedValue = encString("myPropertyValue");

        Map<String, String> properties = ImmutableMap.of(ENABLE_JASYPT_PROPERTY, "true",
                                                         ENCRYPTED_PROPERTY_1, encryptedValue,
                                                         JASYPT_ENCRYPTOR_PASSWORD_PROPERTY, JASYPT_PASSWORD,
                                                         JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY, "");

        new DefaultKillbillConfigSource(properties);
    }

    @Test(groups = "fast", expectedExceptions = EncryptionOperationNotPossibleException.class)
    public void testDecryptInvalidJasyptString() throws IOException, URISyntaxException {
        String encryptedValue = "ENC(notAValidEncryptedString!)";

        Map<String, String> properties = ImmutableMap.of(ENABLE_JASYPT_PROPERTY, "true",
                                                         ENCRYPTED_PROPERTY_1, encryptedValue,
                                                         JASYPT_ENCRYPTOR_PASSWORD_PROPERTY, JASYPT_PASSWORD,
                                                         JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY, JASYPT_ALGORITHM);

        new DefaultKillbillConfigSource(properties);
    }

    @Test(groups = "fast", expectedExceptions = EncryptionOperationNotPossibleException.class)
    public void testDecryptEmptyJasyptString() throws IOException, URISyntaxException {
        String encryptedValue = "ENC()";

        Map<String, String> properties = ImmutableMap.of(ENABLE_JASYPT_PROPERTY, "true",
                                                         ENCRYPTED_PROPERTY_1, encryptedValue,
                                                         JASYPT_ENCRYPTOR_PASSWORD_PROPERTY, JASYPT_PASSWORD,
                                                         JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY, JASYPT_ALGORITHM);

        new DefaultKillbillConfigSource(properties);
    }

    @Test(groups = "fast")
    public void testDecryptJasyptPropertySuccessfully() throws IOException, URISyntaxException {
        String unencryptedValue1 = "myPropertyValue";
        String encryptedValue1 = encString(unencryptedValue1);
        String unencryptedValue2 = "myOtherPropertyValue";
        String encryptedValue2 = encString(unencryptedValue2);

        Map<String, String> properties = ImmutableMap.of(ENABLE_JASYPT_PROPERTY, "true",
                                                         ENCRYPTED_PROPERTY_1, encryptedValue1,
                                                         ENCRYPTED_PROPERTY_2, encryptedValue2,
                                                         JASYPT_ENCRYPTOR_PASSWORD_PROPERTY, JASYPT_PASSWORD,
                                                         JASYPT_ENCRYPTOR_ALGORITHM_PROPERTY, JASYPT_ALGORITHM);

        DefaultKillbillConfigSource configSource = new DefaultKillbillConfigSource(properties);

        String actualValue1 = configSource.getString(ENCRYPTED_PROPERTY_1);
        String actualValue2 = configSource.getString(ENCRYPTED_PROPERTY_2);

        Assert.assertEquals(unencryptedValue1, actualValue1);
        Assert.assertEquals(unencryptedValue2, actualValue2);
    }

    private String encString(final String unencryptedValue) {
        return "ENC(" + encrypt(unencryptedValue, JASYPT_ALGORITHM, JASYPT_PASSWORD) + ")";
    }

    private String encrypt(final String unencryptedValue, final String jasyptAlgorithm, final String jasyptPassword) {
        final StandardPBEStringEncryptor encryptor = setupEncryptor(jasyptPassword, jasyptAlgorithm);
        return encryptor.encrypt(unencryptedValue);
    }

    private StandardPBEStringEncryptor setupEncryptor(String password, String algorithm) {
        final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(password);
        encryptor.setAlgorithm(algorithm);
        return encryptor;
    }
}
