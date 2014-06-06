package org.killbill.billing.platform.test.glue;

import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.platform.api.KillbillConfigSource;

public class TestPlatformModuleNoDB extends TestPlatformModule {

    public TestPlatformModuleNoDB(KillbillConfigSource configSource) {
        super(configSource, false, null, null);
    }

    @Override
    protected void configureBus() {
        install(new BusModule(BusModule.BusType.MEMORY, configSource));
    }
}
