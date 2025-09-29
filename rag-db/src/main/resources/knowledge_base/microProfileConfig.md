Tag: MicroProfile Config
Content:
Several applications require configuration data from outside the application based on the running environment.

If the same property is defined in multiple locations (ConfigSources), a policy decides which value should be used.

`ConfigProvider#getConfig()` allows the access to
the application's current configuration.

The information collected from `org.eclipse.microprofile.config.spi.ConfigSource` composes a Config.

ConfigSources default (there are three by default):"

- `System.getProperties()`(ordinal=400)
- `System.getenv()`(ordinal=300)
- all `META-INF/microprofile-config.properties` files on the ClassPath. (ordinal=100) and inside each file, it is possible to separately configure it via the `config_ordinal` property.

A higher ordinal number is the one that takes precedence."

Source: https://microprofile.io/specifications/microprofile-config-2/