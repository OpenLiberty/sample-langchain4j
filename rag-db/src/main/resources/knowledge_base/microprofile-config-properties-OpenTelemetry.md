:summary: MicroProfile Telemetry: OpenTelemetry properties
:answer: MicroProfile Telemetry: OpenTelemetry properties

The following OpenTelemetry properties can be specified when MicroProfile Telemetry is enabled. This table lists the most important properties. For the full list, see the link:https://opentelemetry.io/docs/languages/java/configuration/#environment-variables-and-system-properties[OpenTelemtry Java configuration properties].

You can enable OpenTelemetry at the runtime level to gather telemetry data from both the runtime and application. When you choose this option, you specify OpenTelemetry properties in runtime-level configuration sources instead of in application-level `microprofile-config.properties` files. In most cases, runtime-level configuration is preferred because it includes both runtime-level telemetry and application-specific telemetry. Alternatively, if your runtime hosts more tha one application, you can enable OpenTelemetry and and configure OpenTelemetry properties at the application level, for example, in microprofile-config.properties` files for each application. However, this option does not enable runtime telemetry. For more information, see xref:ROOT:microprofile-telemetry.adoc[OpenTelemetry].


The runtime reads properties in the following table at either application startup or runtime startup, depending on when the OpenTelemetry instance initializes. In cases where each application is configured to use a separate OpenTelemetry instance, the runtime reads the properties at application startup. In cases where all applications on the runtime share a single OpenTelemetry instance, the runtime reads the properties at runtime startup.

.OpenTelemetry properties enabled by MicroProfile Telemetry
[options="header"]
|===
|Name |Description |When the runtime reads the property |Example

|`otel.blrp.export.timeout`
|Sets the delay interval in milliseconds between two consecutive exports from the link:https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#batch-logrecord-processor[Batch LogRecord Processor]. The default is `30000`.
|Application startup or runtime startup
|`otel.blrp.export.timeout=50000`

|`otel.blrp.schedule.delay`
|Sets the maximum allowed time in milliseconds to export data from the Batch LogRecord Processor. The default is `1000`.
|Application startup or runtime startup
|`otel.blrp.schedule.delay=5000`

|`otel.blrp.max.queue.size`
|Sets the maximum queue size for the Batch LogRecord Processor. The default is `2048`.
|Application startup or runtime startup
|`otel.blrp.max.queue.size=5000`

|`otel.blrp.max.export.batch.size`
|Sets the maximum batch size for the Batch LogRecord Processor. The default is `512`. The value must be Must be less than or equal to the value for `otel.blrp.max.queue.size`.
|Application startup or runtime startup
|`otel.blrp.max.export.batch.size=1024`

|`otel.exporter.jaeger.endpoint`
|Sets the endpoint for the Jaeger exporter. The default is \http://localhost:14250.
|Application startup or runtime startup
|`otel.exporter.jaeger.endpoint=http://localhost:14251`

|`otel.exporter.jaeger.timeout`
|Sets the maximum time, in milliseconds, that the Jaeger exporter waits for each batch export. The default is `10000`.
|Application startup or runtime startup
|`otel.exporter.jaeger.timeout=20000`

|`otel.exporter.otlp.endpoint`
|Sets the endpoint for the OpenTelemetry Protocol (otlp) exporter. The default is \http://localhost:4317
|Application startup or runtime startup
|`otel.exporter.otlp.endpoint=http://localhost:4319`

|`otel.exporter.zipkin.endpoint`
|Sets the endpoint for the Zipkin exporter. The default is \http://localhost:9411/api/v2/spans.
|Application startup or runtime startup
|`otel.exporter.zipkin.endpoint=http://localhost:9413/api/v2/spans`

|`otel.logs.exporter`
|You can use this property to change where the logs that  OpenTelemetry collects are exported.
|Application startup or runtime startup
|`otel.traces.exporter=console`

|`otel.metric.export.interval`
|By default, metric data is exported at an interval of 60000 milliseconds (one minute). To modify the export interval, specify the new value in milliseconds. For more information, see link:https://opentelemetry.io/docs/specs/otel/metrics/sdk/#periodic-exporting-metricreader[Periodic exporting MetricReader] in the OpenTelemetry documentation.
|Application startup or runtime startup
|`otel.metric.export.interval=120000`

|`otel.metric.export.timeout`
|By default, the maximum allowed time to export data is 30000 milliseconds (30 seconds). To modify the timeout value, specify a new value in milliseconds
|Application startup or runtime startup
|`otel.metric.export.timeout=60000`

|`otel.metrics.exporter`
|You can use this property to change where the metrics that OpenTelemetry collects are exported.
|Application startup or runtime startup
|`otel.metrics.exporter=prometheus`

|`otel.sdk.disabled`
|Enables logs, metrics, and traces to be sent to OpenTelemetry. The default is `true`.
|Application startup or runtime startup
|`otel.sdk.disabled=false`

|`otel.service.name`
|Sets of the name of the service that  OpenTelemetry is tracing.
|Application startup or runtime startup
|`otel.service.name=system`

|`otel.traces.exporter`
|Sets the exporter that is used to collect traces. Possible values are `otlp`, `zipkin`, `jaeger`, or `logging`. The default value is `otlp`. For the Jaeger trace service versions 1.35 and later, the `otlp` exporter value is recommended, rather than `jaeger`.
|Application startup or runtime startup
|`otel.traces.exporter=zipkin`

|===