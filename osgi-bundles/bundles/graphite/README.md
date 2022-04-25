# Graphite bundle

Metrics integration with Graphite.

## Testing

1. Copy `killbill-platform-osgi-bundles-graphite-*.jar` to the OSGI bundle installation directory, see the value of `org.killbill.osgi.bundle.install.dir` property.
If the property is not configured, default directory is `/var/tmp/bundles/platform/`.


2. Start Graphite:
```
docker run \
 -p 8081:80 \
 -p 2003:2003 \
 ghcr.io/deniszh/graphite-statsd
```
3. Set the following properties in `killbill.properties`:
```
org.killbill.metrics.graphite=true
org.killbill.metrics.graphite.interval=1
```
4. Go to http://127.0.0.1:8081/
