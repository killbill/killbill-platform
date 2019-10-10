# KPM OSGI bundle

The KPM OSGI bundle enables:

* the `INSTALL` and `UNINSTALL` commands of the [Kill Bill plugins management APIs](https://github.com/killbill/killbill-docs/blob/v3/userguide/tutorials/plugin_management.adoc)
* endpoints specific to [KPM UI](https://github.com/killbill/killbill-kpm-ui)

## Endpoints

List available plugins:

```
curl -v \
     -u admin:password \
     http://127.0.0.1:8080/plugins/killbill-kpm/plugins?latest=true
```
