mapstories
==========

**MapStories** is an application for viewing and sharing information with geographical location. It allows users to add pins to the map that represent personal memories, reported incidents, community events, lost/found items, and much more. MapStories uses Gemini to find corresponding 'Lost' and 'Found' pins. You may use a Gemini API, alternatively, the system will use regular expressions to establish matches. Please note that the alternative matching method has worse accuracy. 

Requisites 
-----
- `JDK 21+` and `Maven 3.6.0+`
- Source code
- Gemini API key

Installation
-----
1. Extract the Source code
2. Navigate to `src/main/resources` and create `gemini-secrets.properties` file, put `api_key=[put Gemini API Key here]`.
3. To compile the server, use:
```bash
mvn compile
```
Note: there's no need to setup the database.

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
