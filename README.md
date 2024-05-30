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
      <version>0.0.6</version>
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

## Frequently Asked Questions (FAQ)

### How to use behind an HTTPS proxy with authentication?

You can configure the proxy by passing the following properties to the Maven build:

```shell
mvn -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080 -Dhttps.proxyUser=proxyuser -Dhttps.proxyPassword=proxypass
```

In addition, you can also leverage the `settings.xml` file to configure the proxy as you would do for Maven:

```xml
<settings>
  <proxies>
    <proxy>
      <id>example-proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>proxy.example.com</host>
      <port>8080</port>
      <username>proxyuser</username>
      <password>proxypass</password>
      <nonProxyHosts>localhost|*.example.com</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

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

### Release Process

#### Release to Maven Central

To release a new version automatically:

```shell
make release V=X.Y.Z VS=X.Y
```
- `V`: New version to release.
- `VS`: New SNAPSHOT version for Maven.

To release a new version manually:

1. Update the version in the `pom.xml` file.
   ```shell
   mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
   ```
2. Commit and tag the release with the  `pom.xml` version.
   ```shell
   git add .
   git commit -m "[RELEASE] vX.Y.Z released"
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```
3. Update the version in the `pom.xml` file to the next snapshot version.
   ```shell
   mvn versions:set -DnewVersion=X.Y-SNAPSHOT -DgenerateBackupPoms=false
   ```
4. Commit the changes with the following message:
   ```shell
   git add .
   git commit -m "[RELEASE] v0.0.6 released, prepare for next development iteration"
   git push origin master
   ```
