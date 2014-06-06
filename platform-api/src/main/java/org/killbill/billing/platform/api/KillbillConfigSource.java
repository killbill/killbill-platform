package org.killbill.billing.platform.api;

public interface KillbillConfigSource {
    String getString(String propertyName);
}
