---
sidebar_position: 1
---

# Introduction

<img align="right" src="/img/logo.png" />

jUPnP is a Java UPnP library and has been forked from the no-longer maintained [Cling project](https://github.com/4thline/cling).

You can use jUPnP in your code by adding a dependency on it.
See the Maven, Gradle etc. dependency examples on [Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jupnp/org.jupnp).

## Build Instructions

Building and running the project is fairly easy if you follow the steps detailed below.

### Prerequisites

The build infrastructure is based on Maven and includes the Maven Wrapper,
so you do not need to install Maven separately.

What you need before you start:
- Java Development Kit (JDK) 11 or newer

You can usually install a JDK using your operating system's package manager, for example:
- Debian/Ubuntu: `openjdk-21-jdk`
- RHEL/Fedora: `java-21-openjdk-devel`

Package names may differ depending on the Java version and distribution.
If a suitable JDK is not available from your operating system, you can use
[Eclipse Temurin](https://adoptium.net/temurin/releases/), which is also used by the CI builds.

### Checkout

Checkout the source code from [GitHub](https://github.com/jupnp/jupnp), e.g. by running:

```shell
git clone https://github.com/jupnp/jupnp.git
```

### Building with Maven

To build jUPnP from the sources, Maven takes care of everything:
- change into the jupnp directory (`cd jupnp`)
- run `./mvnw clean install` to compile and package all sources

On Windows, use `.\mvnw.cmd` instead of `./mvnw`.

The Maven Wrapper downloads the required Maven version automatically.
The build result will be available in the folder `target`.

To improve build times you can add the following options to the command:

| Option                        | Description                                         |
| ----------------------------- | --------------------------------------------------- |
| `-DskipTests`                 | Skip the execution of tests                         |
| `-Dmaven.test.skip=true`      | Skip the compilation and execution of tests         |
| `-Dmaven.javadoc.skip=true`   | Skip the creation of Javadoc JARs                   |
| `-Dmaven.source.skip=true`    | Skip the creation of source code JARs               |
| `-Dlicense.skip=true`         | Skip the license header checks                      |
| `-Dsort.skip=true`            | Skip the POM sort order checks                      |
| `-Dspotless.check.skip=true`  | Skip the Spotless code style checks                 |
| `-T 1C`                       | Build in parallel, using 1 thread per core          |

For example you can skip tests and the Spotless checks during development with:

```shell
./mvnw clean install -DskipTests -Dspotless.check.skip=true
```

Adding these options improves the build time but could hide problems in your code.
Parallel builds are also less easy to debug and the increased load may cause timing sensitive tests to fail.

### Code style

The code style used by jUPnP is available as an Eclipse XML configuration in [codestyle.xml](https://github.com/jupnp/jupnp/blob/main/tools/spotless/codestyle.xml).
To use this configuration while coding, import the code style configuration into an IDE such as Eclipse or IntelliJ.

To check if your code is following the code style run:

```shell
./mvnw spotless:check
```

To reformat your code so it conforms to the code style you can run:

```shell
./mvnw spotless:apply
```

### Integration tests

The OSGi integration tests in the "itests" directory use specific versions of bundles in the runbundles of `itest.bndrun`.
You may need to update these runbundles after creating a new jupnp release or when changing dependencies.
Maven can resolve the runbundles automatically by executing:

```shell
./mvnw clean install -DwithResolver
```

### Working with Eclipse

When using Eclipse ensure that the JDK is set via the `-vm` option in `eclipse.ini`.
Otherwise m2e might fail to resolve the system scoped dependency to `tools.jar`.
