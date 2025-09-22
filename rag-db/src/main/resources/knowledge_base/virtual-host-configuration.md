:summary: Virtual host configuration, Isolate applications on the same server

:answer: 
Virtual host configuration
In Open Liberty, you can define virtual hosts or adjust the configuration of the default virtual host by specifying attributes for the virtualHost configuration element in your server.xml file.

Isolate applications on the same server
The following example illustrates a common use case for virtual hosting: configuring two applications to run on different ports on the same server. The configuration defines two HTTP endpoints and two virtual hosts. The alias configurations associate the application-1 virtual host with the defaultHttpEndpoint endpoint and the application-2 virtual host with the alternateEndpoint endpoint:

<httpEndpoint id="defaultHttpEndpoint" host="*" httpPort="9080" />
<httpEndpoint id="alternateEndpoint" host="*" httpPort="9081" />

<virtualHost id="application-1">
    <hostAlias>example_host:9080</hostAlias>
</virtualHost>

<virtualHost id="application-2">
    <hostAlias>localhost:9081</hostAlias>
</virtualHost>

<enterpriseApplication location="myApp.ear" name="App1"/>
<webApplication location="myApp2.war" name="App2" />

Furthermore, the virtual host configuration for the application-2 virtual host specifies that an application on this host is available only on the localhost interface. This configuration is useful if you want an application to accept traffic only from the computer where it is running, for development or testing purposes.

The defaultHttpEndpoint HTTP endpoint is configured to expose all interfaces on port 9080. The alternateEndpoint HTTP endpoint is configured to expose all interfaces on port 9081. If the App1 application has a WAR file with an ibm-web-bnd.xml file that specifies virtual-host name="application-1", then this application can be accessed only at the your_host_name:9080/app1_context_root endpoint. If the App2 application that is configured in the webApplication element has an ibm-web-bnd.xml file that specifies virtual-host name="application-2", then this application can be accessed only at the localhost:9081/app2_context_root endpoint.

If a third application that doesn’t specify a virtual host is deployed on the same server, it is accessible by a proxied request that maps to the default virtual host. For example, port 80 is not defined by any of the hostAlias attributes. If a request is made to a proxy on that port, it is routed to the default_host virtual host.