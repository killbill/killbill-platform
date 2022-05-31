# InfluxDB bundle

Metrics integration with InfluxDB.

## Testing steps:

1. Start the InfluxDB server:
```
docker run
 -p 8086:8086 \
 influxdb:2.2
```

2. Go to http://localhost:8086/onboarding/0, click on Get Started.


3. On `Setup Initial User` screen, create a user, bucket and an organization. On next screen, click on 'Quick Start'.


4. Click on Data (or Load Your Data) menu -- go to API Tokens and copy it.


5. Add the following properties in `killbill-server.properties`:
````
org.killbill.metrics.influxDb=true
org.killbill.metrics.influxDb.port=8086
org.killbill.metrics.influxDb.interval=10
org.killbill.metrics.influxDb.organization=<Organization>
org.killbill.metrics.influxDb.bucket=<Bucket name>
org.killbill.metrics.influxDb.token=<API Token got on step 4>
````
