/*
Berengar / DatabaseHelper
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

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper {

   // vars
   private static DatabaseHelper mInstance = null;
   private DatabaseOpenHelper mOH;
   private SQLiteDatabase mDB;
   private final Context mCTX;
   private boolean opened = false;
   
   // parameter for database creation and access
   private static final String DATABASE_NAME = "events.db";
   private static final int DATABASE_VERSION = 1;

   // database table CATEGORIES
   public static final String T_CATS   = "cats";
   public static final String C_ID     = "c_id";
   public static final String C_NAME   = "c_name";
   public static final String C_ICON   = "c_icon";
   
   // database table EVENTS
   public static final String T_EVENTS = "events";
   public static final String E_ID     = "e_id";
   public static final String E_IDCAT  = "e_idcat";
   public static final String E_WHEN   = "e_when";
   public static final String E_NAME   = "e_name";   
   
   // query for creating CATEGORIES table
   private static final String CREATE_T_CATS = "CREATE TABLE "+T_CATS+" ("
                               +C_ID   +" INTEGER PRIMARY KEY AUTOINCREMENT, "
                               +C_NAME +" TEXT NOT NULL, "
                               +C_ICON +" TEXT NOT NULL);";
   
   // query for creating EVENTS table
   private static final String CREATE_T_EVENTS = "CREATE TABLE "+T_EVENTS+" ("
                               +E_ID    +" INTEGER PRIMARY KEY AUTOINCREMENT, "
                               +E_IDCAT +" INTEGER NOT NULL, "
                               +E_WHEN  +" INTEGER NOT NULL, "
                               +E_NAME  +" TEXT NOT NULL);";
   
   // static creator, create first time or return existing one
   public static DatabaseHelper getInstance(Context ctx) {
      if (mInstance==null) {
         mInstance = new DatabaseHelper(ctx.getApplicationContext());
      }
      return mInstance;
    }

   //---- OPENHELPER ----------------------------------------------------------

   private static class DatabaseOpenHelper extends SQLiteOpenHelper {
      // constructor
      DatabaseOpenHelper(Context context) {
         super(context,DATABASE_NAME,null,DATABASE_VERSION);
      }
      // called if DB does not exist : create tables
      public void onCreate(SQLiteDatabase db) {
         db.execSQL(CREATE_T_CATS);
         db.execSQL(CREATE_T_EVENTS);
      }
      // called if DB version is increased : delete and recreate whole DB
      public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
         db.execSQL("DROP TABLE IF EXISTS "+T_CATS);
         db.execSQL("DROP TABLE IF EXISTS "+T_EVENTS);
         onCreate(db);
      }
   }
   
   //---- DATABASE MANAGEMENT --------------------------------------------------
   
   // constructor
   public DatabaseHelper(Context ctx) {
      mCTX = ctx;
      mOH = new DatabaseOpenHelper(mCTX);
   }
   
   // open database
   public SQLiteDatabase openDB() {
      if (!opened) {
         mDB = mOH.getWritableDatabase();
         opened = true;
      }
      return mDB;
   }

   // get handle (open database if closed)
   public SQLiteDatabase getHandleDB() {
      return openDB();
   }

   // close database
   public void closeDB() {
      if (opened) mOH.close();
      opened = false;
   }

   // reset database
   public void resetDB() {
      openDB();
      mOH.onUpgrade(this.mDB,1,1);
      closeDB();
   }

   // ---- CATEGORY MANAGEMENT ------------------------------------------------
   
   // create CATEGORY entry
   public long createCategory(CategoryItem item) {
      openDB();
      ContentValues cv = new ContentValues();
      cv.put( C_NAME , item.s_name );
      cv.put( C_ICON , item.s_icon );
      return mDB.insert(T_CATS,null,cv);
   }
   
   // update CATEGORY entry
   public void updateCategory(CategoryItem item) {
      openDB();
      ContentValues cv = new ContentValues();
      cv.put( C_NAME , item.s_name );
      cv.put( C_ICON , item.s_icon );
      String params = C_ID+"=?";
      String[] args = new String[] {Long.toString(item.l_id)};
      mDB.update(T_CATS,cv,params,args);
   }
   
   // delete CATEGORY entry
   public void deleteCategory(long id) {
      openDB();
      String params = C_ID+"=?";
      String[] args = new String[] {Long.toString(id)};
      mDB.delete(T_CATS,params,args);
   }
   
   // count CATEGORY entries
   public int countCategory(long vid) {
      openDB();
      String [] columns = { C_ID };
      Cursor cRes = mDB.query(T_CATS,columns,null,null,null,null,null);
      int cnt = cRes.getCount();
      cRes.close();
      return cnt;
   }
   
   // get CATEGORY entry
   public CategoryItem getCategory(long id) {
      openDB();
      CategoryItem item = null;
      Long lid = Long.valueOf(id);
      String [] columns = { C_ID,C_NAME,C_ICON };
      String params = C_ID+"=?";
      String [] args = { lid.toString() };
      
      Cursor cRes = mDB.query(T_CATS,columns,params,args,null,null,null);
      cRes.moveToFirst();
      while (!cRes.isAfterLast()) {
         item = cursorToCategory(cRes);
         cRes.moveToNext();
      }
      cRes.close();

      return item;
   }
   
   // get array of CATEGORY entries
   public ArrayList<CategoryItem> getarrayCategory() {
      openDB();
      ArrayList<CategoryItem> al = new ArrayList<CategoryItem>();
      String [] columns = { C_ID,C_NAME,C_ICON };
      String orderby = C_ID;

      Cursor cRes = mDB.query(T_CATS,columns,null,null,null,null,orderby);
      cRes.moveToFirst();
      while (!cRes.isAfterLast()) {
         CategoryItem item = cursorToCategory(cRes);
         al.add(item);
         cRes.moveToNext();
      }
      cRes.close();
      
      return al;
   }

   // get array of CATEGORY entries
   public ArrayList<CategoryItem> getarrayCategory(int from,int tot) {
      openDB();
      ArrayList<CategoryItem> al = new ArrayList<CategoryItem>();
      String [] columns = { C_ID,C_NAME,C_ICON };
      String orderby = C_ID;

      Cursor cRes = mDB.query(T_CATS,columns,null,null,null,null,orderby);
      cRes.moveToFirst();
      
      // reset cursor and exit if FROM not reachable or TOT<=0 or FROM<0
      if ((from<0)||(tot<=0)||(!cRes.move(from))) {
         cRes.close();
         return al;
      }
      // retrieve TOT rows
      int nn = 0;
      while ((!cRes.isAfterLast())&&(nn<tot)) {
         CategoryItem item = cursorToCategory(cRes);
         al.add(item);
         cRes.moveToNext();
         nn++;
      }
      cRes.close();
      
      return al;
   }
   
   // get cursor of CATEGORY entries
   public Cursor getcursorCategory() {
      openDB();
      String [] columns = { C_ID,C_NAME,C_ICON };
      String orderby = C_ID;
      return mDB.query(T_CATS,columns,null,null,null,null,orderby);
   }
   
   // helper function extracts VehicleItem from cursor
   private CategoryItem cursorToCategory(Cursor c) {
      CategoryItem ci = new CategoryItem(c.getLong(0),c.getString(1),c.getString(2));
      ci.i_icon = mCTX.getResources().getIdentifier(c.getString(2),"drawable",mCTX.getPackageName());
      if (ci.i_icon==0) ci.i_icon = R.drawable.ic_generic;
      return ci;
   }
   
   // ---- EVENT MANAGEMENT ------------------------------------------------
   
   // create EVENT entry
   public long createEvent(EventItem item) {
      openDB();
      ContentValues cv = new ContentValues();
      cv.put( E_IDCAT , item.l_idcat );
      cv.put( E_WHEN  , item.l_when );
      cv.put( E_NAME  , item.s_name );
      return mDB.insert(T_EVENTS,null,cv);
   }
   
   // update EVENT entry
   public void updateEvent(EventItem item) {
      openDB();
      ContentValues cv = new ContentValues();
      cv.put( E_IDCAT , item.l_idcat );
      cv.put( E_WHEN  , item.l_when );
      cv.put( E_NAME  , item.s_name );
      String params = E_ID+"=?";
      String[] args = new String[] {Long.toString(item.l_id)};
      mDB.update(T_EVENTS,cv,params,args);
   }
   
   // delete EVENT entry
   public void deleteEvent(long id) {
      openDB();
      String params = E_ID+"=?";
      String[] args = new String[] {Long.toString(id)};
      mDB.delete(T_EVENTS,params,args);
   }
 
   // count EVENT entries
   public int countEvent() {
      openDB();
      String [] columns = { E_ID };
      Cursor cRes = mDB.query(T_EVENTS,columns,null,null,null,null,null);
      int cnt = cRes.getCount();
      cRes.close();
      return cnt;
   }

   // get EVENT entry
   public EventItem getEvent(long id) {
      openDB();
      EventItem item = null;
      Long lid = Long.valueOf(id);
      String [] columns = { E_ID,E_IDCAT,E_WHEN,E_NAME };
      String params = E_ID+"=?";
      String [] args = { lid.toString() };
      
      Cursor cRes = mDB.query(T_EVENTS,columns,params,args,null,null,null);
      cRes.moveToFirst();
      while (!cRes.isAfterLast()) {
         item = cursorToEvent(cRes);
         cRes.moveToNext();
      }
      cRes.close();

      return item;
   }

   // get array of EVENT entries
   public ArrayList<EventItem> getarrayEvent() {
      openDB();
      ArrayList<EventItem> al = new ArrayList<EventItem>();
      String [] columns = { E_ID,E_IDCAT,E_WHEN,E_NAME };
      String orderby = E_ID;

      Cursor cRes = mDB.query(T_EVENTS,columns,null,null,null,null,orderby);
      cRes.moveToFirst();
      while (!cRes.isAfterLast()) {
         EventItem item = cursorToEvent(cRes);
         al.add(item);
         cRes.moveToNext();
      }
      cRes.close();
      
      return al;
   }

   // get array of EVENT entries
   public ArrayList<EventItem> getarrayEvent(int from,int tot) {
      openDB();
      ArrayList<EventItem> al = new ArrayList<EventItem>();
      String [] columns = { E_ID,E_IDCAT,E_WHEN,E_NAME };
      String orderby = E_ID;

      Cursor cRes = mDB.query(T_EVENTS,columns,null,null,null,null,orderby);
      cRes.moveToFirst();
      
      // reset cursor and exit if FROM not reachable or TOT<=0 or FROM<0
      if ((from<0)||(tot<=0)||(!cRes.move(from))) {
         cRes.close();
         return al;
      }
      // retrieve TOT rows
      int nn = 0;
      while ((!cRes.isAfterLast())&&(nn<tot)) {
         EventItem item = cursorToEvent(cRes);
         al.add(item);
         cRes.moveToNext();
         nn++;
      }
      cRes.close();
      
      return al;
   }

   // get cursor of EVENT entries
   public Cursor getcursorEvent() {
      openDB();
      String [] columns = { E_ID,E_IDCAT,E_WHEN,E_NAME };
      String orderby = E_ID;
      return mDB.query(T_EVENTS,columns,null,null,null,null,orderby);
   }

   // helper function extracts EventItem from cursor
   private EventItem cursorToEvent(Cursor c) {
      EventItem ci = new EventItem(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3));
      return ci;
   }

   
   
}
