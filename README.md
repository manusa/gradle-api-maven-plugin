# Gradle API Maven Plugin

## Developing

### Integration test

There are integration tests to verify that the plugin works as expected.
The tests rely on the Maven Invoker Plugin to run a Maven build with the plugin applied.

You can use the tests to debug the project in your IDE.
Start the project compilation with the following command:
```shell
mvn -Pit-debug verify
```
Then, configure your IDE to perform a remote debug connection to port 8000.
