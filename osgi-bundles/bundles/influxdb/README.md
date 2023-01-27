# InfluxDB bundle

Metrics integration with InfluxDB.


## Testing steps:

1. Copy `killbill-platform-osgi-bundles-influxdb-*.jar` to the OSGI bundle installation directory, see the value of `org.killbill.osgi.bundle.install.dir` property.
   If the property is not configured, the default OSGI bundle directory is `/var/tmp/bundles/platform/`.

NOTE: The OSGI bundle directory is the same as the KPM bundle path. For more information, please check [setting up KPM in Kaui](https://docs.killbill.io/latest/getting_started.html#_setting_up_kpm_in_kaui).


2. Start the InfluxDB server:
```
docker run
 -p 8086:8086 \
 influxdb:2.2
```

3. Go to http://localhost:8086/onboarding/0, click on Get Started.


4. On `Setup Initial User` screen, create a user, bucket and an organization. On next screen, click on 'Quick Start'.


5. Click on Data (or Load Your Data) menu -- go to API Tokens and copy it.


6. Configure the following properties as explained in the [Kill Bill configuration guide](https://docs.killbill.io/latest/userguide_configuration.html).
````
org.killbill.metrics.influxDb=true
org.killbill.metrics.influxDb.port=8086
org.killbill.metrics.influxDb.interval=10
org.killbill.metrics.influxDb.organization=<Organization>
org.killbill.metrics.influxDb.bucket=<Bucket name>
org.killbill.metrics.influxDb.token=<API Token got on step 4>
````

7. Restart Kill Bill.


8. On InfluxDB dashboard, click on `Explore` > `FROM` > `<Bucket Name>`


## InfluxDB properties: 
For more InfluxDB customization, please check the following properties with their default values:
```
org.killbill.metrics.influxDb=false
org.killbill.metrics.influxDb.host=localhost
org.killbill.metrics.influxDb.port=8086
org.killbill.metrics.influxDb.socketTimeout=1000
org.killbill.metrics.influxDb.database=killbill
org.killbill.metrics.influxDb.prefix=""
org.killbill.metrics.influxDb.senderType=HTTP
org.killbill.metrics.influxDb.organization=killbill
org.killbill.metrics.influxDb.bucket=killbill
org.killbill.metrics.influxDb.token=""
org.killbill.metrics.influxDb.interval=30
```