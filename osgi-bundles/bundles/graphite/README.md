# Graphite bundle

Metrics integration with Graphite.

## Testing

1. Copy `killbill-platform-osgi-bundles-graphite-*.jar` to `/var/tmp/bundles/platform/`
2. Start Graphite:
```
docker run \
 -p 8081:80 \
 -p 2003:2003 \
 ghcr.io/deniszh/graphite-statsd
```
3. In `killbill.properties`:
```
org.killbill.metrics.graphite=true
org.killbill.metrics.graphite.interval=1
```
4. Go to http://127.0.0.1:8081/
