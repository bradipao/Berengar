/*
Berengar DbTool
Copyright (c) 2013 Bradipao <bradipao@gmail.com>
http://gplus.to/Bradipao

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package it.bradipao.berengar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class DbTool {

   public static final String LOGTAG = "DBTOOL";
   public static final boolean GOLOG = true;
   
   //---- JSON2DB -------------------------------------------------------------
   
   public static int json2db(SQLiteDatabase mDB,JSONObject jsonDB) {
      
      // vars
      int iTableNum = 0;
      JSONArray jsonTables = new JSONArray();
      
      int iRes = 0;
      try {
         // recover tables number
         iTableNum = Integer.parseInt(jsonDB.getString("tables_num"));
         if (GOLOG) Log.d(LOGTAG,"TABLE NUM : "+iTableNum);
         // recover tables
         jsonTables = jsonDB.getJSONArray("tables");
         for (int i=0;i<jsonTables.length();i++) {
            iRes += json2table(mDB,jsonTables.getJSONObject(i));
         }
      } catch (JSONException e) {
         Log.e(LOGTAG,"error in json2db",e);
      }
      
      return iRes;
   }

   //---- JSON2TABLE ----------------------------------------------------------
   
   public static int json2table(SQLiteDatabase mDB,JSONObject jsonTable) {
      
      // vars
      JSONArray jsonRows = new JSONArray();
      JSONArray jsonColsName = new JSONArray();
      JSONArray jsonCols = null;
      ContentValues cv = null;
      
      int iRes = 0;
      try {
         // init database transaction
         mDB.beginTransaction();
         // fetch table name and drop if exists
         String sTableName = jsonTable.getString("table_name");
         mDB.execSQL("DROP TABLE IF EXISTS "+sTableName);
         if (GOLOG) Log.d(LOGTAG,"TABLE NAME : "+sTableName);
         // fetch and execute create sql
         String sTableSql = jsonTable.getString("table_sql");
         mDB.execSQL(sTableSql);
         // fetch columns name
         jsonColsName = jsonTable.getJSONArray("cols_name");
         // fetch rows array
         jsonRows = jsonTable.getJSONArray("rows");
         // iterate through rows
         for (int i=0;i<jsonRows.length();i++) {
            // fetch columns
            jsonCols = jsonRows.getJSONArray(i);
            // perform insert
            cv = new ContentValues();
            for (int j=0;j<jsonCols.length();j++) cv.put( jsonColsName.getString(j) , jsonCols.getString(j) );
            mDB.insert(sTableName,null,cv);
            if (GOLOG) Log.d(LOGTAG,"INSERT IN "+sTableName+" ID="+jsonCols.getString(0));
         }
         iRes++;
         // set transaction successful
         mDB.setTransactionSuccessful();
      } catch (Exception e) {
         Log.e(LOGTAG,"error in json2table",e);
      } finally {
         // end transaction, commit if successful else rollback
         mDB.endTransaction();
      }
      
      return iRes;
   }

   //---- GSON2DB -------------------------------------------------------------
   // strong constraint is order of fields into table object in json file
   // order should be : table_name / table_sql / cols_name / rows
   // or : cols_name / table_name / table_sql / rows
   
   public static int gson2db(SQLiteDatabase mDB,File jsonFile) {
      
      // vars
      int iTableNum = 0;
      FileReader fr = null;
      BufferedReader br = null;
      JsonReader jr = null;
      String name = null;
      String val = null;
      
      String mTable = null;
      String mTableSql = null;
      ArrayList<String> xmlFields = null;
      ArrayList<String> xmlValues = null;
      ContentValues cv = null;
      
      // file readers
      try {
         fr = new FileReader(jsonFile);
         br = new BufferedReader(fr);
         jr = new JsonReader(br);
      } catch (FileNotFoundException e) {
         Log.e(LOGTAG,"error in gson2db file readers",e);
      }
      
      // parsing
      try {
         // start database transaction
         mDB.beginTransaction();
         // open root {
         jr.beginObject();
         // iterate through root objects
         while (jr.hasNext()) {
            name = jr.nextName();
            if (jr.peek()==JsonToken.NULL) jr.skipValue();
            // number of tables
            else if (name.equals("tables_num")) {
               val = jr.nextString();
               iTableNum = Integer.parseInt(val);
               if (GOLOG) Log.d(LOGTAG,"TABLE NUM : "+iTableNum);
            }
            // iterate through tables array
            else if (name.equals("tables")) {
               jr.beginArray();
               while (jr.hasNext()) {
                  // start table
                  mTable = null;
                  xmlFields = null;
                  jr.beginObject();
                  while (jr.hasNext()) {
                     name = jr.nextName();
                     if (jr.peek()==JsonToken.NULL) jr.skipValue();
                     // table name
                     else if (name.equals("table_name")) {
                        mTable = jr.nextString();
                     }
                     // table sql
                     else if (name.equals("table_sql")) {
                        mTableSql = jr.nextString();
                        if ((mTable!=null)&&(mTableSql!=null)) {
                           mDB.execSQL("DROP TABLE IF EXISTS "+mTable);
                           mDB.execSQL(mTableSql);
                           if (GOLOG) Log.d(LOGTAG,"DROPPED AND CREATED TABLE : "+mTable);
                        }
                     }
                     // iterate through columns name
                     else if (name.equals("cols_name")) {
                        jr.beginArray();
                        while (jr.hasNext()) {
                           val = jr.nextString();
                           if (xmlFields==null) xmlFields = new ArrayList<String>();
                           xmlFields.add(val);
                        }
                        jr.endArray();
                        if (GOLOG) Log.d(LOGTAG,"COLUMN NAME : "+xmlFields.toString());
                     }
                     // iterate through rows
                     else if (name.equals("rows")) {
                        jr.beginArray();
                        while (jr.hasNext()) {
                           jr.beginArray();
                           // iterate through values in row
                           xmlValues = null;
                           cv = null;
                           while (jr.hasNext()) {
                              val = jr.nextString();
                              if (xmlValues==null) xmlValues = new ArrayList<String>();
                              xmlValues.add(val);
                           }
                           jr.endArray();
                           // add to database
                           cv = new ContentValues();
                           for (int j=0;j<xmlFields.size();j++) cv.put( xmlFields.get(j) , xmlValues.get(j) );
                           mDB.insert(mTable,null,cv);
                           if (GOLOG) Log.d(LOGTAG,"INSERT IN "+mTable+" : "+xmlValues.toString());
                        }
                        jr.endArray();
                     }
                     else jr.skipValue();
                  }
                  // end table
                  jr.endObject();
               }
               jr.endArray();
            }
            else jr.skipValue();   
         }
         // close root }
         jr.endObject();
         jr.close();
         // successfull transaction
         mDB.setTransactionSuccessful();
      } catch (IOException e) {
         Log.e(LOGTAG,"error in gson2db gson parsing",e);
      } finally {
         mDB.endTransaction();
      }

      return iTableNum;
   }
   
   //---- DB2JSON -------------------------------------------------------------
   
   public static JSONObject db2json(SQLiteDatabase mDB,String sDbName) {
      // vars
      JSONObject jsonDB = new JSONObject();
      JSONArray jsonNameTables = new JSONArray();
      JSONArray jsonTables = new JSONArray();
      
      // read tables
      String sqlquery = "select * from sqlite_master";
      Cursor cur = mDB.rawQuery(sqlquery,null);
      // iterate through tables
      int iTableNum = 0;
      String sTableName = "";
      String sTableSql = "";
      while (cur.moveToNext()) {
         sTableName = cur.getString(cur.getColumnIndex("name"));
         sTableSql = cur.getString(cur.getColumnIndex("sql"));
         if (GOLOG) Log.d(LOGTAG,"TABLE NAME : "+sTableName);
         // skip metadata, sequence, and uidx before exporting tables
         if (!sTableName.equals("android_metadata") && !sTableName.equals("sqlite_sequence")
         && !sTableName.startsWith("uidx") && !sTableName.startsWith("idx_") && !sTableName.startsWith("_idx")) {
            // add new table
            iTableNum++;
            jsonNameTables.put(sTableName);
            // try exporting table
            jsonTables.put(table2json(mDB,sTableName,sTableSql));
         }
      }
      cur.close();
      
      // final json building
      try {
         // json db format
         jsonDB.put("jsondb_format","1");
         // database name
         if ((sDbName!=null)&&(!sDbName.isEmpty())) jsonDB.put("db_name",sDbName);
         else jsonDB.put("db_name","database.sqlite");
         // tables number and name
         jsonDB.put("tables_num",String.valueOf(iTableNum));
         jsonDB.put("tables_name",jsonNameTables);
         // tables
         jsonDB.put("tables",jsonTables);
      } catch (JSONException e) {
         Log.e(LOGTAG,"error in db2json",e);
      }
      
      // return String
      return jsonDB;
   }
   
   //---- TABLE2JSON ----------------------------------------------------------
   
   public static JSONObject table2json(SQLiteDatabase mDB,String sTableName,String sTableSql) {
      // vars
      JSONObject jsonTable = new JSONObject();
      JSONArray jsonRows = new JSONArray();
      JSONArray jsonColsName = new JSONArray();
      JSONArray jsonCols = null;
      
      // read table
      String sqlquery = "select * from " + sTableName;
      Cursor cur = mDB.rawQuery(sqlquery,null);
      // iteratew through rows
      int i = -1;
      while (cur.moveToNext()) {
         // at first element store column names
         if (i==-1) for (i=0;i<cur.getColumnCount();i++) {
            jsonColsName.put(cur.getColumnName(i));
         }
         // get values
         jsonCols = new JSONArray();
         for (i=0;i<cur.getColumnCount();i++) {
            jsonCols.put(cur.getString(i));
         }
         // add values to rows array
         jsonRows.put(jsonCols);
      }
      
      // final json building
      try {
         // table name
         jsonTable.put("table_name",sTableName);
         // code for create table
         if ((sTableSql!=null)&&(!sTableSql.isEmpty())) jsonTable.put("table_sql",sTableSql);
         // columns name
         jsonTable.put("cols_name",jsonColsName);
         // rows
         jsonTable.put("rows",jsonRows);
      } catch (JSONException e) {
         Log.e(LOGTAG,"error in table2json",e);
      }
      
      // return String
      return jsonTable;
   }
   
   //---- DB2GSON -------------------------------------------------------------
   
   public static int db2gson(SQLiteDatabase mDB,File jsonFile) {
      // vars
      int iTableNum = 0;
      FileWriter fw = null;
      BufferedWriter bw = null;
      JsonWriter jw = null; 
      String sqlquery = "";
      Cursor cur = null; 
      
      String mTable = null;
      String mTableSql = null;
      ArrayList<String> aTable = new ArrayList<String>();
      ArrayList<String> aTableSql = new ArrayList<String>();
      
      // file writers
      try {
         fw = new FileWriter(jsonFile);
         bw = new BufferedWriter(fw);
         jw = new JsonWriter(bw);
      } catch (FileNotFoundException e) {
         Log.e(LOGTAG,"error in db2gson file writers",e);
      } catch (IOException e) {
         Log.e(LOGTAG,"error in db2gson file writers",e);
      }
      
      // read tables list and extract name and createsql
      sqlquery = "select * from sqlite_master";
      cur = mDB.rawQuery(sqlquery,null);
      while (cur.moveToNext()) {
         mTable = cur.getString(cur.getColumnIndex("name"));
         mTableSql = cur.getString(cur.getColumnIndex("sql"));
         // add new table, and skip metadata, sequence, and uidx before exporting tables
         if (!mTable.equals("android_metadata") && !mTable.equals("sqlite_sequence")
         && !mTable.startsWith("uidx") && !mTable.startsWith("idx_") && !mTable.startsWith("_idx")) {
            iTableNum++;
            aTable.add(mTable);
            aTableSql.add(mTableSql);
            if (GOLOG) Log.d(LOGTAG,"TABLE NAME : "+mTable);
         }
      }
      cur.close();
      
      // start writing json
      try {
         // open root {
         jw.beginObject();
         // header elements
         jw.name("tables_num").value(Integer.toString(iTableNum));
         jw.name("jsondb_format").value("1");
         // tables name
         jw.name("tables_name");
         jw.beginArray();
         for(int i=0;i<aTable.size();i++) jw.value(aTable.get(i));
         jw.endArray();
         
         // open tables array
         jw.name("tables");
         jw.beginArray();
         // iterate through tables
         for(int i=0;i<aTable.size();i++) {
            // open table object
            jw.beginObject();
            // table name and table sql
            jw.name("table_name").value(aTable.get(i));
            jw.name("table_sql").value(aTableSql.get(i));
            // iteratew through rows
            sqlquery = "select * from " + aTable.get(i);
            cur = mDB.rawQuery(sqlquery,null);
            int k = -1;
            while (cur.moveToNext()) {
               if (k==-1) {
                  // column names generated at very first row
                  jw.name("cols_name");
                  jw.beginArray();
                  for (k=0;k<cur.getColumnCount();k++) jw.value(cur.getColumnName(k));
                  jw.endArray();
                  // open rows array
                  jw.name("rows");
                  jw.beginArray();
               }
               // get columns values in row
               jw.beginArray();
               for (k=0;k<cur.getColumnCount();k++) jw.value(cur.getString(k));
               jw.endArray();
            }
            // close rows array
            jw.endArray();
            // close table object
            jw.endObject();
         }
         // close tables array
         jw.endArray();
         
         // close root {
         jw.endObject();
         jw.close();
      } catch (IOException e) {
         Log.e(LOGTAG,"error in db2gson file writers",e);
      }

      // return number of tables
      return iTableNum;
   }
   
   //---- XML2DB --------------------------------------------------------------
   
   public static int xml2db(SQLiteDatabase mDB,String xmlDB) {   
      XmlDbParser xdp = new XmlDbParser(mDB);
      return xdp.parse(xmlDB.getBytes());
   }
   
   public static class XmlDbParser extends DefaultHandler {
      // nodes
      final String XML_DATABASE = "database";
      final String XML_DBNAME = "dbname";
      final String XML_TABLES = "tables";
      final String XML_TABLE = "table";
      final String XML_TABLENAME = "tablename";
      final String XML_TABLESQL = "tablesql";
      final String XML_COLSNAME = "colsname";
      final String XML_ROWS = "rows";
      final String XML_ROW = "r";
      final String XML_COL = "c";
      
      // section flags
      boolean bTableName = false;
      boolean bTableSql = false;
      boolean bColsName = false;
      boolean bC = false;
      boolean bR = false;
      
      // vars
      SQLiteDatabase mDB = null;
      int iTableNum = 0;
      String mNode = null;
      String mVal = null;
      String mTable = null;
      ArrayList<String> xmlFields = null;
      ArrayList<String> xmlValues = null;
      ContentValues cv = null;
      
      // constructor
      XmlDbParser(SQLiteDatabase db) {
         mDB = db;
      }
      
      // parse method
      public int parse(byte[] data) {
         // create parse factory
         SAXParserFactory factory = SAXParserFactory.newInstance();
         try {
            SAXParser parser = factory.newSAXParser();
            mDB.beginTransaction();
            parser.parse(new ByteArrayInputStream(data),this);
         } catch (Exception e) {
            Log.e(LOGTAG,"error in xml2db",e);
         } finally {
            mDB.endTransaction();
         }
         // return number of tables written in database
         return iTableNum;
      }
      
      // start element method
      @Override
      public void startElement(String uri,String localName,String name,Attributes attributes) throws SAXException {
         super.startElement(uri,localName,name,attributes);
         
         // setting mNode
         if (name.trim().length()==0) mNode = localName;
         else mNode = name;
         
         // reset values at table start and begin transaction
         if (mNode.equals(XML_TABLE)) {
            mVal = null;
            mTable = null;
            xmlFields = null;
            if (GOLOG) Log.d(LOGTAG,"BEGIN TABLE");
         }
         
         // reset values at row start
         if (mNode.equals(XML_ROW)) {
            xmlValues = null;
            cv = null;
         }
         
         // start node flag
         if (mNode.equals(XML_TABLENAME)) bTableName = true;
         if (mNode.equals(XML_TABLESQL)) bTableSql = true;
         if (mNode.equals(XML_COLSNAME)) bColsName = true;
         if (mNode.equals(XML_COL)) bC = true;
         if (mNode.equals(XML_ROW)) bR = true;

      }
      
      // characters method
      @Override
      public void characters(char[] ch,int start,int length) throws SAXException {
         super.characters(ch,start,length);
         
         // save table name
         if (bTableName) {
            mTable = new String(ch).substring(start,start+length);
            if (GOLOG) Log.d(LOGTAG,"NEW TABLE : "+mTable);
         }
         
         // drope table and create new one, transactioned
         if (bTableSql) {
            String sCreateSql = new String(ch).substring(start,start+length);
            if (mTable!=null) {
               mDB.execSQL("DROP TABLE IF EXISTS "+mTable);
               mDB.execSQL(sCreateSql);
               if (GOLOG) Log.d(LOGTAG,"DROPPED AND CREATED TABLE : "+mTable);
            }
         }
         
         // fetching columns names
         if (bColsName && bC) {
            mVal = new String(ch).substring(start,start+length);
            if (xmlFields==null) xmlFields = new ArrayList<String>();
            xmlFields.add(mVal);
         }
         
         // fetching columns values
         if (bR && bC) {
            mVal = new String(ch).substring(start,start+length);
            if (xmlValues==null) xmlValues = new ArrayList<String>();
            xmlValues.add(mVal);
         }
            
      }
      
      // end element method
      @Override
      public void endElement(String uri,String localName,String name) throws SAXException {
         super.endElement(uri,localName,name);
         
         // setting mNode
         if (name.trim().length()==0) mNode = localName;
         else mNode = name;
         if (mNode==null) return;
         
         // insert at end of row
         if (mNode.equals(XML_ROW)) {
            cv = new ContentValues();
            for (int j=0;j<xmlFields.size();j++) cv.put( xmlFields.get(j) , xmlValues.get(j) );
            mDB.insert(mTable,null,cv);
            if (GOLOG) Log.d(LOGTAG,"INSERT IN "+mTable+" : "+xmlValues.toString());
         }
         
         // end of table
         if (mNode.equals(XML_TABLE)) {
            iTableNum++;
            if (GOLOG) Log.d(LOGTAG,"END TABLE");
         }
         
         // end of database
         if (mNode.equals(XML_DATABASE)) {
            mDB.setTransactionSuccessful();
            if (GOLOG) Log.d(LOGTAG,"END TABLE");
         }
         
         // end node flags
         if (mNode.equals(XML_TABLENAME)) bTableName = false;
         if (mNode.equals(XML_TABLESQL)) bTableSql = false;
         if (mNode.equals(XML_COLSNAME)) bColsName = false;
         if (mNode.equals(XML_COL)) bC = false;
         if (mNode.equals(XML_ROW)) bR = false;
         
      }
      
      // start and end document method
      @Override
      public void startDocument() throws SAXException {
         super.startDocument();
      }
      @Override
      public void endDocument() throws SAXException {
         super.endDocument();
      } 
      
   }

   
   //---- DB2XML --------------------------------------------------------------
   
   public static String db2xml(SQLiteDatabase mDB,String sDbName) {
      // vars
      final String XML_DATABASE = "database";
      final String XML_DBNAME = "dbname";
      final String XML_TABLES = "tables";
      final String XML_TABLE = "table";
      final String XML_TABLENAME = "tablename";
      final String XML_TABLESQL = "tablesql";
      final String XML_COLSNAME = "colsname";
      final String XML_ROWS = "rows";
      final String XML_ROW = "r";
      final String XML_COL = "c";
      
      // tables list query and cursor
      String tblquery = "select * from sqlite_master";
      Cursor tblcur = mDB.rawQuery(tblquery,null);
      String rowquery = "";
      Cursor rowcur = null;

      // xml serializer
      XmlSerializer sr = Xml.newSerializer();
      StringWriter sw = new StringWriter();
      
      try {
         // prepare xml document
         sr.setOutput(sw);
         sr.startDocument("UTF-8",true);
         sr.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output",true);
         
         // start document
         sr.startTag("",XML_DATABASE);
         sr.startTag("",XML_DBNAME);
         if ((sDbName!=null)&&(!sDbName.isEmpty())) sr.text(sDbName);
         else sr.text("db_name");
         sr.endTag("",XML_DBNAME);
         sr.startTag("",XML_TABLES);
         
         // iterate through tables
         String sTableName = "";
         String sTableSql = "";
         while (tblcur.moveToNext()) {
            sTableName = tblcur.getString(tblcur.getColumnIndex("name"));
            sTableSql = tblcur.getString(tblcur.getColumnIndex("sql"));
            if (GOLOG) Log.d(LOGTAG,"TABLE NAME : "+sTableName);
            // skip metadata, sequence, and uidx before exporting tables
            if (!sTableName.equals("android_metadata") && !sTableName.equals("sqlite_sequence")
            && !sTableName.startsWith("uidx") && !sTableName.startsWith("idx_") && !sTableName.startsWith("_idx")) {

               // table query and cursor
               rowquery = "select * from " + sTableName;
               rowcur = mDB.rawQuery(rowquery,null);
               // exporting table
               sr.startTag("",XML_TABLE);
               sr.startTag("",XML_TABLENAME);
               sr.text(sTableName);
               sr.endTag("",XML_TABLENAME);
               if ((sTableSql!=null)&&(!sTableSql.isEmpty())) {
                  sr.startTag("",XML_TABLESQL);
                  sr.text(sTableSql);
                  sr.endTag("",XML_TABLESQL);
               }
               // iteratew through rows
               int i = -1;
               while (rowcur.moveToNext()) {
                  // at first element store column names
                  if (i==-1) {
                     sr.startTag("",XML_COLSNAME);
                     for (i=0;i<rowcur.getColumnCount();i++) {
                        sr.startTag("",XML_COL);
                        sr.text(rowcur.getColumnName(i));
                        sr.endTag("",XML_COL);
                     }
                     sr.endTag("",XML_COLSNAME);
                     sr.startTag("",XML_ROWS);
                  }
                  // get values
                  sr.startTag("",XML_ROW);
                  for (i=0;i<rowcur.getColumnCount();i++) {
                     sr.startTag("",XML_COL);
                     sr.text(rowcur.getString(i));
                     sr.endTag("",XML_COL);
                  }
                  sr.endTag("",XML_ROW);
               }
               // finishing table query
               rowcur.close();
               sr.endTag("",XML_ROWS);
               sr.endTag("",XML_TABLE);

            }
         }
         // finishing table query
         tblcur.close();
         sr.endTag("",XML_TABLES);
         sr.endTag("",XML_DATABASE);
         
         // finishing
         sr.endDocument();
         sr.flush();
      
      } catch (Exception e) {
         Log.e(LOGTAG,"error in db2xml",e);
      }
   
      return sw.toString();
   }
   
   //---- TABLE2XML -----------------------------------------------------------
   // to test or remove
   
   public static String table2xml(SQLiteDatabase mDB,String sTableName,String sTableSql,XmlSerializer xsr) {
      // vars
      //StringBuilder sb = new StringBuilder(256);
      final String XML_TABLE = "table";
      final String XML_TABLENAME = "tablename";
      final String XML_TABLESQL = "tablesql";
      final String XML_COLSNAME = "colsname";
      final String XML_ROWS = "rows";
      final String XML_ROW = "r";
      final String XML_COL = "c";
      
      // read table
      String sqlquery = "select * from " + sTableName;
      Cursor cur = mDB.rawQuery(sqlquery,null);

      // xml serializer
      XmlSerializer sr = null;
      StringWriter sw = null;
      
      try {
         // if XMLserializer is passed then use it else create new
         if (xsr!=null) sr = xsr;
         else {
            sr = Xml.newSerializer();
            sw = new StringWriter();
            sr.setOutput(sw);
            sr.startDocument("UTF-8",true);
            sr.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output",true);
         }
          
         // start table
         sr.startTag("",XML_TABLE);
         sr.startTag("",XML_TABLENAME);
         sr.text(sTableName);
         sr.endTag("",XML_TABLENAME);
         if ((sTableSql!=null)&&(!sTableSql.isEmpty())) {
            sr.startTag("",XML_TABLESQL);
            sr.text(sTableSql);
            sr.endTag("",XML_TABLESQL);
         }
         
         // iteratew through rows
         int i = -1;
         while (cur.moveToNext()) {
            // at first element store column names
            if (i==-1) {
               sr.startTag("",XML_COLSNAME);
               for (i=0;i<cur.getColumnCount();i++) {
                  sr.startTag("",XML_COL);
                  sr.text(cur.getColumnName(i));
                  sr.endTag("",XML_COL);
               }
               sr.endTag("",XML_COLSNAME);
               sr.startTag("",XML_ROWS);
            }
            // get values
            sr.startTag("",XML_ROW);
            for (i=0;i<cur.getColumnCount();i++) {
               sr.startTag("",XML_COL);
               sr.text(cur.getString(i));
               sr.endTag("",XML_COL);
            }
            sr.endTag("",XML_ROW);
         }
         // finishing table query
         cur.close();
         sr.endTag("",XML_ROWS);
         sr.endTag("",XML_TABLE);
         
         // finishing
         sr.endDocument();
         sr.flush();

      } catch (Exception e) {
         Log.e(LOGTAG,"error in table2xml",e);
      }
   
      // if XMLserializer created here, return all XML else return empty
      if (xsr==null) return sw.toString(); else return "";
   }

   
   //---- JSONMINIFY ----------------------------------------------------------
   // Java porting created by Bernhard Gass on 8/01/13
   // Ported from https://github.com/getify/JSON.minify
   // distributed under MIT License
   public static String jsonMinify(String jsonString) {
      String tokenizer = "\"|(/\\*)|(\\*/)|(//)|\\n|\\r";
      String magic = "(\\\\)*$";
      Boolean in_string = false;
      Boolean in_multiline_comment = false;
      Boolean in_singleline_comment = false;
      String tmp = "";
      String tmp2 = "";
      List<String> new_str = new ArrayList<String>();
      Integer from = 0;
      String lc = "";
      String rc = "";
      
      Pattern pattern = Pattern.compile(tokenizer);
      Matcher matcher = pattern.matcher(jsonString);
      
      Pattern magicPattern = Pattern.compile(magic);
      Matcher magicMatcher = null;
      Boolean foundMagic = false;
      
      if (!matcher.find()) return jsonString;
      else matcher.reset();
      
      while (matcher.find()) {
         lc = jsonString.substring(0, matcher.start());
         rc = jsonString.substring(matcher.end(), jsonString.length());
         tmp = jsonString.substring(matcher.start(), matcher.end());
         
         if (!in_multiline_comment && !in_singleline_comment) {
            tmp2 = lc.substring(from);
            if (!in_string)
            tmp2 = tmp2.replaceAll("(\\n|\\r|\\s)*","");            
            new_str.add(tmp2);
         }
         from = matcher.end();   
         
         if (tmp.charAt(0) == '\"' && !in_multiline_comment && !in_singleline_comment) {
            magicMatcher = magicPattern.matcher(lc);
            foundMagic = magicMatcher.find();
            if (!in_string || !foundMagic || (magicMatcher.end() - magicMatcher.start()) % 2 == 0) {
               in_string = !in_string;
            }
            from--;
            rc = jsonString.substring(from);
         }
         else if (tmp.startsWith("/*") && !in_string && !in_multiline_comment && !in_singleline_comment) {
            in_multiline_comment = true;
         }
         else if (tmp.startsWith("*/") && !in_string && in_multiline_comment && !in_singleline_comment) {
            in_multiline_comment = false;
         }
         else if (tmp.startsWith("//") && !in_string && !in_multiline_comment && !in_singleline_comment) {
            in_singleline_comment = true;
         }
         else if ((tmp.startsWith("\n") || tmp.startsWith("\r")) && !in_string && !in_multiline_comment && in_singleline_comment) {
            in_singleline_comment = false;
         }
         else if (!in_multiline_comment && !in_singleline_comment && !tmp.substring(0, 1).matches("\\n|\\r|\\s")) {
            new_str.add(tmp);
         }
      }
      
      new_str.add(rc);  
      StringBuffer sb = new StringBuffer();
      for (String str : new_str) sb.append(str);
      
      return sb.toString();
   }
   
   
}









