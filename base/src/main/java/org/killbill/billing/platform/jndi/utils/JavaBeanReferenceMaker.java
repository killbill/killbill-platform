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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.NotSerializableException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import javax.naming.BinaryRefAddr;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Inspired from com.mchange.v2.naming.JavaBeanReferenceMaker - supports properties with getter/setter only
// TODO The whole package could be in commons
public class JavaBeanReferenceMaker {

    private static final Logger logger = LoggerFactory.getLogger(JavaBeanReferenceMaker.class);

    private static final Object[] EMPTY_ARGS = new Object[0];
    private static final byte[] NULL_TOKEN_BYTES = new byte[0];

    private final ReferenceIndirector indirector = new ReferenceIndirector();

    public Reference createReference(final Object bean) throws NamingException {
        final Collection<RefAddr> refAddrs = new ArrayList<RefAddr>();

        try {
            final BeanInfo bi = Introspector.getBeanInfo(bean.getClass());
            final PropertyDescriptor[] pds = bi.getPropertyDescriptors();

            for (final PropertyDescriptor pd : pds) {
                final String propertyName = pd.getName();
                final Class propertyType = pd.getPropertyType();
                final Method getter = pd.getReadMethod();
                final Method setter = pd.getWriteMethod();

                // Only use properties that are both readable and writable
                if (getter != null && setter != null) {
                    final Object val = getter.invoke(bean, EMPTY_ARGS);

                    final RefAddr refAddr = getRefAddr(pd, propertyName, propertyType, val);
                    refAddrs.add(refAddr);

                    logger.debug(this.getClass().getName() + ": propertyName=[" + propertyName + "], val=[" + val + "], RefAddr=[" + refAddr.toString() + "]");
                } else {
                    logger.debug(this.getClass().getName() + ": Skipping " + propertyName + " because it is " + (setter == null ? "read-only." : "write-only."));
                }
            }

            final Reference out = new Reference(bean.getClass().getName(), JavaBeanObjectFactory.class.getName(), null);
            for (final RefAddr refAddr : refAddrs) {
                out.add(refAddr);
            }

            return out;
        } catch (final Exception e) {
            throw new NamingException("Could not create reference from bean: " + e.toString());
        }
    }

    private RefAddr getRefAddr(final PropertyDescriptor pd, final String propertyName, final Class propertyType, final Object val) throws NotSerializableException {
        if (val == null) {
            return new BinaryRefAddr(propertyName, NULL_TOKEN_BYTES);
        } else if (Coerce.canCoerce(propertyType)) {
            return new StringRefAddr(propertyName, String.valueOf(val));
        } else {
            // Other Object properties
            final PropertyEditor pe = BeansUtils.findPropertyEditor(pd);
            if (pe != null) {
                pe.setValue(val);
                final String textValue = pe.getAsText();
                if (textValue != null) {
                    return new StringRefAddr(propertyName, textValue);
                }
            }

            // Property editor approach failed
            final byte[] valBytes = SerializableUtils.toByteArray(val, indirector, IndirectPolicy.INDIRECT_ON_EXCEPTION);
            return new BinaryRefAddr(propertyName, valBytes);
        }
    }
}
