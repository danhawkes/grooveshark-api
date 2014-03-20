# Thresher
An unofficial API for Grooveshark, in Java.

[![Build Status](http://arcs.co/jenkins/buildStatus/icon?job=thresher)](http://arcs.co/jenkins/job/thresher/)

## Installation

```xml
<repositories>
	<repository>
		<id>co.arcs</id>
		<url>http://arcs.co/archiva/repository/internal</url>
	</repository>
</repositories>

<dependency>
	<groupId>co.arcs.groove</groupId>
	<artifactId>thresher</artifactId>
	<version>1.1.1</version>
</dependency>
```

Alternatively, download the latest Jar [here](https://arcs.co/archiva/browse/co.arcs.groove/thresher).

## Usage

First off, you'll need a client:

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

```java
user = client.login("username", "hunter2");
```

### Library / Favorites

List contents: 

```java
user.library.get();
user.favorites.get();
```

Add/Remove songs:

```java
user.library.add(song);
user.library.remove(song);
user.favorites.add(song);
user.favorites.remove(song);
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
