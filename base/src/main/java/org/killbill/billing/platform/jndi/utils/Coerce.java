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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Coerce {

    private Coerce() {}

    private static final Set<Class> CAN_COERCE;

    static {
        final Class[] classes =
                {
                        byte.class,
                        boolean.class,
                        char.class,
                        short.class,
                        int.class,
                        long.class,
                        float.class,
                        double.class,
                        String.class,
                        Byte.class,
                        Boolean.class,
                        Character.class,
                        Short.class,
                        Integer.class,
                        Long.class,
                        Float.class,
                        Double.class
                };

        final Set<Class> tmp = new HashSet<Class>(Arrays.<Class>asList(classes));
        CAN_COERCE = Collections.<Class>unmodifiableSet(tmp);
    }

    public static boolean canCoerce(final Class cl) {
        return CAN_COERCE.contains(cl);
    }

    public static int toInt(final String s) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException e) {
            return (int) Double.parseDouble(s);
        }
    }

    public static byte toByte(final String s) {
        return (byte) toInt(s);
    }

    public static short toShort(final String s) {
        return (short) toInt(s);
    }

    public static char toChar(String s) {
        s = s.trim();
        if (s.length() == 1) {
            return s.charAt(0);
        } else {
            return (char) toInt(s);
        }
    }

    public static Object toObject(final String s, Class type) {
        if (type == byte.class) {
            type = Byte.class;
        } else if (type == boolean.class) {
            type = Boolean.class;
        } else if (type == char.class) {
            type = Character.class;
        } else if (type == short.class) {
            type = Short.class;
        } else if (type == int.class) {
            type = Integer.class;
        } else if (type == long.class) {
            type = Long.class;
        } else if (type == float.class) {
            type = Float.class;
        } else if (type == double.class) {
            type = Double.class;
        }

        if (type == String.class) {
            return s;
        } else if (type == Byte.class) {
            return new Byte(toByte(s));
        } else if (type == Boolean.class) {
            return Boolean.valueOf(s);
        } else if (type == Character.class) {
            return new Character(toChar(s));
        } else if (type == Short.class) {
            return new Short(toShort(s));
        } else if (type == Integer.class) {
            return new Integer(s);
        } else if (type == Long.class) {
            return new Long(s);
        } else if (type == Float.class) {
            return new Float(s);
        } else if (type == Double.class) {
            return new Double(s);
        } else {
            throw new IllegalArgumentException("Cannot coerce to type: " + type.getName());
        }
    }
}
