[![Build Status](https://travis-ci.org/a-langer/jsr223-commonjs-modules.svg?branch=master)](https://travis-ci.org/a-langer/jsr223-commonjs-modules)
[![license](http://img.shields.io/badge/license-MIT-brightgreen.svg)](https://github.com/a-langer/jsr223-commonjs-modules/blob/master/LICENSE)
[![Maven JitPack](https://img.shields.io/github/tag/a-langer/jsr223-commonjs-modules.svg?label=maven)](https://jitpack.io/#a-langer/jsr223-commonjs-modules)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.a-langer/jsr223-commonjs-modules/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.a-langer/jsr223-commonjs-modules)

# CommonJS Modules Support for Nashorn, Rhino and Graal.js

This library adds support for CommonJS modules (aka `require`) inside a Nashorn, Rhino and Graal.js script engines. It is based on the specification for [NodeJS modules](https://nodejs.org/api/modules.html) and it supports loading modules from the `node_modules` folder just as Node does. Of course, it doesn't provide an implementation for Node's APIs, so any module that depends on those won't work.

This project is a fork of [nashorn-commonjs-modules](https://github.com/malaporte/nashorn-commonjs-modules).

# Supported features:

* Ready for use in scripting engines Nashorn, Rhino and Graal.js.
* Displays the file name and line number in the error stacktrace.
* Compatible with JSR-223 standard.
* Implementation on pure Java.
* No dependency on third-party libraries.

# Getting the library using Maven

Add this dependency to your `pom.xml` to reference the library:

```xml
<dependency>
    <groupId>com.github.a-langer</groupId>
    <artifactId>jsr223-commonjs-modules</artifactId>
    <version>1.0.1</version>
</dependency>
```

# Usage

Enabling `require` in Nashorn script engine:

```xml
<!-- Dependency need only for JDK 15 and later -->
<dependency>
    <groupId>org.openjdk.nashorn</groupId>
    <artifactId>nashorn-core</artifactId>
    <version>15.3</version>
</dependency>
```

```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
Require.enable(engine, myRootFolder);
```

Enabling `require` in Rhino script engine:

```xml
<dependency>
    <groupId>cat.inspiracio</groupId>
    <artifactId>rhino-js-engine</artifactId>
    <version>1.7.10</version>
</dependency>
<dependency>
    <groupId>org.mozilla</groupId>
    <artifactId>rhino</artifactId>
    <version>1.7.14</version>
</dependency>
```

```java
ScriptEngine engine = new ScriptEngineManager().getEngineByName("rhino");
Require.enable(engine, myRootFolder);
```

Enabling `require` in Graal.js script engine:

```xml
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js-scriptengine</artifactId>
    <version>22.0.0.2</version>
</dependency>
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js</artifactId>
    <version>22.0.0.2</version>
</dependency>
```

```java
System.setProperty("jvm.Dtruffle.js.NashornJavaInterop", "true");
System.setProperty("polyglot.js.nashorn-compat", "true");
ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
Require.enable(engine, myRootFolder);
```

This will expose a new global `require` function at the engine scope. Any code that is then run using this engine can make use of `require`.

The second argument specifies the root `Folder` from which modules are made available. `Folder` is an interface exposing a few calls that need to be implemented by backing providers to enable loading files and accessing subfolders. Out-of-the-box, the library supports loading modules from the filesystem and from Java resources.

## Loading modules from the filesystem

Use the `FilesystemFolder.create` method to create an implementation of `Folder` rooted at a particular location in the filesystem:

```java
FilesystemFolder rootFolder = FilesystemFolder.create(new File("/path/to/my/folder"), "UTF-8");
Require.enable(engine, rootFolder);
```

You need to specify the encoding of the files. Most of the time UTF-8 will be a reasonable choice.

The resulting folder is rooted at the path you specified, and JavaScript code won't be able to "escape" that root by using `../../..`. In other words, it behaves as is the root folder was the root of the filesystem.


## Loading modules from Java resources

Use the `ResourceFolder.create` method to create an implementation of `Folder` backed by Java resources:

```java
ResourceFolder rootFolder = ResourceFolder.create(getClass().getClassLoader(), "com/github/alanger/commonjs_modules/test1", "UTF-8");
Require.enable(engine, rootFolder);
```

As for `ResourceFolder`, you need to specify the encoding for the files that are read.

## Related repositories
* [nashorn-commonjs-modules](https://github.com/malaporte/nashorn-commonjs-modules) - CommonJS for Nashorn.
* [graal-commonjs-modules](https://github.com/transposit/graal-commonjs-modules) - CommonJS for Graal.js.
* [jvm-npm](https://github.com/nodyn/jvm-npm) - CommonJS written on javascript for DynJS, Nashorn and Rhino.

