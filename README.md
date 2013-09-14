Berengar
========

### What is Berengar ?

Berengar is an extremely rough Android app designed to develop and test database helper functions for import/export from/to json/xml text files.


### What Berengar can do ?

* There is a rough user interface to insert data into two database tables
* DbTool.db2json() exports full database to json string
* DbTool.json2db() imports full database from json string
* DbTool.db2xml() exports full database to xml string
* DbTool.xml2db() imports full database from xml string
* FileTool.stringWrite() writes string to text file in SD card, GZip compression optional
* FileTool.stringRead() reads string from text file in SD card, GZip compression optional


### Limitations

* Tested only with database with 2 tables and about 10 entries
* Many error checks are missing
* String limitations
* JSON parsing implemented with org.json.JSONObject
* XML parsing implemented with SAX


### What's next

* Test with bigger databases 


### Why the name Berengar ?

Berengar of Arundel is the assistant librarian in "The name of the rose" novel by Umberto Eco.
