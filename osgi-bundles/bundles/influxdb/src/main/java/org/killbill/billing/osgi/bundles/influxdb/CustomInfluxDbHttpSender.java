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
    private final int connectTimeout;
    private final int readTimeout;
    static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final InfluxDbWriteObject influxDbWriteObject;
    private final InfluxDbWriteObjectSerializer influxDbWriteObjectSerializer;
    private final String token;

    /**
     * Creates a new http sender given connection details.
     *
     * @param hostname       the influxDb hostname
     * @param port           the influxDb http port
     * @param database       the influxDb database to write to
     * @param timePrecision  the time precision of the metrics
     * @param connectTimeout the connect timeout
     * @param readTimeout    the read timeout
     * @throws Exception while creating the influxDb sender(MalformedURLException)
     */
    public CustomInfluxDbHttpSender(final String protocol, final String hostname, final int port, final String database,
                                    final TimeUnit timePrecision, final int connectTimeout, final int readTimeout,
                                    final String measurementPrefix, final String organization, final String bucket,
                                    final String token) throws Exception {

        this.influxDbWriteObject = new InfluxDbWriteObject(database, timePrecision);
        this.influxDbWriteObjectSerializer = new CustomInfluxDbWriteObjectSerializer(measurementPrefix);

        final String endpoint = new URL(protocol, hostname, port, "/api/v2/write").toString();
        final String queryOrg = String.format("org=%s&bucket=%s", organization, bucket);
        this.url = new URL(endpoint + "?" + queryOrg);

        logger.info("InfluxDB write request will be sent to this endpoint: " + endpoint);

        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.token = token;
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
    public void appendPoints(final InfluxDbPoint point) {
        if (point != null) {
            influxDbWriteObject.getPoints().add(point);
        }
    }

    @Override
    public void setTags(final Map<String, String> tags) {
        if (tags != null) {
            influxDbWriteObject.setTags(tags);
        }
    }

    @Override
    public int writeData() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Token " + token);
        con.setDoOutput(true);
        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        final String lineProtocolString = influxDbWriteObjectSerializer.getLineProtocolString(influxDbWriteObject);
        final byte[] line = lineProtocolString.getBytes(UTF_8);

        logger.debug("InfluxDB data points to write: " + new String(line, StandardCharsets.UTF_8));

        try (final OutputStream out = con.getOutputStream()) {
            out.write(line);
            out.flush();
        }

        final int responseCode = con.getResponseCode();
        con.disconnect();
        // Check if non 2XX response code.
        if (responseCode / 100 != 2) {
            throw new IOException(
                    "Server returned HTTP response code: " + responseCode + " for URL: " + url + " with content :'"
                    + con.getResponseMessage() + "'");
        }

        logger.debug("InfluxDB write data response code: " + responseCode);

        return responseCode;
    }

    @Override
    public Map<String, String> getTags() {
        return influxDbWriteObject.getTags();
    }
}
