package com.foursevengames.speaknews;
 
import java.util.ArrayList;
import java.util.List;
 
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
 
public class DatabaseHandler extends SQLiteOpenHelper {

  // All Static variables
  // Database Version
  private static final int DATABASE_VERSION = 2; 
  // Database Name
  private static final String DATABASE_NAME = "myDatabase";
  // Feeds table name
  private static final String TABLE_FEEDS = "rssFeeds";
  // Feeds Table Columns names
  private static final String KEY_ID = "id";
  private static final String KEY_NAME = "name";
  private static final String KEY_URL = "rssUrl";
 
  public DatabaseHandler(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }
 
  // Creating Tables
  @Override
  public void onCreate(SQLiteDatabase db) {
    String CREATE_FEEDS_TABLE = "CREATE TABLE " + TABLE_FEEDS + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," + KEY_URL + " TEXT" + ")";
    db.execSQL(CREATE_FEEDS_TABLE);
  }

  // Upgrading database
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEEDS);
 
    onCreate(db);
  }
 
  /**
   * All CRUD(Create, Read, Update, Delete) Operations
   */
 
  public void addFeed(Feed feed) {
    SQLiteDatabase db = this.getWritableDatabase(); 
    ContentValues values = new ContentValues();
    values.put(KEY_NAME, feed.getName()); 
    values.put(KEY_URL, feed.getRssUrl()); 
 
    db.insert(TABLE_FEEDS, null, values);
    db.close();
  }

  // This is where the bug is 
  public Feed getFeed(String name) {
    SQLiteDatabase db = this.getWritableDatabase();
    Cursor cursor = db.query(TABLE_FEEDS, new String[] { KEY_ID,KEY_NAME, KEY_URL }, KEY_NAME + "=?", new String[] { String.valueOf(name) }, null, null, null, null);
    if (cursor.getCount() != 0) {
      cursor.moveToFirst();
      Feed feed = new Feed();
      feed.setID(Integer.parseInt(cursor.getString(0))); 
      feed.setName(cursor.getString(1));
      feed.setRssUrl(cursor.getString(2));
      return feed;
    } else {
      return new Feed();
    }
  } 

 
  public ArrayList<Feed> getAllFeeds() {
    ArrayList<Feed> feedList = new ArrayList<Feed>();
    String selectQuery = "SELECT  * FROM " + TABLE_FEEDS + " ORDER BY " + KEY_NAME + " COLLATE NOCASE";
    SQLiteDatabase db = this.getWritableDatabase();
    Cursor cursor = db.rawQuery(selectQuery, null);
 
    // looping through all rows and adding to list
    if (cursor.moveToFirst()) {
      do {
        Feed feed = new Feed();
        feed.setID(Integer.parseInt(cursor.getString(0)));
        feed.setName(cursor.getString(1));
        feed.setRssUrl(cursor.getString(2));
        feedList.add(feed);
      } while (cursor.moveToNext());
    }
 
    db.close();
    return feedList;
  }
 
  public int updateFeed(Feed feed) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(KEY_NAME, feed.getName());
    values.put(KEY_URL, feed.getRssUrl());
 
    return db.update(TABLE_FEEDS, values, KEY_ID + " = ?", new String[] { String.valueOf(feed.getID()) });
  }
 
  public void deleteFeed(Feed feed) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_FEEDS, KEY_NAME + " = ?", new String[] { String.valueOf(feed.getName()) });
    db.close();
  }
 
  public int getFeedsCount() {
    String countQuery = "SELECT  * FROM " + TABLE_FEEDS;
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(countQuery, null);
    cursor.close();
 
    return cursor.getCount();
  }
 
}
