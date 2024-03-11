# KPM Configuration

```properties
# This is Kill Bill configuration. Put in here because KPM plugins will depend a lot on this value to put plugins files.
org.killbill.osgi.bundle.install.dir=/var/tmp/bundles


# Configuration for killbillApi usage
org.killbill.billing.plugin.kpm.adminUsername=admin
org.killbill.billing.plugin.kpm.adminPassword=password


# Configuration for KPMClient (Kill Bill's plugin HttpClient).
org.killbill.billing.plugin.kpm.strictSSL=false
org.killbill.billing.plugin.kpm.readTimeoutSec=60
org.killbill.billing.plugin.kpm.connectTimeoutSec=60


# "org.killbill.billing.plugin.kpm.nexus<XXX>" properties exists because:
# 1. Backward compatibility with older configuration
# 2. This is a "fallback" for pluginsInstall.coordinate configuration if "url/authMethod/authUsername/authPassword/authToken" not set.
# 3. This is a required configuration to get Kill Bill version (See more AvailablePluginsComponentsFactory.createVersionsProvider() )
# In codebase, if *.kpm.nexusUrl value not set, or contains "oss.sonatype.org", the final construct of URL would be 
# ${*.kpm.nexusUrl} + "/content/repositories" + ${*.kpm.nexusRepository}. 
org.killbill.billing.plugin.kpm.nexusUrl=https://oss.sonatype.org
org.killbill.billing.plugin.kpm.nexusRepository=/releases

# How Authentication header will construct. If none, then KPMPlugin will not send "Authorization" header when download 
# any files. If BASIC, then *.kpm.nexusAuthToken value will be ignored and will use username/password values). 
# If TOKEN, then *.kpm.nexusAuthUsername and *.kpm.nexusAuthPassword will be ignored (will use *.kpm.nexusAuthToken value).
org.killbill.billing.plugin.kpm.nexusAuthMethod=NONE|BASIC|TOKEN
org.killbill.billing.plugin.kpm.nexusAuthUsername=VALID_USERNAME
org.killbill.billing.plugin.kpm.nexusAuthPassword=VALID_PASSWORD
org.killbill.billing.plugin.kpm.nexusAuthToken=VALID_TOKEN

# Warning: 'nexusMavenMetadataUrl' share authentication configuration with ".nexus<XXX>". This is because 
# 'nexusMavenMetadataUrl' is always part of "get Kill Bill version" logic (See class named 'DefaultNexusMetadataFiles').
# Also, usually maven-metadata.xml will host in the same url as Kill Bill's pom.xml
org.killbill.billing.plugin.kpm.nexusMavenMetadataUrl=https://repo1.maven.org/maven2/org/kill-bill/billing/killbill/maven-metadata.xml


# Cache configuration for in 'AvailablePluginsComponentsFactory.java'. Since
# 'AvailablePluginsComponentsFactory.java' used to construct 'availablePlugins'
# component, the configuration property set under 'kpm.availablePlugins.cache'.
# Why not under 'kpm.cache'? To allow other area of code base to added another
# cache configuration in the future, if needed.
# 
# FIXME-TS-93: We need to add note to documentation and say clearly that:
# 1. By default, KAUI always send forceDownload=false.
# 2. Setting `availablePlugins.cache.enabled=true` means `AvailablePluginsComponentsFactory.java` will not download 
#    anything when install plugin via coordinate.
# 3. `availablePlugins.cache.enabled` will take precedence over `forceDownload`. Setting `cache.enabled=false` mean KPM 
#    plugin will always download plugins and plugins information.
org.killbill.billing.plugin.kpm.availablePlugins.cache.enabled=false
org.killbill.billing.plugin.kpm.availablePlugins.cache.expirationSecs=86460
org.killbill.billing.plugin.kpm.availablePlugins.cache.size=10


# Configuration to get plugins directory. Current implementation only support file based 'plugins_directory.yml', with 
# YAML file structure looks like this:
# https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml
# Currently, we have 2 usage of 'pluginsDirectory':
# 1. Default implementation of PluginManager.getAvailablePlugins()
# 2. By 'ArtifactAndVersionFinder.java', where we need to know plugin's artifact/version based on plugin key.
# Those authMethod/authUsername/authPassword/authToken values is serve the same purpose as 
org.killbill.billing.plugin.kpm.pluginsDirectory.url=https://raw.githubusercontent.com/killbill/killbill-cloud/master/kpm/lib/kpm/plugins_directory.yml
org.killbill.billing.plugin.kpm.pluginsDirectory.authMethod=NONE|BASIC|TOKEN
org.killbill.billing.plugin.kpm.pluginsDirectory.authUsername=VALID_USERNAME
org.killbill.billing.plugin.kpm.pluginsDirectory.authPassword=VALID_PASSWORD
org.killbill.billing.plugin.kpm.pluginsDirectory.authToken=VALID_TOKEN


# Configuration for coordinate based plugin installation: This is where
# pluginManager.install(pluginKey, kbVersion, groupId, artifactId, pluginVersion, forceDownload)
# called.
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.verifySHA1=false
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.url=https://oss.sonatype.org/content/repositories/releases
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.authMethod=NONE|BASIC|TOKEN
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.authUsername=VALID_USERNAME|KPM_NEXUS_AUTH_USERNAME
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.authPassword=VALID_PASSWORD|KPM_NEXUS_AUTH_PASSWORD
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.authToken=VALID_TOKEN|KPM_NEXUS_AUTH_TOKEN

# If KPM plugin can not find any plugin in "*.pluginInstall.coordinate.url" and 
# "*.pluginInstall.coordinate.alwaysTryPublicRepository=true", KPM plugin will try to find plugin from public/official 
# killbill repository. 
org.killbill.billing.plugin.kpm.pluginInstall.coordinate.alwaysTryPublicRepository=false

```