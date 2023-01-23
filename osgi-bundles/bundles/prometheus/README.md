# Prometheus bundle

Metrics integration with Prometheus.

The default Killbill metrics endpoint is `http://<KB host>:<KB Port>/plugins/killbill-prometheus`.

For example, http://127.0.0.1:8080/plugins/killbill-prometheus.

## Testing

1. Copy `killbill-platform-osgi-bundles-prometheus-*.jar` to the OSGI bundle installation directory, see the value of `org.killbill.osgi.bundle.install.dir` property.
   If the property is not configured, the default OSGI bundle directory is `/var/tmp/bundles/platform/`.

NOTE: The OSGI bundle directory is the same as the KPM bundle path. For more information, please check [setting up KPM in Kaui](https://docs.killbill.io/latest/getting_started.html#_setting_up_kpm_in_kaui).


2. Create a `prometheus.yml` file with the following configuration:

NOTE: Use machine's IP in `targets` even if the Killbill is running on a localhost. Example, `targets: ['172.19.32.1:8080']`
```
scrape_configs:
  - job_name: killbill
    scrape_interval: 30s
    scrape_timeout: 25s 
    static_configs:
        - targets: ['<machine's IP where Killbill is running>:<Killbill port>']
    metrics_path: /plugins/killbill-prometheus
    scheme: http
```

3. Run Prometheus on Docker. In the following command, replace the `prometheus.yml` path.
```
docker run \
    -p 9091:9090 \
    -v /path/to/prometheus.yml:/etc/prometheus/prometheus.yml \
    prom/prometheus
```

4. Go to http://localhost:9091/ to access the Prometheus dashboard.


5. Verify that Killbill endpoint is UP by clicking on Status > Targets. If the endpoint state is not UP, verify the IP address and port in `prometheus.yml`.


6. Sample curl expression to fetch all metrics: http://localhost:9091/api/v1/label/__name__/values
