# Gradle API Maven Plugin

Maven plugin that registers a Maven extension that will automatically add all Gradle dependencies for a given version to your Project.

This is especially useful since Gradle no longer publishes its artifacts to Maven Central.

## Usage

Add a _fake_ dependency to your project with the version of Gradle you want to use.

```xml
<dependencies>
  <dependency>
    <groupId>org.gradle</groupId>
    <artifactId>gradle-all</artifactId>
    <version>${version.gradle}</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Add the plugin with the `extensions` option enabled.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.marcnuri.plugins</groupId>
      <artifactId>gradle-api-maven-plugin</artifactId>
      <version>0.1.0</version>
      <extensions>true</extensions>
    </plugin>
  </plugins>
</build>
```

> [!IMPORTANT]
> Please make sure to provide the `<extensions>true</extensions>` option for the plugin.

> [!NOTE]
> It's likely that you'll also need to add a dependency for `org.codehaus.groovy:groovy-all` to your project.

With these settings, you should now be able to import and make use of the Gradle API types.

## Developing

### Integration test

There are integration tests to verify that the plugin works as expected.
The tests rely on the Maven Invoker Plugin to run a Maven build with the plugin applied.

You can use the tests to debug the project in your IDE.
Start the project compilation with the following command:
```shell
mvn -Pit-debug verify
```
Then, configure your IDE to perform a remote debug connection to port `8000`.
