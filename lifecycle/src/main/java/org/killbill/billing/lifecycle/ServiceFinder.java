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

package org.killbill.billing.lifecycle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceFinder<T> {

    private static final Logger log = LoggerFactory.getLogger(ServiceFinder.class);

    private final ClassLoader loader;
    private final String interfaceFilter;
    private final Set<Class<? extends T>> servicesTypes;

    public ServiceFinder(final ClassLoader loader, final String interfaceFilter) {
        this.loader = loader;
        this.interfaceFilter = interfaceFilter;
        this.servicesTypes = initialize();
        for (final Class<? extends T> svc : servicesTypes) {
            log.debug("Found service class {}", svc.getName());
        }
    }

    public Set<Class<? extends T>> getServices() {
        return servicesTypes;
    }

    private Set<Class<? extends T>> initialize() {
        try {

            final Set<String> packageFilter = new HashSet<String>();
            packageFilter.add("org.killbill.billing");
            final String jarFilter = "killbill";
            return findClasses(loader, interfaceFilter, jarFilter, packageFilter);
        } catch (final ClassNotFoundException nfe) {
            throw new RuntimeException("Failed to initialize ClassFinder", nfe);
        }
    }

    /*
     *  Code originally from Kris Dover <krisdover@hotmail.com> and adapted for my purpose.
     *
     */
    @SuppressWarnings("unchecked")
    private Set<Class<? extends T>> findClasses(final ClassLoader classLoader,
                                             final String interfaceFilter,
                                             final String jarFilter,
                                             final Set<String> packageFilter)
    throws ClassNotFoundException {

        final Set<Class<? extends T>> result = new HashSet<Class<? extends T>>();

        Object[] classPaths;
        try {
            classPaths = ((java.net.URLClassLoader) classLoader).getURLs();
        } catch (final ClassCastException cce) {
            classPaths = System.getProperty("java.class.path", "").split(File.pathSeparator);
        }

        for (final Object classPath1 : classPaths) {
            Enumeration<?> files = null;
            JarFile module = null;

            final String protocol;
            final File classPath;
            if ((URL.class).isInstance(classPath1)) {
                final URL urlClassPath = (URL) classPath1;
                classPath = new File(urlClassPath.getFile());
                protocol = urlClassPath.getProtocol();
            } else {
                classPath = new File(classPath1.toString());
                protocol = "file";
            }

            // Check if the protocol is "file". For example, if classPath is http://felix.extensions:9/,
            // the file will be "/" and we don't want to scan the full filesystem
            if ("file".equals(protocol) && classPath.isDirectory()) {
                log.debug("DIR : " + classPath);

                final List<String> dirListing = new ArrayList<String>();
                recursivelyListDir(dirListing, classPath, new StringBuffer());
                files = Collections.enumeration(dirListing);
            } else if (classPath.getName().endsWith(".jar")) {

                log.debug("JAR : " + classPath);

                final String[] jarParts = classPath.getName().split("/");
                final String jarName = jarParts[jarParts.length - 1];
                if (jarFilter != null && jarName != null && !jarName.startsWith(jarFilter)) {
                    continue;
                }
                boolean failed = true;
                try {
                    module = new JarFile(classPath);
                    failed = false;
                } catch (final MalformedURLException mue) {
                    throw new ClassNotFoundException("Bad classpath. Error: " + mue.getMessage());
                } catch (final IOException io) {
                    throw new ClassNotFoundException("jar file '" + classPath.getName() +
                                                     "' could not be instantiate from file path. Error: " + io.getMessage());
                }
                if (!failed) {
                    files = module.entries();
                }
            }

            while (files != null && files.hasMoreElements()) {
                final String fileName = files.nextElement().toString();

                if (fileName.endsWith(".class")) {
                    final String className = fileName.replaceAll("/", ".").substring(0, fileName.length() - 6);
                    if (packageFilter != null) {
                        boolean skip = true;
                        for (final String aPackageFilter : packageFilter) {
                            final String filter = aPackageFilter + ".";
                            if (className.startsWith(filter)) {
                                skip = false;
                                break;
                            }
                        }
                        if (skip) {
                            continue;
                        }
                    }
                    Class<?> theClass = null;
                    try {
                        theClass = Class.forName(className, false, classLoader);
                    } catch (final NoClassDefFoundError e) {
                        continue;
                    }
                    if (!theClass.isInterface()) {
                        continue;
                    }
                    final Class<?>[] classInterfaces = getAllInterfaces(theClass);
                    String interfaceName = null;
                    for (final Class<?> classInterface : classInterfaces) {

                        interfaceName = classInterface.getName();
                        if (!interfaceFilter.equals(interfaceName)) {
                            continue;
                        }
                        result.add((Class<? extends T>) theClass);
                        break;
                    }

                }
            }
            if (module != null) {
                try {
                    module.close();
                } catch (final IOException ioe) {
                    throw new ClassNotFoundException("The module jar file '" + classPath.getName() +
                                                     "' could not be closed. Error: " + ioe.getMessage());
                }
            }
        }
        return result;
    }

    private static Class<?>[] getAllInterfaces(final Class<?> theClass) {
        final Set<Class<?>> superInterfaces = new HashSet<Class<?>>();
        final Class<?>[] classInterfaces = theClass.getInterfaces();
        for (final Class<?> cur : classInterfaces) {
            getSuperInterfaces(superInterfaces, cur);
        }
        return superInterfaces.toArray(new Class<?>[superInterfaces.size()]);
    }

    private static void getSuperInterfaces(final Set<Class<?>> superInterfaces, final Class<?> theInterface) {

        superInterfaces.add(theInterface);
        final Class<?>[] classInterfaces = theInterface.getInterfaces();
        for (final Class<?> cur : classInterfaces) {
            getSuperInterfaces(superInterfaces, cur);
        }
    }

    private static void recursivelyListDir(final List<String> dirListing, final File dir, final StringBuffer relativePath) {
        int prevLen;
        if (dir.isDirectory()) {
            final File[] files = dir.listFiles();
            if (files == null) {
                return;
            }

            for (final File file : files) {
                prevLen = relativePath.length();
                recursivelyListDir(dirListing, file,
                                   relativePath.append(prevLen == 0 ? "" : "/").append(file.getName()));
                relativePath.delete(prevLen, relativePath.length());
            }
        } else {
            dirListing.add(relativePath.toString());
        }
    }
}
