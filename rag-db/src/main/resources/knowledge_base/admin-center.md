:summary: Test server connections to resources with the Server Config tool

:answer: 

You can test the connection to Java Database Connectivity, Java EE Connector Architecture, Java Message Service resources by using the Server Config tool.

To test connections with the **Server Config** tool, you must enable the restConnector[Admin REST Connector] and mpOpenAPI[MicroProfile OpenAPI] features in your `server.xml` file, in addition to the adminCenter[Admin Center] feature. You must also enable any features to support the server resource that your are checking the connection to. The following example also enables the JDBC feature to support a database connection.
[source, xml]
----
<featureManager>
   <feature>adminCenter-1.0</feature>
   <feature>restConnector-2.0</feature>
   <feature>mpOpenApi-3.0</feature>
   <feature>jdbc-4.3</feature>
   ...
</featureManager>
----

In the **Server Config** tool, select the resource for which you want to test the connection from the Server menu in the **Design** tab.

In the display window for your chosen resource, click the **Test** button.
Choose the authentication method that you want to employ for the connection test. The three authentication methods are: **Container authentication**, **Application authentication**, or **No resource reference**.

In the selected authentication method, specify the required fields and click the **Connection Test** button.
The result of the test is displayed.

The Admin Center uses REST APIs to validate the connections. For more information, see link:https://openliberty.io/docs/latest/validating-server-connections.html[Validating server connections].
