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

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeansUtils {

    private static final Logger logger = LoggerFactory.getLogger(BeansUtils.class);

    private BeansUtils() {}

    public static PropertyEditor findPropertyEditor(final PropertyDescriptor pd) {
        PropertyEditor out = null;
        Class editorClass = null;
        try {
            editorClass = pd.getPropertyEditorClass();
            if (editorClass != null) {
                out = (PropertyEditor) editorClass.newInstance();
            }
        } catch (final Exception e) {
            logger.warn("Bad property editor class " + (editorClass == null ? "" : editorClass.getName()) + " registered for property " + pd.getName(), e);
        }

        if (out == null) {
            out = PropertyEditorManager.findEditor(pd.getPropertyType());
        }

        return out;
    }
}
