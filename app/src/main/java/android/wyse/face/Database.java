package android.wyse.face;

/**
 * Created by sonu on 01/12/16.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "cara.db";
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String TABLE_USERS = "users";
    public static final String USER_ID = "userid";
    public static final String MARK_FLAG = "InOutStatus";
    public static final String DATE_TIME = "datetime";
    public static final String UPLOAD_STATUS = "upload_status";
    public static final String USER_NAME = "name";
    public static final String MATCH_SCORE = "matchScore";
    public static final String LAST_KNOWN_TEMP = "temperature";
    public static final String LAST_STATUS = "laststatus";
    public static final String LAST_DATETIME = "lastdatetime";
    public Database(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("create table " + TABLE_ATTENDANCE + "(id integer primary key, " + USER_ID +
                " text," + MARK_FLAG + " text," + DATE_TIME + " text,timeStamp text,"+LAST_KNOWN_TEMP+" " +
                "text,"+MATCH_SCORE+" text,"+UPLOAD_STATUS+" text)");

        db.execSQL("create table " + TABLE_USERS + "(id integer primary key,"+USER_ID+" text,"+USER_NAME+" text, "+LAST_STATUS+" text,"+LAST_DATETIME+" text)" );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

    public void onUpgradeDb(){
        try {
           // Log.d("CaraDb","onUpgrade");
            SQLiteDatabase db = this.getWritableDatabase();
            //added to support older database versions
            Cursor crs=getAllUsers();
            ArrayList<UserDataModel> users=new ArrayList<>();
            while (crs.moveToFirst()){
                users.add(new UserDataModel(crs.getString(crs.getColumnIndex(USER_ID)),crs.getString(crs.getColumnIndex(USER_NAME))));
            }
            crs.close();
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("create table " + TABLE_USERS + "(id integer primary key,"+USER_ID+" text,"+USER_NAME+" text, "+LAST_STATUS+" text,"+LAST_DATETIME+" text)" );

            for (UserDataModel umodel:users) {
                insertUsers(umodel.getUserid(),umodel.getName());
            }
            users.clear();
        }catch (Exception er){
           // er.printStackTrace();
        }
    }

    public boolean remUserByIdDb(String userid){
        int result=0;
        if (isUserExits(userid)){
            SQLiteDatabase db = this.getWritableDatabase();
            // Log.d("database", "deleteContact");
            result=db.delete(TABLE_USERS, USER_ID+" =?", new String[]{userid});
            db.close();
        }
        return result>0;
    }

    public boolean isUserExits(String userid){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean isPresent=db.rawQuery("SELECT "+USER_ID+" FROM "+TABLE_USERS+" where "+USER_ID+"=?", new String[]{userid}).moveToFirst();
        db.close();
        return isPresent;
    }

    public boolean insertUsers(String userId,String userName){
        if (!isUserExits(userId)) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(USER_ID, userId);
            contentValues.put(USER_NAME, userName);
            contentValues.put(LAST_STATUS, "0");
            contentValues.put(LAST_DATETIME, "0");
            long result = db.insert(TABLE_USERS, null, contentValues);
            Log.d("TAG","User added into cara db");
            return result > 0;
        }else{
            Log.d("TAG","User present in cara db");
            return false;
        }
    }


    /**
     *
     * @param userId - empid of registered user
     * @param key - table col index
     * @param value - value or data to update for key
     * @return
     */
    public boolean updateUser(String userId,String key,String value){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(key, value);
        db.update(TABLE_USERS, contentValues, USER_ID+" ="+userId, null);
        db.close();
        return true;
    }

    /**
     *
     * @param resetLimit - reset user table by limit in miliseconds
     * @return boolean
     */
    public boolean resetStatus(String resetLimit){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LAST_STATUS, "0");
        int cnt=db.update(TABLE_USERS, contentValues,"lastdatetime < ? ", new String[]{resetLimit});
        db.close();
        return cnt>=0;
    }

    /**
     *
     * @param userid - emp id
     * @return Cursor of row
     */
    public Cursor getLastKnownTemp(String userid){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select "+USER_ID+","+LAST_KNOWN_TEMP+",timeStamp from "+TABLE_ATTENDANCE+" where "+USER_ID+"="+userid+" ORDER BY timeStamp DESC LIMIT 1",null);
    }

    /**
     *
     * @param userid - get in out status of employee from user table
     * @return database row cursor
     */
    public Cursor getInOutStatus(String userid) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select "+USER_ID+","+LAST_STATUS+","+LAST_DATETIME+" from "+TABLE_USERS+"" +
                " where userid="+userid,null);
    }

    /**
     *  insertAttendance
     * @param userId - employee id
     * @param markFlag - in or out status of punch
     * @param dateTime - date and time at which record to store
     * @param status - upload status of the record
     * @param temp - temperature of the user from sensor
     * @param matchScore - match score for this record
     * @param timeStamp - unix time stamp
     * @return - boolean value true for success
     */
    public boolean insertAttendance(String userId, String markFlag,
                                    String dateTime,String status,
                                    String temp, String matchScore,
                                    String timeStamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(USER_ID, userId);
        contentValues.put(MARK_FLAG, markFlag);
        contentValues.put(DATE_TIME, dateTime);
        contentValues.put("timeStamp",timeStamp);
        contentValues.put(LAST_KNOWN_TEMP,temp);
        contentValues.put(MATCH_SCORE,matchScore);
        contentValues.put(UPLOAD_STATUS, status);
        long result = db.insert(TABLE_ATTENDANCE, null, contentValues);
        db.close();
        return result > 0;
    }

    public Cursor getData(String field_value,String table,String field_name) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + table + " where " + field_name + "=" + field_value, null);
    }


    public Cursor getOfflinePunches() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_ATTENDANCE+" where "+UPLOAD_STATUS+"=0 ORDER BY id DESC LIMIT 500 ", null);
    }


    public Cursor getAllPunches() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_ATTENDANCE+" where 1", null);
    }

    /**
     * Get device records from provided lowerlimit
     * @param lowerLimit - time in miliseconds
     * @return
     */
    public Cursor getAllPunches(String lowerLimit) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_ATTENDANCE+" where timeStamp>=? ORDER BY id DESC", new String[]{lowerLimit});
    }

    public synchronized Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_USERS+" where 1", null);
    }


    public int numberOfRows(String dataBaseName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, dataBaseName);
        db.close();
        return numRows;
    }

    /**
     *
     * @param id - auto increment id
     * @param status - online or offline status
     * @return
     */
    public boolean updateAttendance(int id,String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(UPLOAD_STATUS,status);
        int re = db.update(TABLE_ATTENDANCE, contentValues, "id = ? ", new String[]{Integer.toString(id)});
        db.close();
        return re > 0;
    }

    /**
     *
     * @param txid  - timestamp of the record
     * @param status - online or offline status of record
     * @return
     */
    public boolean updateAttendance(final String  txid,final String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(UPLOAD_STATUS,status);
        int re = db.update(TABLE_ATTENDANCE, contentValues, "timeStamp = ? ", new String[]{txid});
        db.close();
        return re > 0;
    }

    /**
     * Get offline pending records count
     * @return
     */
    public int getPendingRecords(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_ATTENDANCE+" where "+UPLOAD_STATUS+"=0", null);
        int count=res.getCount();
        res.close();
        db.close();
        return  count;
    }


    /**
     * Get all attendance records count
     * @return
     */
    public int getDeviceRecords(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_ATTENDANCE+" where 1", null);
        int count=res.getCount();
        res.close();
        return  count;
    }

    /**
     *
     * @param lowerLimit - timestamp
     */
    public void flushRecordsByLimit(String lowerLimit) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Log.d("database", "deleteContact");
        db.delete(TABLE_ATTENDANCE, "timestamp < ? and "+UPLOAD_STATUS+"=1", new String[]{lowerLimit});
    }


    public int deletePunch(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
       // Log.d("database", "deleteContact");
        return db.delete(TABLE_ATTENDANCE,
                "user_id = ? ",
                new String[]{Integer.toString(id)});
    }

    public int deletePunchBelow(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Log.d("database", "deleteContact");
        return db.delete(TABLE_ATTENDANCE,
                "user_id<=? ",
                new String[]{Integer.toString(id)});
    }

    public int flushAllRecords() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Log.d("database", "deleteContact");
        return db.delete(TABLE_ATTENDANCE, "1",null);
    }

    public int flushUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Log.d("database", "deleteContact");
        return db.delete(TABLE_USERS, "1",null);
    }


}

