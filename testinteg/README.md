[![Maven Central](https://maven-badges.herokuapp.com/maven-central/foundation.icon/javaee-integration-test/badge.svg)](https://search.maven.org/search?q=g:foundation.icon%20a:javaee-integration-test)

# An Integration Testing Framework for Java Smart Contract

This subproject contains a Java library that can be used to perform the integration testing on your Java Smart Contract implementation.
It assumes there is a running HAVAH network (either local or remote) that can be connected for the testing,
and provides some utility classes and methods to interact with the HAVAH network using [ICON SDK for Java](https://github.com/icon-project/icon-sdk-java).

## Usage

You can include this package from [Maven Central](https://search.maven.org/search?q=g:foundation.icon%20a:javaee-integration-test)
by adding the following dependency in your `build.gradle`.

```groovy
testImplementation 'foundation.icon:javaee-integration-test:0.9.0'
```

For a more complete example, please visit [Java Smart Contract Examples](https://github.com/havah-project/smart-contract-examples).
