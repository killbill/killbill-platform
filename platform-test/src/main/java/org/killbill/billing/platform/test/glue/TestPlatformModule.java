package org.killbill.billing.platform.test.glue;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.bus.ExternalPersistentBusConfig;
import org.killbill.billing.lifecycle.glue.BusModule;
import org.killbill.billing.lifecycle.glue.PersistentBusProvider;
import org.killbill.billing.osgi.api.OSGIConfigProperties;
import org.killbill.billing.osgi.glue.DefaultOSGIModule;
import org.killbill.billing.platform.api.KillbillConfigSource;
import org.killbill.billing.platform.api.KillbillService;
import org.killbill.billing.platform.glue.KillBillModule;
import org.killbill.billing.platform.glue.MetricsModule;
import org.killbill.billing.platform.glue.NotificationQueueModule;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;

import javax.annotation.Nullable;
import java.util.Set;

public abstract class TestPlatformModule extends KillBillModule {

    private static final String EXTERNAL_BUS = "externalBus";

    private final boolean withOSGI;
    private final OSGIConfigProperties osgiConfigProperties;
    private final Set<? extends KillbillService> services;

    protected TestPlatformModule(KillbillConfigSource configSource, boolean withOSGI, @Nullable  final OSGIConfigProperties osgiConfigProperties, @Nullable final Set<? extends KillbillService> services) {
        super(configSource);
        this.withOSGI = withOSGI;
        this.osgiConfigProperties = osgiConfigProperties;
        this.services = services;
    }

    @Override
    protected void configure() {
        configureLifecycle();

        configureBus();
        // For the bus
        install(new MetricsModule(configSource));

        if (withOSGI) {
            configureOSGI();
        }
    }

    protected void configureLifecycle() {
        if (services != null) {
            bind(Lifecycle.class).toInstance(new DefaultLifecycle(services));
        } else {
            bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
        }
    }

    protected void configureBus() {
        install(new BusModule(BusModule.BusType.PERSISTENT, configSource));
    }

    protected void configureNotificationQ() {
        install(new NotificationQueueModule(configSource));
    }

    protected void configureOSGI() {
        final PersistentBusConfig extBusConfig = new ExternalPersistentBusConfig(skifeConfigSource);
        install(new DefaultOSGIModule(configSource, osgiConfigProperties));

        bind(PersistentBusProvider.class).annotatedWith(Names.named(EXTERNAL_BUS)).toInstance(new PersistentBusProvider(extBusConfig));
        bind(PersistentBus.class).annotatedWith(Names.named(EXTERNAL_BUS)).toProvider(Key.get(PersistentBusProvider.class, Names.named(EXTERNAL_BUS))).asEagerSingleton();
    }
}
