package org.killbill.billing.platform.test.glue;

import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.platform.api.KillbillConfigSource;

import javax.annotation.Nullable;

public class TestPlatformModuleWithEmbeddedDB extends TestPlatformModule {

    public TestPlatformModuleWithEmbeddedDB(KillbillConfigSource configSource) {
        super(configSource, false, null, null);
    }

    public TestPlatformModuleWithEmbeddedDB(KillbillConfigSource configSource, boolean withOSGI, @Nullable OSGIConfigProperties osgiConfigProperties) {
        super(configSource, withOSGI, osgiConfigProperties, null);
    }
}
