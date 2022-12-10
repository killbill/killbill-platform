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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.izettle.metrics.influxdb.data.InfluxDbPoint;
import com.izettle.metrics.influxdb.data.InfluxDbWriteObject;
import com.izettle.metrics.influxdb.utils.InfluxDbWriteObjectSerializer;

public class CustomInfluxDbWriteObjectSerializer extends InfluxDbWriteObjectSerializer {

    private static final Pattern DOUBLE_QUOTE = Pattern.compile("\"");
    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern SPACE = Pattern.compile(" ");
    private static final Pattern EQUAL = Pattern.compile("=");
    private final String measurementPrefix;

    public CustomInfluxDbWriteObjectSerializer(final String measurementPrefix) {
        super(measurementPrefix);
        this.measurementPrefix = measurementPrefix;
    }

    // measurement[,tag=value,tag2=value2...] field=value[,field2=value2...]

    /**
     * Calculate the lineprotocol for all Points.
     *
     * @return the String with newLines.
     */
    @Override
    public String getLineProtocolString(final InfluxDbWriteObject influxDbWriteObject) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final InfluxDbPoint point : influxDbWriteObject.getPoints()) {
            pointLineProtocol(point, stringBuilder);
            stringBuilder.append(" ");
            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    private void pointLineProtocol(final InfluxDbPoint point, final StringBuilder stringBuilder) {
        lineProtocol(point.getTags(), point.getFields(), point.getMeasurement(), stringBuilder);
    }

    private void lineProtocol(final Map<String, String> tags, final Map<String, Object> fields,
                              final String measurement, final StringBuilder stringBuilder) {
        stringBuilder.append(escapeMeasurement(measurementPrefix + measurement));
        concatenatedTags(tags, stringBuilder);
        concatenateFields(fields, stringBuilder);
    }

    private void concatenatedTags(final Map<String, String> tags, final StringBuilder stringBuilder) {
        for (final Map.Entry<String, String> tag : tags.entrySet()) {
            stringBuilder.append(",");
            stringBuilder.append(escapeKey(tag.getKey())).append("=").append(escapeKey(tag.getValue()));
        }
        stringBuilder.append(" ");
    }

    private void concatenateFields(final Map<String, Object> fields, final StringBuilder stringBuilder) {
        final NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        numberFormat.setMaximumFractionDigits(340);
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumFractionDigits(1);

        boolean firstField = true;
        for (final Map.Entry<String, Object> field : fields.entrySet()) {
            final Object value = field.getValue();
            if (value instanceof Double) {
                final Double doubleValue = (Double) value;
                if (doubleValue.isNaN() || doubleValue.isInfinite()) {
                    continue;
                }
            } else if (value instanceof Float) {
                final Float floatValue = (Float) value;
                if (floatValue.isNaN() || floatValue.isInfinite()) {
                    continue;
                }
            }

            if (!firstField) {
                stringBuilder.append(",");
            }
            stringBuilder.append(escapeKey(field.getKey())).append("=");
            firstField = false;
            if (value instanceof String) {
                final String stringValue = (String) value;
                stringBuilder.append("\"").append(escapeField(stringValue)).append("\"");
            } else if (value instanceof Number) {
                stringBuilder.append(numberFormat.format(value));
            } else if (value instanceof Boolean) {
                stringBuilder.append(value);
            } else {
                stringBuilder.append("\"").append(escapeField(value.toString())).append("\"");
            }
        }
    }

    private String escapeField(final String field) {
        return DOUBLE_QUOTE.matcher(field).replaceAll("\\\"");
    }

    private String escapeMeasurement(final String key) {
        final String toBeEscaped = SPACE.matcher(key).replaceAll("\\\\ ");
        return COMMA.matcher(toBeEscaped).replaceAll("\\\\,");
    }

    private String escapeKey(final String key) {
        String toBeEscaped = SPACE.matcher(key).replaceAll("\\\\ ");
        toBeEscaped = COMMA.matcher(toBeEscaped).replaceAll("\\\\,");
        return EQUAL.matcher(toBeEscaped).replaceAll("\\\\=");
    }
}
