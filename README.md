Berengar
========

### What is Berengar ?

Berengar is an extremely rough Android app designed to develop and test database helper functions for import/export from/to json/xml text files.


### What Berengar can do ?

* There is a rough user interface to insert data into two database tables
* DbTool.db2gson() exports full database to json file (no String limitation)
* DbTool.gson2db() imports full database from json file (no String limitation)
* DbTool.db2xml() exports full database to xml file (no String limitation)
* DbTool.xml2db() imports full database from xml string (no String limitation)
* DbTool.db2json() exports full database to json string (use JSONArray/JSONObject)
* DbTool.json2db() imports full database from json string (use JSONArray/JSONObject)

### How can I use it?

```java
// XML FORMAT

// to create database backup in xml format
File xmlFile = new File(exportDir,"myfilename.xml");
iRes = DbTool.db2xml(dbHelper.getHandleDB(),xmlFile);

// to restore database from xml
File xmlFile = new File(exportDir,"myfilename.xml");
iRes = DbTool.xml2db(dbHelper.getHandleDB(),xmlFile);

// JSON FORMAT

// to create database backup in json using GSON library and streaming generator
File jsonFile = new File(exportDir,"myfilename.json");
iRes = DbTool.db2gson(dbHelper.getHandleDB(),jsonFile);

// to restore database from json using GSON library and streaming parser
File jsonFile = new File(exportDir,"myfilename.json");
iRes = DbTool.gson2db(dbHelper.getHandleDB(),jsonFile); 

// DEPRECATED

// to create database backup in json format and minify it
String json_backup = DbTool.db2json(dbHelper.getHandleDB(),"optional_db_name").toString(3);
json_backup = DbTool.jsonMinify(json_backup);

// to restore database from json using DOM-like access
JSONObject jsonDB = new JSONObject(json_backup);
iRes = DbTool.json2db(dbHelper.getHandleDB(),jsonDB);
```


### Limitations

* Tested only with database with 2 tables and about 10 entries
* Many error checks are missing


### What's next

* Test with bigger databases 
* Add XML minify function


### Why the name Berengar ?

Berengar of Arundel is the assistant librarian in "The name of the rose" novel by Umberto Eco.
