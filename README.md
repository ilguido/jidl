# jidl
Java Industrial Data Logger based on [Apache PLC4J](https://plc4x.apache.org/).

## Notice
JIDL is alpha software and insecure in a production environment.

## Dependencies
- Java 11+
- PLC4J 0.10 and its dependencies
  - bcprov
  - bit-io
  - commons-codec
  - commons-lang3
  - jackson-annotations
  - json-simple
  - netty-buffer
  - netty-codec
  - netty-common
  - netty-handler
  - netty-resolver
  - netty-transport
  - plc4j-api
  - plc4j-driver-modbus
  - plc4j-driver-opcua
  - plc4j-driver-s7
  - plc4j-spi
  - plc4j-transport-tcp
  - slf4j-api
  - slf4j-simple
  - vavr
- MariaDB JDBC connector (optional)
- MonetDB JDBC connector (optional)
- SQLite JDBC connector (optional)

## Compiling
Copy all the dependencies into the `libs` directory.
Edit the `build.sh` script to suit your needs; in particular, set the desired Java compiler, or leave the default `javac`.
E.g.:
```
JAVAC=/path/to/custom/jdk/bin/javac
```
Run the `build.sh` script with the desired parameter. 
E.g. `sh build.sh all` to compile everything, including the Javadoc documentation, or `sh build.sh jar` to build a jar file from the compiled classes (you have to compile jidl, before building the jar file).

## Use
JIDL can read and write tags (i.e. variables) on PLC devices and then log the read tags in a database.
JIDL, until stopped, endlessly reads a number of tags from some remote device, copies those tags to a database, writes other tags to the remote devices, repeats.

### Configuration file
JIDL reads its configuraton from an INI file. The configuration file can be loaded from the graphical interface or passed to JIDL with the command line `-c` switch.
The configuration file includes one section with the configuration of the database, e.g.:
```
[datalogger]
type=sqlite
name=mydb
dir=/var/db/
```
Given that configuration, at startup, JIDL opens the SQLite database `/var/db/mydb.db`.
#### Contents of the configuration file
There is only one section `datalogger`.
The parameters then depends on the type of "data logger".
- *Dummy*: prints all the data to the standard output, for testing purposes
```
[datalogger]
type=dummy
file=file_name_without_extension
dir=/path/to/file/
```
<details>JIDL loads the list of tags to read from /path/to/file/file_name_without_extension.ini. It prints the data to the standard output.</details>

- *SQLite*: store all the data in a SQLite DB file
```
[datalogger]
type=sqlite
file=file_name_without_extension
dir=/path/to/DB/
```
 <details>JIDL loads the list of tags to read from /path/to/DB/file_name_without_extension.db. It logs the data to /path/to/DB/file_name_without_extension.db.</details>
  
- *Maria DB*: store all the data in a Maria DB data base
```
[datalogger]
type=mariadb
name=db_name
dir=/tmp
server=192.168.100.10
port=3306
username=user
password=password
```
 <details>JIDL loads the list of tags to read from data base db_name at the server listening from 192:168:100:10:3306. It logs the data to the data base server.</details>

- *Monet DB*: store all the data in a Monet DB data base
```
[datalogger]
type=monetdb
name=db_name
dir=/tmp
server=192.168.10.100
port=5000
username=user
password=password
```
 <details>JIDL loads the list of tags to read from data base db_name at the server listening from 192:168:10:100:5000. It logs the data to the data base server.</details>

### Structure of the data base
The database must be structured as following.
- one *JIDL Diagnostics* table, where JIDL logs its actions, errors etc.
- one *JIDL Configuration* table, where JIDL can find all the information about the tags to read and write.
- a table for each connection to a PLC, where JIDL logs the tag values.
#### Contents of the *JIDL Diagnostics* table
Two columns: `TIMESTAMP` as `TEXT`, and `MESSAGE` as `TEXT`.
#### Contents of the *JIDL Configuration* table
Two columns: `ID` as `TEXT`, and `DATA` as `TEXT`.
The `DATA` column contains an INI section for each row. These INI sections describe either: a connection to a PLC, a tag to be read from a PLC, a tag to be written to a PLC.
The `ID` cell determines the type of section.
- *connection* row
  - the `ID` must be alphanumeric, e.g. `myconnection`
  - the `DATA` contains an INI section with the configuration of the connection
- *tag reader* row
  - the `ID` must be the alphanumeric name of the tag, then two colons, then the alphanumeric name of the connection, e.g. `mytag::myconnection`
  - the `DATA` contains an INI section with the address and data type of the tag
- *tag writer* row
  - the `ID` must be the alphanumeric name of the tag, then two colons, then the alphanumeric name of the connection, then "lesser than", than "minus", then a tag reader full name, e.g. `myothertag::myotherconnection<-mytag::myconnection`
  - the `DATA` contains an INI section with the address of the tag
  - JIDL writes into this tag the value of the specified tag reader, e.g. the value of mytag::myconnection is written to myothertag::myotherconnection
Example:
```
| ID      | DATA               |
|---------|--------------------|
|conn     |[conn]              |
|         |type=S7             |
|         |address=192.168.1.1 |
|         |rack=0              |
|         |slot=2              |
|         |seconds=2           |
|---------|--------------------|
|tag::conn|[tag::conn]         |
|         |address=DB1.DBW2    |
|         |type=INTEGER        |
|         |                    |
```
<details>

  <summary>Supported connections</summary>

The sample rate of data reads from each connection is configured with the parameter: seconds.
#### Modbus TCP
Type: modbus.
with parameters: address (IP address), port (positive integer number), reversed (boolean), seconds (positive integer number).
#### OPCUA
Type: opcua.
with parameters: address (IP address), port (positive integer number), path (text), discovery (boolean), username (text), password (text), seconds (positive integer number).
#### S7
Type: S7.
with parameters: address (IP address), rack (positive integer number or zero), slot (positive integer number or zero), seconds (positive integer number).
#### JSON
Type: json.
with parameters: address (full URL to the JSON resource), seconds (positive integer number).

</details>

<details>

  <summary>Tag configuration</summary>

#### Tag reader
Address: the address according to PLC4J usage, or just the name of the variable for JSON variables.
Type: BOOLEAN, INTEGER, DOUBLE_INTEGER, FLOAT, REAL, BYTE, WORD, TEXT.
#### Tag writer
Address: the address according to PLC4J usage.
The type of the tag is the same of the source tag reader.

</details>

#### Contents of the connection tables
There is one table for each connection. The table name is the same as the connection name. Each connection table comprises one `TIMESTAMP` as `TEXT` column and as many other columns as the number of tags configured for that connection, each of a suitable data type.
Example, following the preceding example:
```
| TIMESTAMP (TEXT)     | tag (INTEGER)     |
|----------------------|-------------------|
|2024-07-15 18:17:16,0 | 132               |
|2024-07-15 18:17:18,0 | 135               |
|2024-07-15 18:17:20,0 | 137               |
```
#### In case of using the dummy data logger
Since the dummy data logger does not use a data base, it reads its "JIDL Configuration" from an INI file. The contents of this INI file are the same as the `DATA` column of the *JIDL Configuration* table.
