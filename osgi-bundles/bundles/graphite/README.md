# Graphite bundle

Metrics integration with Graphite.


## Testing steps:

1. Copy `killbill-platform-osgi-bundles-graphite-*.jar` to the OSGI bundle installation directory, see the value of `org.killbill.osgi.bundle.install.dir` property.
If the property is not configured, the default OSGI bundle directory is `/var/tmp/bundles/platform/`. 

NOTE: The OSGI bundle directory is the same as the KPM bundle path. For more information, please check [setting up KPM in Kaui](https://docs.killbill.io/latest/getting_started.html#_setting_up_kpm_in_kaui).


2. Start Graphite:
```
docker run \
 -p 8081:80 \
 -p 2003:2003 \
 ghcr.io/deniszh/graphite-statsd
```

3. Configure the following properties as explained in the [Kill Bill configuration guide](https://docs.killbill.io/latest/userguide_configuration.html).
```
org.killbill.metrics.graphite=true
org.killbill.metrics.graphite.interval=1
```

4. Restart Kill Bill.


5. Go to http://127.0.0.1:8081/. 

On the Graphite dashboard, Killbill metrics are under -- `Metrics > killbill`.

## Graphite properties:
For more Graphite customization, please check the following properties with their default values:
```
org.killbill.metrics.graphite=false
org.killbill.metrics.graphite.host=localhost
org.killbill.metrics.graphite.port=2003
org.killbill.metrics.graphite.interval=30
org.killbill.metrics.graphite.prefix=killbill
```