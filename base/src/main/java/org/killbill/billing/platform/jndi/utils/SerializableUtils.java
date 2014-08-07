/*
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

package org.killbill.billing.platform.jndi.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.killbill.billing.platform.jndi.utils.ReferenceIndirector.ReferenceSerialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializableUtils {

    private static final Logger logger = LoggerFactory.getLogger(SerializableUtils.class);

    private SerializableUtils() {}

    public static byte[] toByteArray(final Object obj, final ReferenceIndirector indirector, final IndirectPolicy policy) throws NotSerializableException {
        try {
            if (policy == IndirectPolicy.DEFINITELY_INDIRECT) {
                if (indirector == null) {
                    throw new IllegalArgumentException("null indirector is not consistent with " + policy);
                }

                final ReferenceSerialized indirect = indirector.indirectForm(obj);
                return toByteArray(indirect);
            } else if (policy == IndirectPolicy.INDIRECT_ON_EXCEPTION) {
                if (indirector == null) {
                    throw new IllegalArgumentException("null indirector is not consistent with " + policy);
                }

                try {
                    return toByteArray(obj);
                } catch (final NotSerializableException e) {
                    return toByteArray(obj, indirector, IndirectPolicy.DEFINITELY_INDIRECT);
                }
            } else if (policy == IndirectPolicy.DEFINITELY_DIRECT) {
                return toByteArray(obj);
            } else {
                throw new InternalError("unknown indirecting policy: " + policy);
            }
        } catch (final NotSerializableException e) {
            throw e;
        } catch (final Exception e) {
            logger.warn("An Exception occurred while serializing an Object to a byte[] with an Indirector.", e);
            throw new NotSerializableException(e.toString());
        }
    }

    /**
     * By default, unwraps IndirectlySerialized objects, returning the original
     */
    public static Object fromByteArray(final byte[] bytes) throws IOException, ClassNotFoundException {
        final ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        final Object out = in.readObject();
        if (out instanceof ReferenceSerialized) {
            return ((ReferenceSerialized) out).getObject();
        } else {
            return out;
        }
    }

    private static byte[] toByteArray(final Object obj) throws NotSerializableException {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            return baos.toByteArray();
        } catch (final NotSerializableException e) {
            // This is the only IOException that shouldn't signal a bizarre error...
            e.fillInStackTrace();
            throw e;
        } catch (final IOException e) {
            throw new Error("IOException writing to a byte array!");
        }
    }
}
