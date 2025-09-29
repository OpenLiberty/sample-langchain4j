Tag: Jakarta EE
Content: 

Jakarta EE is a Java platform that helps build enterprise applications; it specifies a runtime environemnt and various application programming interfaces (APIs). 

An extension to Java SE (which provides the Java Runtime Environment) is Jakarta EE. 

Core Profile

The Core Profile is added in Jakarta EE 10 and includes the Jakarta EE features that are needed for modern cloud native applications. This subset of function includes all Jakarta EE functions that are required for MicroProfile-based applications.
    ex. Jakarta JSON Binding allows for binding the data and runtime between JSON documents and Java classes.
    Package name: 
    `jakarta.json.bind` - main classes or interfaces are listed.
    For example: Main classes in `jakarta.json.bind` include `Jsonb` (serialize and deserialize JSON/Java Objects), `JsonBuilder` (clients use this to create instances of Jsonb), `JsonbConfig`(configration to set custom  serializers and deserializers), and `JsonbException`.

Web Profile
The Web Profile defines a reasonably complete stack that targets modern web applications. This stack is a subset of the platform standard APIs that can address the needs of most web applications.

Platform
The platform defines the full complement of the Jakarta EE programming model. In addition to the Web Profile features, the platform has specifications for advanced business capabilities that an enterprise needs, such as for connectivity, enterprise beans, messaging, and application clients.

Source: https://jakarta.ee/learn/docs/jakartaee-tutorial/current/web/jsonb/jsonb.html 

