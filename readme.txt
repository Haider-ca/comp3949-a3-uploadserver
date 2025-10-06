Upload Server – In-Class Assignment III (COMP 3949)
Author: Haider Al-Sudani

Description
-----------
Java 11 server that:
1) GET / → returns HTML upload form
2) POST / (multipart/form-data: caption, date, fileName) → saves file as images/<caption>_<date>_<original>
3) Responds with an HTML alphabetical listing of the images folder
4) Native client (HttpOverTcp) that posts a multipart request

Prerequisites
-------------
- Java 11 (Temurin/OpenJDK)

Compile & Run in IntelliJ
-------------------------
- Project SDK: Java 11
- Run config 1: UploadServer
  Main class: comp3949.bcit.a3.UploadServer
  Working dir: $ProjectFileDir$
- Run → UploadServer → visit http://localhost:8082/ → upload a file
- Run config 2: HttpOverTcp
  Main class: comp3949.bcit.a3.HttpOverTcp
  Program args: localhost 8082 test.png
  Working dir: $ProjectFileDir$
- Run → HttpOverTcp → prints HTML listing from server

Files
-----
src/comp3949/bcit/a3/
  HttpServlet.java
  HttpServletRequest.java
  HttpServletResponse.java
  UploadServer.java
  UploadServerThread.java
  UploadServlet.java
  HttpOverTcp.java
images/ (runtime uploads; tracked via .gitkeep)

Notes
-----
- Binary-safe multipart parsing
- Multi-threaded server via UploadServerThread
