mapstories
==========

**MapStories** is an application for viewing and sharing information with geographical location. It allows users to add pins to the map that represent personal memories, reported incidents, community events, lost/found items, and much more.

Installation
-----

So far, the application is accessible to those that have `JDK 21+`(haven't tried less) and `Maven 3.6.0+` (reasonable) installed on their system.

To compile the server, use:
```bash
mvn compile
```

Usage
-----

_developers' version_

- To conduct the `surefire` tests, use:

  ```bash
  mvn test
  ```

- To run the application, use the following snippet. It will launch the server on `localhost`, port `7070`.
  ```bash
  mvn compile exec:java
  ```

- To generate an overview of the coding standards and development practices that we have violated, as well as to print some summaries, execute:
  ```bash
  mvn compile site
  ```
