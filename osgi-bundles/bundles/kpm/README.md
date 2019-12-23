# KPM OSGI bundle

The KPM OSGI bundle enables:

* the `INSTALL` and `UNINSTALL` commands of the [Kill Bill plugins management APIs](https://github.com/killbill/killbill-docs/blob/v3/userguide/tutorials/plugin_management.adoc)
* endpoints specific to [KPM UI](https://github.com/killbill/killbill-kpm-ui)

## Configuration

Available global configuration properties:

* `org.killbill.billing.plugin.kpm.kpmPath` (default: `kpm`)
* `org.killbill.billing.plugin.kpm.bundlesPath` (default: `/var/tmp/bundles`)
* `org.killbill.billing.plugin.kpm.nexusUrl` (default: `https://oss.sonatype.org`)
* `org.killbill.billing.plugin.kpm.nexusRepository` (default: `releases`)
* `org.killbill.billing.plugin.kpm.strictSSL` (default: `true`)
* `org.killbill.billing.plugin.kpm.readTimeoutSec` (default: `60`)
* `org.killbill.billing.plugin.kpm.connectTimeoutSec` (default: `60`)

## Endpoints

List available plugins:

```
curl -v \
     -u admin:password \
     http://127.0.0.1:8080/plugins/killbill-kpm/plugins?latest=true
```
