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

package org.killbill.billing.platform.api;

/**
 * The interface <code>KillbillService<code/> represents a service that will go through the Killbill lifecyle.
 * <p/>
 * A <code>KillbillService<code> can register handlers for the various phases of the lifecycle, so
 * that its proper initialization/shutdown sequence occurs at the right time with regard
 * to other <code>KillbillService</code>.
 */
public interface KillbillService {

    public static class ServiceException extends Exception {

        private static final long serialVersionUID = 176191207L;

        public ServiceException() {
            super();
        }

        public ServiceException(final String msg, final Throwable e) {
            super(msg, e);
        }

        public ServiceException(final String msg) {
            super(msg);
        }

        public ServiceException(final Throwable msg) {
            super(msg);
        }
    }

    /**
     * @return the name of the service
     */
    public String getName();

    /**
     *
     */
    public int getRegistrationOrdering();

    // Known services
    public enum KILLBILL_SERVICES {

        /* Platform range 0-100 */
        NODES_SERVICE("nodes-service", 10),
        BUS_SERVICE("bus-service", 20),
        EXTERNAL_BUS_SERVICE("external-bus-service", 30),
        SECURITY_SERVICE("security-service", 40),
        CONFIG_SERVICE("config-service", 50),
        BROADCAST_SERVICE("broadcast-service", 60),
        RETRIABLE_BUS_HANDLER_SERVICE("extBusEvent-listener-service", 70),
        /* Kill Bill core 100 - 500 */
        TENANT_SERVICE("tenant-service", 110),
        CATALOG_SERVICE("catalog-service", 120),
        ACCOUNT_SERVICE("account-service", 130),
        SUBSCRIPTION_BASE_SERVICE("subscription-service", 140),
        ENTITLEMENT_SERVICE("entitlement-service", 150),
        INVOICE_SERVICE("invoice-service", 160),
        PAYMENT_SERVICE("payment-service", 170),
        OVERDUE_SERVICE("overdue-service", 180),
        CURRENCY_SERVICE("currency-service", 190),
        JAXRS_SERVICE("jaxrs-service", 200),
        /*  Apis, plugins, push notifications */
        BEATRIX_SERVICE("beatrix-service", 500),
        SERVER_SERVICE("server-service", 510),
        OSGI_SERVICE("osgi-service", 520);

        String serviceName;
        int registrationOrdering;

        KILLBILL_SERVICES(final String serviceName, final int registrationOrdering) {
            this.serviceName = serviceName;
            this.registrationOrdering = registrationOrdering;
        }

        public String getServiceName() {
            return serviceName;
        }

        public int getRegistrationOrdering() {
            return registrationOrdering;
        }
    };

}
