# Thresher
An unofficial API for Grooveshark, in Java.

[![Build Status](https://arcs.co/jenkins/buildStatus/icon?job=thresher)](https://arcs.co/jenkins/job/thresher/)

## Download

With Maven:

```xml
<dependency>
	<groupId>co.arcs.groove</groupId>
	<artifactId>thresher</artifactId>
	<version>X.X.X</version>
</dependency>
```

Or Gradle:

```groovy
compile 'co.arcs.groove:thresher:X.X.X'
```

Find the latest version on [maven central](http://search.maven.org/#search|ga|1|g%3A%22co.arcs.groove%22%20AND%20a%3A%22thresher%22).

## Usage

All API requests go through a client object:

```java
client = new Client();
```

### Search

By keyword: 

```java
client.search("clair de lune");
```

For popular songs:

```java
client.searchPopularSongs();
```

### Stream

Get a short-lived song URL:

```java
client.getStreamUrl(Song);
```


### Log in

Log in to get access a user's data:

```java
user = client.login("username", "hunter2");
```

List the contents of their library and favorites: 

```java
user.library().get();
user.favorites().get();
```

Add or remove songs:

```java
user.library().add(song);
user.library().remove(song);
user.favorites().add(song);
user.favorites().remove(song);
```

## Build / Test

To build and run tests against the real servers:

```shell
mvn clean test
```

## Notes

Bug reports and pull requests are much appreciated.  
Thanks to [sosedoff](https://github.com/sosedoff) and contributors for providing API information in their [Ruby library](https://github.com/sosedoff/grooveshark).

## Licence

Apache 2.0. See `LICENCE.txt` for details.
