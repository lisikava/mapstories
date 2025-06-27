mapstories
==========

**MapStories** is an application for viewing and sharing information with geographical location. It allows users to add pins to the map that represent personal memories, reported incidents, community events, lost/found items, and much more. MapStories uses Gemini to match corresponding 'Lost' and 'Found' pins. You may use the Gemini API, or, alternatively, the system will use regular expressions to establish matches. Please note that the alternative matching method has worse accuracy. 

Requisites
-----
- `JDK 21+`
- `Maven 3.6.0+`
- `PostgreSQL 16+`
- email account for the server

Installation
-----
1. Extract the Source code
2. To configure the mail server, refer to `mailer.properties` in project resources. It is necessary to set up `mailer-secrets.properties` with email account credentials in form:
```properties
username=your@mail.com
password=your_password
```
3. To configure the LLM-powered matcher, refer to `gemini.properties` in project resources. If you have an API key, place it in `gemini-secrets.properties`:
```properties
api_key=your_api_key
```
API key is not necessary for the functioning of the application.
4. To configure the local database, refer to `dbconfig.properties` in project resources. Either create the database with the properties described there, or modify the file to match the database you wish to use. In psql, do ```create extension hstore;```. Then, import the `schema.sql` file (and, optionally, `sample.sql` if you wish to populate the database with sample pins.) into the database.  
5. Compile the server with:
```bash
mvn compile
```

Usage
-----

- To run the application, use the following snippet. It will compile and launch the server on `localhost`, port `7070`.
  ```bash
  mvn compile exec:java
  ```

- To run simple tests, use:

  ```bash
  mvn test
  ```
