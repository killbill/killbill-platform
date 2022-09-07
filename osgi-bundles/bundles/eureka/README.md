# Kill Bill Eureka Module Installation

## Kill Bill Eureka Plugin Installation

1. Follow [Plugin Installation](https://docs.killbill.io/latest/plugin_installation.html) section in Kill Bill documentation. You should be able to install plugins successfully.
2. Install Kill Bill Eureka plugin. 
3. Check installation status and make sure that the plugin JAR file exist. (for example, by `ls`-ing `/var/tmp/bundles` sub-directory)


## Eureka Server setup

1. Make sure there's one Eureka server running.
2. For development/test, you can use Spring's framework cloud starter:
   
   2.1. Download scaffolding project with Eureka as dependency from start.spring.io 
        [here](https://start.spring.io/#!type=maven-project&language=java&platformVersion=2.7.3&packaging=jar&jvmVersion=11&groupId=com.example&artifactId=demo&name=demo&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.demo&dependencies=cloud-eureka-server). 
   
   2.2. Extract and add minimal configuration to `<extracted-dir>/src/main/resources/application.properties` as follows:
   ```
   server.port=8761
   eureka.client.registerWithEureka=false
   eureka.client.fetchRegistry=false
   eureka.instance.hostname=localhost
   ```

   2.3. Open console/command line, go to `<extracted-dir>`, and run: `./mvnw spring-boot:run`.


## Kill Bill instance setup

1. Make sure that you have MySQL/Postgres running and configured for run with Kill Bill. 
   [Read here](https://docs.killbill.io/latest/development.html#_configuring_the_database) for more detail. 
   (At the time of writing, H2 not working properly when run Kill Bill more than 1 instances).

2. (Optional. Please delete this account at production).
   To interact with Kill Bill REST API, you need at least one `tenants` account. Execute this to add one `tenants` account:
   ```roomsql
   INSERT INTO tenants (record_id, id, external_key, api_key, api_secret, api_salt, created_date, created_by, updated_date, updated_by) 
   VALUES 
   (1, 'f76d3b8a-2fe9-4538-b434-a5dbf51b2d27', null, 'bob', 'iJTgdUDR/6RZF3lgBNtKxXZ+tPadfjHtQtykVq6yRkEecrlWp/wkWJ65G2EeHMfOpjVQ9XfYKyGYy86tMFT5pw==', 'IGfdQIzGWp7AbQ5Xx6h07w==', '2022-09-03 08:27:55', 'demo', '2022-09-03 08:28:06', 'demo');
   ```
   This will create `tenants` with `API-KEY: bob, API-SECRET: lazar`.

3. Create configuration file (for example, `killbill-eureka.properties`):
   ```properties

   # Database config
   org.killbill.dao.url=jdbc:mysql://127.0.0.1:3306/killbill_dev
   org.killbill.dao.user=root
   org.killbill.dao.password=admin

   # Database config (OSGI plugins)
   org.killbill.billing.osgi.dao.url=jdbc:mysql://127.0.0.1:3306/killbill_osgi_dev
   org.killbill.billing.osgi.dao.user=root
   org.killbill.billing.osgi.dao.password=admin

   # Eureka client specifics configuration. Read more https://github.com/Netflix/eureka/wiki/Configuring-Eureka
   eureka.serviceUrl.default=http://localhost:8761/eureka

   eureka.registration.enabled=true
   eureka.name=killbill
   eureka.port.enabled=true
   eureka.securePort.enabled=false

   eureka.statusPageUrlPath=/1.0/metrics
   eureka.healthCheckUrlPath=/1.0/healthCheck

   eureka.decoderName=JacksonJson
   eureka.preferSameZone=true
   eureka.shouldUseDns=false

   # Enable eureka in Kill Bill
   org.killbill.eureka=true

   # Kill Bill plugins root directory. If not set, the value would be "/var/tmp/bundles"
   org.killbill.osgi.bundle.install.dir=<killbill-plugins-dir>
   ```


## Running and Testing

1. Make sure that plugin installation configured properly, and database and Eureka running.

2. Run Kill Bill instance with command:
   ```
   # First instance:
   mvn jetty:run \
     -Dorg.killbill.server.properties=file:///killbill-eureka.properties \
     -Djetty.http.port=8080
   
   # Second instance:
   mvn jetty:run \
     -Dorg.killbill.server.properties=file:///killbill-eureka.properties \
     -Djetty.http.port=8081
   ```
3. If everything works properly, you can open Eureka Spring Console at http://localhost:8761 in browser, and see that 2 Kill Bill instances discovered by Eureka, and the status is `UP`.

4. Try to `put-out` and `put-in` Kill Bill from Eureka, for example by calling this using `killbill-client-java`:
   ```java
   public class EurekaServiceRegistryTest extends Playground { 
     @Test void putOutRotation() throws KillBillClientException {
       final AdminApi adminApi = new AdminApi(new KillBillHttpClient());
       adminApi.putOutOfRotation(RequestOptions.empty());
     }
   
     @Test
     void putInRotation() throws KillBillClientException {
       final AdminApi adminApi = new AdminApi(new KillBillHttpClient());
       adminApi.putInRotation(RequestOptions.empty());
     }
   }
   ```
5. If you open Eureka Console, you'll see that one of Kill Bill instance status is `DOWN`.