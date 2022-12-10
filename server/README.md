# Kill Bill skeleton server

This skeleton server can be quite helpful to quickly test plugins, without having to start a full Kill Bill server.

To use it:

* Create the plugin directory (`mkdir -p /var/tmp/bundles/plugins/java/foo/1.0`) and copy to it your plugin jar.
* Start the Jetty server: `mvn jetty:run`
* To restart the plugin without having to restart the Jetty
  server: `touch /var/tmp/bundles/plugins/java/foo/1.0/tmp/restart.txt`
    * This relies on a specific Jetty configuration, see the pom.xml (the Kill Bill restart mechanism only invokes
      stop/start, it doesn't reload the jar).

## H2

To have additional tables installed in H2, you can pass DDL files to the command line, e.g.:

```
-Dorg.killbill.dao.additionalSeedFiles=file:/path/to/killbill-analytics-plugin/src/main/resources/org/killbill/billing/plugin/analytics/ddl.sql
```

Notes:

* For safety, if a database already exists, DDL files aren't executed. Run `rm -f /var/tmp/killbill.*` to re-run them.
* H2 UI is at http://127.0.0.1:8082/
```
url: jdbc:h2:file:/var/tmp/killbill
username: killbill
password: killbill
```
