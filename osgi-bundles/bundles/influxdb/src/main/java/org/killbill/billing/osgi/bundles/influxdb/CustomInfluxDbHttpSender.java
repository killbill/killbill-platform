/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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

package org.killbill.billing.osgi.bundles.influxdb;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.izettle.metrics.influxdb.InfluxDbSender;
import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import com.izettle.metrics.influxdb.utils.InfluxDbWriteObjectSerializer;

public class CustomInfluxDbHttpSender implements InfluxDbSender {

    private static final Logger logger = LoggerFactory.getLogger(CustomInfluxDbHttpSender.class);
    private final URL url;
    // The base64 encoded authString.
    private final int connectTimeout;
    private final int readTimeout;
    static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final InfluxDbWriteObject influxDbWriteObject;
    private final InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer;

    // final String token = "rsMgEQEAWeHce2jzL1oU0NPbCyqcFKuRghKPT4B9jNpx80dsOXUD6wR2NKx1qHe1ut2xgH9zqLbEGhub6Wx3cA==";
    //final InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray(), "killbill", "killbill");

    /**
     * Creates a new http sender given connection details.
     *
     * @param hostname       the influxDb hostname
     * @param port           the influxDb http port
     * @param database       the influxDb database to write to
     * @param authString     the authorization string to be used to connect to InfluxDb, of format username:password
     * @param timePrecision  the time precision of the metrics
     * @param connectTimeout the connect timeout
     * @param readTimeout    the read timeout
     * @throws Exception exception while creating the influxDb sender(MalformedURLException)
     */
    public CustomInfluxDbHttpSender(
            final String protocol, final String hostname, final int port, final String database, final String authString,
            final TimeUnit timePrecision, final int connectTimeout, final int readTimeout, final String measurementPrefix)
            throws Exception {

        this.influxDbWriteObject = new InfluxDbWriteObject(database, timePrecision);
        this.influxDbWriteObjectSerializer = new InfluxDbWriteObjectSerializer(measurementPrefix);

        // String endpoint = new URL(protocol, hostname, port, "/write").toString();
        String endpoint = new URL("http://localhost:8086/api/v2/write?org=killbill&bucket=killbill&precision=ns").toString();
        // String queryDb = String.format("db=%s", URLEncoder.encode(database, "UTF-8"));
        // String queryPrecision = String.format("precision=%s", TimeUtils.toTimePrecision(timePrecision));
        // this.url = new URL(endpoint + "?" + queryDb + "&" + queryPrecision);
        this.url = new URL(endpoint);
        logger.info("Influxdb endpoint " + endpoint);

        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public void flush() {
        influxDbWriteObject.setPoints(new HashSet<>());
    }

    @Override
    public boolean hasSeriesData() {
        return influxDbWriteObject.getPoints() != null && !influxDbWriteObject.getPoints().isEmpty();
    }

    @Override
    public void appendPoints(InfluxDbPoint point) {
        if (point != null) {
            influxDbWriteObject.getPoints().add(point);
        }
    }

    @Deprecated
    public CustomInfluxDbHttpSender(
            final String protocol, final String hostname, final int port, final String database, final String authString,
            final TimeUnit timePrecision) throws Exception {
        this(protocol, hostname, port, database, authString, timePrecision, 1000, 1000, "");
        logger.info("Influxdb inside deprecated const");
    }

    protected int writeData(byte[] line) throws Exception {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token Al6_8e75E-hBQ2aytW_aScHfr-A8nnS_d46ngHESCqWPIl3UB8DgDqDUhDAmErwzJIIVh6VmqW_6io-AgzimRA==");
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        OutputStream out = con.getOutputStream();
        try {
            out.write(line);
            out.flush();
        } finally {
            out.close();
        }

        int responseCode = con.getResponseCode();
        logger.info("Response code is " + responseCode);

        // Check if non 2XX response code.
        if (responseCode / 100 != 2) {
            throw new IOException(
                    "Server returned HTTP response code: " + responseCode + " for URL: " + url + " with content :'"
                    + con.getResponseMessage() + "'");
        }
        return responseCode;
    }

    @Override
    public void setTags(Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }

    @Override
    public int writeData() throws Exception {
        String linestr = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);
        final byte[] line = linestr.getBytes(UTF_8);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token Al6_8e75E-hBQ2aytW_aScHfr-A8nnS_d46ngHESCqWPIl3UB8DgDqDUhDAmErwzJIIVh6VmqW_6io-AgzimRA==");
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        OutputStream out = con.getOutputStream();
        try {
            String lineString = new String(line, StandardCharsets.UTF_8);
            logger.info("Output stream write line: " + lineString);
            out.write(line);
            out.flush();
        } finally {
            out.close();
        }

        int responseCode = con.getResponseCode();
        logger.info("Response code is " + responseCode);

        // Check if non 2XX response code.
        if (responseCode / 100 != 2) {
            throw new IOException(
                    "Server returned HTTP response code: " + responseCode + " for URL: " + url + " with content :'"
                    + con.getResponseMessage() + "'");
        }
        return responseCode;
    }

    @Override
    public Map<String, String> getTags() {
        return influxDbWriteObject.getTags();
    }

    protected InfluxDbWriteObject getWriteObject() {
        return this.influxDbWriteObject;
    }

    protected InfluxDbWriteObjectSerializer getSerializer() {
        return this.influxDbWriteObjectSerializer;
    }
}
