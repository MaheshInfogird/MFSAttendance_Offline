package com.hrgirdattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by infogird47 on 08/07/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper
{
    // Database Version
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "attendance_offline_db_";

    //table name
    private static final String TABLE_User_Details = "User_Details";
    private static final String TABLE_SignINOut = "Att_SignINOut_Details";

    //Columns names
    private static final String KEY_ID = "User_ID";
    private static final String KEY_FirstNAME = "User_FirstName";
    private static final String KEY_LastNAME = "User_LastName";
    private static final String KEY_PH_NO = "U_Mobile_Number";
    private static final String KEY_CID = "CId";
    private static final String KEY_AttType = "attType";
    private static final String KEY_Thumb1 = "Thumb1";
    private static final String KEY_Thumb2 = "Thumb2";
    private static final String KEY_Thumb3 = "Thumb3";
    private static final String KEY_Thumb4 = "Thumb4";
    private static final String KEY_shift = "shift";

    private static final String KEY_PrimaryKey1 = "Primary_key1";
    private static final String KEY_PrimaryKey = "Primary_key";
    private static final String KEY_UserId = "UserId";
    private static final String KEY_Date_Time = "Date_Time";
    private static final String KEY_Sign_InOut = "SignInOut";

    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String CREATE_UserDetails_TABLE = "CREATE TABLE " + TABLE_User_Details + "("
                + KEY_PrimaryKey1 + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_ID + " TEXT," + KEY_FirstNAME + " TEXT,"
                + KEY_LastNAME + " TEXT,"+ KEY_PH_NO + " TEXT,"
                + KEY_CID + " TEXT," + KEY_AttType + " TEXT,"
                + KEY_Thumb1 + " TEXT," + KEY_Thumb2 + " TEXT,"
                + KEY_Thumb3 + " TEXT," + KEY_Thumb4 + " TEXT," + KEY_shift + " TEXT" + ")";

        db.execSQL(CREATE_UserDetails_TABLE);
        Log.i("create_table", CREATE_UserDetails_TABLE);
        Log.i("data","table user details created");

        String CREATE_SignInOut_TABLE = "CREATE TABLE " + TABLE_SignINOut + "("
                + KEY_PrimaryKey + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_UserId + " TEXT," + KEY_Date_Time + " TEXT," +  KEY_Sign_InOut + " TEXT" +")";
        db.execSQL(CREATE_SignInOut_TABLE);
        Log.i("data","table signinout created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_User_Details);
        // Create tables again
        //  onCreate(db);
    }

    public void addContact(UserDetails_Model contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, contact.getUid()); // Contact Name
        values.put(KEY_FirstNAME, contact.getFirstname()); // Contact Name
        values.put(KEY_LastNAME, contact.getLastname());
        values.put(KEY_PH_NO, contact.getMobile_no());
        values.put(KEY_CID, contact.getCid());
        values.put(KEY_AttType, contact.getAttType());
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());// Contact Phone Number
        values.put(KEY_shift, contact.getShift());

        db.insert(TABLE_User_Details, null, values);
        db.close();
    }

    public void UpdateContact(UserDetails_Model contact, String uid)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());// Contact Phone Number

        db.update(TABLE_User_Details, values, KEY_ID+"="+uid, null);
        //db.update(TABLE_User_Details, values, KEY_PH_NO+"="+mob, null);
        db.close();
    }

    public void adddata_signinout(SigninOut_Model contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_UserId, contact.getUserId()); // Contact Name
        values.put(KEY_Date_Time, contact.getDate_Time()); // Contact Name// Contact Phone Number
        values.put(KEY_Sign_InOut, contact.getSignInOutId()); // Contact Name// Contact Phone Number

        db.insert(TABLE_SignINOut, null, values);
        db.close();
    }

    public SigninOut_Model checkdata_signinout(String uid)
    {
        String selectQuery = "SELECT  * FROM " + TABLE_SignINOut + " WHERE UserId= "+uid+"";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        SigninOut_Model contact = new SigninOut_Model();

        if(cursor.getCount() ==0)
        {
            Log.i("Empty","Table EMpty");
        }
        else
        {
            cursor.moveToLast();
            contact.setPrimaryKey(cursor.getString(0));
            contact.setUserId(cursor.getString(1));
            contact.setDate_Time(cursor.getString(2));
            contact.setSignInOutId(cursor.getString(3));

            Log.i("setPrimaryKey",cursor.getString(0));
            Log.i("setUserId",cursor.getString(1));
            Log.i("setDate_Time",cursor.getString(2));
            Log.i("setSignInOut",cursor.getString(3));
        }
        return contact;
    }

    public SigninOut_Model checkdata()
    {
        String selectQuery = "SELECT  * FROM " + TABLE_SignINOut;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        SigninOut_Model contact = new SigninOut_Model();

        if(cursor.getCount() == 0)
        {
            Log.i("Empty","Table EMpty");
        }
        else
        {
            cursor.moveToLast();
            Log.i("setUserId",cursor.getString(0));
            Log.i("setDate_Time",cursor.getString(1));
            Log.i("setUserId",cursor.getString(2));
            Log.i("setDate_Time",cursor.getString(3));
        }
        return contact;
    }

    public List<SigninOut_Model> getSigninoutData(int key)
    {
        List<SigninOut_Model> contactList = new ArrayList<SigninOut_Model>();
        String selectQuery = "SELECT  * FROM " + TABLE_SignINOut+ " WHERE Primary_key > "+key+"";
        Log.i("selectQuery", ""+selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() != 0)
        {
            cursor.moveToFirst();
            do
            {
                SigninOut_Model contact = new SigninOut_Model();
                contact.setPrimaryKey(cursor.getString(0));
                contact.setUserId(cursor.getString(1));
                contact.setDate_Time(cursor.getString(2));
                contact.setSignInOutId(cursor.getString(3));

                contactList.add(contact);
            } while (cursor.moveToNext());
        }
        return contactList;
    }

    public List<UserDetails_Model> getAllContacts()
    {
        List<UserDetails_Model> contactList = new ArrayList<UserDetails_Model>();
        String selectQuery = "SELECT  * FROM " + TABLE_User_Details;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null)
        {
            if (cursor.moveToFirst())
            {
                do {
                    UserDetails_Model contact = new UserDetails_Model();
                    contact.setPrimaryKey(cursor.getString(0));
                    contact.setUid(cursor.getString(1));
                    contact.setFirstname(cursor.getString(2));
                    contact.setLastname(cursor.getString(3));
                    contact.setMobile_no(cursor.getString(4));
                    contact.setCid(cursor.getString(5));
                    contact.setAttType(cursor.getString(6));
                    contact.setThumb1(cursor.getString(7));
                    contact.setThumb2(cursor.getString(8));
                    contact.setThumb3(cursor.getString(9));
                    contact.setThumb4(cursor.getString(10));
                    contact.setShift(cursor.getString(11));

                    contactList.add(contact);
                } while (cursor.moveToNext());
            }
        }
       /* for(int i = 0;i<contactList.size();i++){
            Log.i("ALL_USER_DATA \n SHIFT ",contactList.get(i).getShift()+"\n AttType"+contactList.get(i).getAttType()+"\n UID "+contactList.get(i).getUid());
        }*/
        return contactList;
    }


    public boolean checkEmpId(String uId)
    {
        String sql = "SELECT "+ KEY_ID+" FROM "+TABLE_User_Details+" WHERE "+ KEY_ID+"="+uId;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(sql,null);

        if(cursor.getCount() > 0)
        {
            cursor.close();
            return true;
        }
        else
        {
            cursor.close();
            return false;
        }
    }

    public int updateContact(UserDetails_Model contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, contact.getUid());
        values.put(KEY_FirstNAME, contact.getFirstname());
        values.put(KEY_LastNAME, contact.getLastname());
        values.put(KEY_PH_NO, contact.getMobile_no());
        values.put(KEY_CID, contact.getCid());
        values.put(KEY_AttType, contact.getAttType());
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());
        values.put(KEY_shift, contact.getShift());

        return db.update(TABLE_User_Details, values, KEY_ID + " = ?",
                new String[] { String.valueOf(contact.getUid()) });
    }

    public void UpdateContactAttType(UserDetails_Model contact, String uid)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());
        values.put(KEY_AttType, contact.getAttType());
        values.put(KEY_shift, contact.getShift());

        db.update(TABLE_User_Details, values, KEY_ID+"="+uid, null);
        db.close();
    }

    public void deleteContact(String uId)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from "+ TABLE_User_Details +"where" + KEY_PH_NO +"="+mob);
        db.execSQL("DELETE FROM " + TABLE_User_Details + " WHERE " + KEY_ID + "= '" + uId + "'");
        //db.delete(TABLE_User_Details, KEY_PH_NO + "=" + mob, null);
        db.close();
    }

    public void delete_record()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from "+ TABLE_User_Details);
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_User_Details);

        String CREATE_UserDetails_TABLE = "CREATE TABLE " + TABLE_User_Details + "("
                + KEY_PrimaryKey1 + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_ID + " TEXT," + KEY_FirstNAME + " TEXT,"
                + KEY_LastNAME + " TEXT,"+ KEY_PH_NO + " TEXT,"
                + KEY_CID + " TEXT," + KEY_AttType + " TEXT,"
                + KEY_Thumb1 + " TEXT," + KEY_Thumb2 + " TEXT,"
                + KEY_Thumb3 + " TEXT," + KEY_Thumb4 + " TEXT," + KEY_shift + " TEXT" + ")";
        db.execSQL(CREATE_UserDetails_TABLE);
        Log.i("data","table userdetails created");
    }

    public void delete_attendance_record()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_SignINOut);
        String CREATE_SignInOut_TABLE = "CREATE TABLE " + TABLE_SignINOut + "("
                + KEY_PrimaryKey + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_UserId + " TEXT," + KEY_Date_Time + " TEXT," +  KEY_Sign_InOut + " TEXT" +")";
        db.execSQL(CREATE_SignInOut_TABLE);

        //DELETE FROM sessions WHERE timestamp >= '2010-12-06' AND timestamp < '2010-12-07'
    }

    public void delete_prev_att_record(String preDate, String date)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_SignINOut +" where "+ KEY_Date_Time +" < "+date);
        //db.execSQL("DELETE FROM "+ TABLE_SignINOut +" WHERE "+KEY_Date_Time+" NOT BETWEEN "+ preDate +" AND "+date);

        //DELETE FROM sessions WHERE timestamp >= '2010-12-06' AND timestamp < '2010-12-07'
    }

    public void delete_3daysE_record()
    {
        List<SigninOut_Model> contactList = new ArrayList<SigninOut_Model>();
        String selectQuery = "SELECT  * FROM " + TABLE_SignINOut;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        System.out.println("Date = "+ cal.getTime());

        Date newDate = cal.getTime();

        String timeStamp = sdf.format(newDate);
        Log.i("timeStamp", timeStamp);

        if (cursor.moveToFirst())
        {
            do {
                String dt = cursor.getString(2);
                Log.i("dt", dt);
                Date date_var = new Date(),before3days = new Date();

                try
                {
                    date_var = sdf.parse(dt);
                    Log.i("date_var", timeStamp);
                    before3days = sdf.parse(timeStamp);
                    Log.i("before3days", timeStamp);
                    String newDateString = sdf.format(date_var);
                    String newDateStringn = sdf.format(date_var);

                    if(date_var.equals(before3days) || date_var.before(before3days))
                    {
                        SQLiteDatabase dbn = this.getWritableDatabase();
                        dbn.execSQL("delete from "+ TABLE_SignINOut +" where "+KEY_Date_Time+" = '"+newDateStringn+"'");
                        Log.i("delete_query", "delete from "+ TABLE_SignINOut +" where "+KEY_Date_Time+" = '"+newDateStringn+"'");
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());
        }
    }

    public UserDetails_Model check_userData()
    {
        String selectQuery = "SELECT * FROM " +TABLE_User_Details+" ORDER BY "+  KEY_ID +" DESC LIMIT 1";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        UserDetails_Model contact = new UserDetails_Model();

        if(cursor.getCount() ==0){
            Log.i("Empty","Table Empty");
        }
        else {
            cursor.moveToFirst();
            contact.setUid(cursor.getString(1));
            Log.i("uid_db",cursor.getString(1));
        }
        return contact;
    }
}
