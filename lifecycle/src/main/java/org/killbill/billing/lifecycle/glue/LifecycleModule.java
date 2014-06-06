package org.killbill.billing.lifecycle.glue;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.killbill.billing.lifecycle.DefaultLifecycle;
import org.killbill.billing.lifecycle.api.Lifecycle;
import org.killbill.billing.lifecycle.bus.ExternalPersistentBusConfig;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;
import org.skife.config.ConfigSource;

public class LifecycleModule extends AbstractModule {

    public static final String EXTERNAL_BUS = "externalBus";

    private final ConfigSource configSource;

    public LifecycleModule(final ConfigSource configSource) {
        this.configSource = configSource;
    }

    @Override
    protected void configure() {
        installLifecycle();
        installExternalBus();
    }

    protected void installLifecycle() {
        bind(Lifecycle.class).to(DefaultLifecycle.class).asEagerSingleton();
    }

    protected void installExternalBus() {
        final PersistentBusConfig extBusConfig = new ExternalPersistentBusConfig(configSource);

        bind(PersistentBusProvider.class).annotatedWith(Names.named(EXTERNAL_BUS)).toInstance(new PersistentBusProvider(extBusConfig));
        bind(PersistentBus.class).annotatedWith(Names.named(EXTERNAL_BUS)).toProvider(Key.get(PersistentBusProvider.class, Names.named(EXTERNAL_BUS))).asEagerSingleton();
    }
}

