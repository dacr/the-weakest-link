# scala project skeleton

Everything needed to generate a single standalone executable jar, using scala language.

## Features

* [sbt](http://www.scala-sbt.org/) build system
* source code generation at build time to inject project meta data
* [logback](https://logback.qos.ch/) integration with configuration for both run and tests


## To use eclipse with this project

Edit (or create) the file ~/.sbt/0.13/plugins.sbt and add the following line to enable eclipse plugin :
```
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")
```

## Typical usages

```
$ sbt
> eclipse
> exit

$ sbt run

$ sbt test

$ sbt assembly

$ java -jar target/dummy.jar

```

