Berengar
========

### What is Berengar ?

Berengar is an extremely rough Android app designed to develop and test database helper functions for import/export from/to json/xml text files.


### What Berengar can do ?

* There is a rough user interface to insert data into two database tables
* DbTool.db2json() exports full database to json string
* DbTool.json2db() imports full database from json string
* DbTool.gson2db() imports full database from json file (no String limitation)
* DbTool.db2xml() exports full database to xml string
* DbTool.xml2db() imports full database from xml string
* FileTool.stringWrite() writes string to text file in SD card, GZip compression optional
* FileTool.stringRead() reads string from text file in SD card, GZip compression optional


### How can I use it?

```java
// to create database backup in json format and minify it
String json_backup = DbTool.db2json(dbHelper.getHandleDB(),"optional_db_name").toString(3);
json_backup = DbTool.jsonMinify(json_backup);

// to create database backup in xml format
String xml_backup = DbTool.db2xml(dbHelper.getHandleDB(),"optional_db_name");

// to restore database from json using DOM-like access
JSONObject jsonDB = new JSONObject(json_backup);
iRes = DbTool.json2db(dbHelper.getHandleDB(),jsonDB);

// to restore database from json using GSON library and streaming parser
File jsonFile = new File(exportDir,"myfilename.json");
iRes = DbTool.gson2db(dbHelper.getHandleDB(),jsonFile); 

// to restore database from xml
iRes = DbTool.xml2db(dbHelper.getHandleDB(),xml_backup);
```


### Limitations

* Tested only with database with 2 tables and about 10 entries
* Many error checks are missing
* String limitations
* JSON parsing implemented with org.json.JSONObject
* XML parsing implemented with SAX


### What's next

* Add db2gson (to remove String limitation)
* Test with bigger databases 
* Add XML minify function


### Why the name Berengar ?

Berengar of Arundel is the assistant librarian in "The name of the rose" novel by Umberto Eco.
