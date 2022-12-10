# Kill Bill platform
![Maven Central](https://img.shields.io/maven-central/v/org.kill-bill.billing/killbill-platform?color=blue&label=Maven%20Central)

Underlying platform powering Kill Bill. It features:

* JNDI management
* Lifecycle
* Services discovery
* OSGI and plugin layer, including the JRuby bridge
* Container integration glue

The platform is billing and payment agnostic, and can be used to create other services.

## Kill Bill compatibility

| Platform version | Kill Bill version |
| ---------------: | ----------------: |
| 0.26.y           | 0.16.z            |
| 0.36.y           | 0.18.z            |
| 0.37.y           | 0.19.z            |
| 0.38.y           | 0.20.z            |
| 0.39.y           | 0.22.z            |
| 0.40.y           | 0.22.z            |
| 0.41.y           | 0.24.z            |

We've upgraded numerous dependencies in 0.40.x (required for Java 11 support).

## Usage

Add the relevant submodule(s) to a project:

```xml
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-api</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-base</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-lifecycle</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-all-bundles</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-api</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-defaultbundles</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-eureka</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-graphite</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-influxdb</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-kpm</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-lib-killbill</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-lib-slf4j-osgi</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-logger</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-metrics</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-test-beatrix</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-bundles-test-payment</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-lib-bundles</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-osgi-test-bundles</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-server</artifactId>
    <version>... release version ...</version>
</dependency>
<dependency>
    <groupId>org.kill-bill.billing</groupId>
    <artifactId>killbill-platform-test</artifactId>
    <version>... release version ...</version>
</dependency>
```

## About

Kill Bill is the leading Open-Source Subscription Billing & Payments Platform. For more information about the project, go to https://killbill.io/.
