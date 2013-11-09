# Grooveshark-api

An unofficial API for Grooveshark, in Java.

# Installation

### Maven

Add the repository to your POM:

```xml
<repositories>
	<repository>
		<id>arcs.co</id>
		<url>http://arcs.co/archiva/repository/internal</url>
	</repository>
</repositories>

<dependency>
	<groupId>co.arcs</groupId>
	<artifactId>grooveshark-api</artifactId>
	<version>0.4</version>
</dependency>
```

### Jar:

Download the jar directly: :[grooveshark-api-0.3.jar](https://arcs.co/archiva/repository/internal/co/arcs/grooveshark-api/0.3/grooveshark-api-0.3.jar)

# Usage

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
client.getUrl(Song);
```

### Login

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

# Build / Test

To build and run tests against the real servers:

```shell
mvn clean install
```

# Contribute

Bug reports and pull requests are much appreciated.

# Thanks

To [sosedoff](https://github.com/sosedoff) and contributors for providing API information in their [Ruby library](https://github.com/sosedoff/grooveshark).

# Licence

Apache 2.0. See `LICENCE.txt` for details.
