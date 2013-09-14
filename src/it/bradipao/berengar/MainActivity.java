/*
Berengar
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

   public static final String LOGTAG = "DBER";
   
   // vars
   private DatabaseHelper dbHelper = null;
   private File root,exportDir,jsonFile,xmlFile;
   
   // views
   TextView tvRes;
   Button btCatAdd,btEvnAdd;
   EditText etCatName,etCatIcon;
   EditText etEvnIdcat,etEvnWhen,etEvnName;
   Button btExportJson,btImportJson,btExportXml,btImportXml;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ac_main);
      
      // get database
      dbHelper = DatabaseHelper.getInstance(this);

      // get views
      tvRes = (TextView) findViewById(R.id.tvResult);
      btCatAdd = (Button) findViewById(R.id.btCatAdd);
      btEvnAdd = (Button) findViewById(R.id.btEvnAdd);
      etCatName = (EditText) findViewById(R.id.etCatName);
      etCatIcon = (EditText) findViewById(R.id.etCatIcon);
      etEvnIdcat = (EditText) findViewById(R.id.etEvnIdcat);
      etEvnWhen = (EditText) findViewById(R.id.etEvnWhen);
      etEvnName = (EditText) findViewById(R.id.etEvnName);
      btExportJson = (Button) findViewById(R.id.btExportJSON);
      btImportJson = (Button) findViewById(R.id.btImportJSON);
      btExportXml = (Button) findViewById(R.id.btExportXML);
      btImportXml = (Button) findViewById(R.id.btImportXML);
      
      // check external storage mounted and create folder else notify
      if (FileTool.checkExternalStorage()) {
         root = Environment.getExternalStorageDirectory();
         exportDir = new File(root.getAbsolutePath()+"/berengar");
         if (!exportDir.exists()) exportDir.mkdir();
         if (exportDir.exists()) jsonFile = new File(exportDir,"events.sqlite.json");
         if (exportDir.exists()) xmlFile = new File(exportDir,"events.sqlite.xml");
      }
      
      // button btCatAdd listener
      btCatAdd.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            String sName = etCatName.getText().toString();
            String sIcon = etCatIcon.getText().toString();
            if (getResources().getIdentifier(sIcon,"drawable",getPackageName())==0) sIcon = "ic_generic";
            dbHelper.createCategory(new CategoryItem(sName,sIcon));
         }
      });
      
      // button btCatAdd listener
      btEvnAdd.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            // date extraction
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            try {
               date = formatter.parse(etEvnWhen.getText().toString());
            } catch (ParseException e) {
               e.printStackTrace();
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            // other fields
            long iIdcat = Long.parseLong(etEvnIdcat.getText().toString());
            long iWhen = calendar.getTimeInMillis();
            String sName = etEvnName.getText().toString();
            dbHelper.createEvent(new EventItem(iIdcat,iWhen,sName));
         }
      });

      // button btExport listener
      btExportJson.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            // from database to json string
            String so = "";
            try {
               so = DbTool.db2json(dbHelper.getHandleDB(),"events.sqlite").toString(3);
               // so = DbTool.jsonMinify(so);
            } catch (JSONException e) {
               Log.e(LOGTAG,"error in JSONObject.tostring()",e);
            }
            // from json string to text file
            try {
               FileTool.stringWrite(so,jsonFile,false);
            } catch (IOException e) {
               Log.e(LOGTAG,"error in FileWriter",e);
            }
            tvRes.setText("Database backup saved on SD");
         }
      });
      
      // button btImport listener
      btImportJson.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            // from text file to json string
            String si = "";
            try {
               si = FileTool.stringRead(jsonFile,false);
            } catch (IOException e) {
               Log.e(LOGTAG,"error in FileReader",e);
            }
            // from json string to database
            int iRes = 0;
            try {
               JSONObject jsonDB = new JSONObject(si);
               iRes = DbTool.json2db(dbHelper.getHandleDB(),jsonDB);
            } catch (JSONException e) {
               Log.e(LOGTAG,"error in new JSONObject(jsonDB)",e);
            }
            
            tvRes.setText("num tables : "+String.valueOf(iRes));
         }
      });
      
      // button btExportXML listener
      btExportXml.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            // from database to json string
            String so = "";
            try {
               so = DbTool.db2xml(dbHelper.getHandleDB(),"events.xml");
            } catch (Exception e) {
               Log.e(LOGTAG,"error in XML export",e);
            }
            // from xml string to text file
            try {
               FileTool.stringWrite(so,xmlFile,false);
            } catch (IOException e) {
               Log.e(LOGTAG,"error in FileWriter",e);
            }
            tvRes.setText("Database backup saved on SD");
         }
      });
      
      // button btImportXML listener
      btImportXml.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            // from text file to xml string
            String si = "";
            try {
               si = FileTool.stringRead(xmlFile,false);
            } catch (IOException e) {
               Log.e(LOGTAG,"error in FileReader",e);
            }
            // from xml string to database
            int iRes = 0;
            try {
               iRes = DbTool.xml2db(dbHelper.getHandleDB(),si);
            } catch (Exception e) {
               Log.e(LOGTAG,"error in xml2db",e);
            }
            
            tvRes.setText("num tables : "+String.valueOf(iRes));
         }
      });
      
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.main, menu);
      return true;
   }

}
