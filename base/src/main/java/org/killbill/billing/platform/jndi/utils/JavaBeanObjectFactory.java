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
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaBeanObjectFactory implements ObjectFactory {

    private static final Logger logger = LoggerFactory.getLogger(JavaBeanObjectFactory.class);

    private static final Object NULL_TOKEN = new Object();

    public Object getObjectInstance(final Object refObj, final Name name, final Context nameCtx, final Hashtable env) throws Exception {
        if (refObj instanceof Reference) {
            final Reference ref = (Reference) refObj;

            final Map<String, RefAddr> refAddrsMap = new HashMap<String, RefAddr>();
            for (final Enumeration e = ref.getAll(); e.hasMoreElements(); ) {
                final RefAddr addr = (RefAddr) e.nextElement();
                refAddrsMap.put(addr.getType(), addr);
            }

            final Class beanClass = Class.forName(ref.getClassName());
            final Map propMap = createPropertyMap(beanClass, refAddrsMap);

            return findBean(beanClass, propMap);
        } else {
            return null;
        }
    }

    private Map createPropertyMap(final Class beanClass, final Map refAddrsMap) throws Exception {
        final BeanInfo bi = Introspector.getBeanInfo(beanClass);
        final PropertyDescriptor[] pds = bi.getPropertyDescriptors();

        final Map<String, Object> out = new HashMap<String, Object>();
        for (final PropertyDescriptor pd : pds) {
            final String propertyName = pd.getName();
            final Class propertyType = pd.getPropertyType();
            final Object addr = refAddrsMap.remove(propertyName);
            if (addr != null) {
                if (addr instanceof StringRefAddr) {
                    final String content = (String) ((StringRefAddr) addr).getContent();
                    if (Coerce.canCoerce(propertyType)) {
                        out.put(propertyName, Coerce.toObject(content, propertyType));
                    } else {
                        final PropertyEditor pe = BeansUtils.findPropertyEditor(pd);
                        pe.setAsText(content);
                        out.put(propertyName, pe.getValue());
                    }
                } else if (addr instanceof BinaryRefAddr) {
                    final byte[] content = (byte[]) ((BinaryRefAddr) addr).getContent();
                    if (content.length == 0) {
                        out.put(propertyName, NULL_TOKEN); // we use an empty array to mean null
                    } else {
                        out.put(propertyName, SerializableUtils.fromByteArray(content)); // this will handle "indirectly serialized" objects.
                    }
                } else {
                    logger.warn(this.getClass().getName() + " -- unknown RefAddr subclass: " + addr.getClass().getName());
                }
            }
        }

        for (final Object o : refAddrsMap.keySet()) {
            final String type = (String) o;
            logger.warn(this.getClass().getName() + " -- RefAddr for unknown property: " + type);
        }
        return out;
    }

    protected Object createBlankInstance(final Class beanClass) throws Exception {
        return beanClass.newInstance();
    }

    protected Object findBean(final Class beanClass, final Map propertyMap) throws Exception {
        final Object bean = createBlankInstance(beanClass);
        final BeanInfo bi = Introspector.getBeanInfo(bean.getClass());
        final PropertyDescriptor[] pds = bi.getPropertyDescriptors();

        for (final PropertyDescriptor pd : pds) {
            final String propertyName = pd.getName();
            final Object value = propertyMap.get(propertyName);
            final Method setter = pd.getWriteMethod();
            if (value != null) {
                if (setter != null) {
                    setter.invoke(bean, new Object[]{(value == NULL_TOKEN ? null : value)});
                } else {
                    logger.warn(this.getClass().getName() + ": Could not restore read-only property '" + propertyName + "'.");
                }
            } else {
                if (setter != null) {
                    logger.debug(this.getClass().getName() + " -- Expected writable property ''" + propertyName + "'' left at default value");
                }
            }
        }

        return bean;
    }
}
