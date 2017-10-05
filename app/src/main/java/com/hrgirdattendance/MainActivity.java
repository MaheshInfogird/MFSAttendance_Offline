package com.hrgirdattendance;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity
{
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    public static final String MyPREFERENCES_InOutKey = "MyPrefs_Key" ;
    public static final String MyPREFERENCES_prefix = "MyPrefs_prefix" ;
    public static final String MyPREFERENCES_devName = "MyPrefs_devName" ;
    SharedPreferences pref, shared_pref, key_pref, pref_prefix, pref_dev;
    SharedPreferences.Editor key_editor, editor_prefix, editor_dev;
    
    Button btn_attendance, btn_registration, btn_resetThumb, btn_syncData, btn_getData;
    CheckInternetConnection internetConnection;
    UserSessionManager session;
    ConnectionDetector cd;
    ProgressDialog progressDialog;
    DatabaseHandler db;
    public static NetworkChange receiver;
    GPSTracker gps;
    
    String response_version, myJson1, Url;
    String Packagename;
    String url_http, logo, myJson2;
    String pk;
    String UserName, Password;
    String android_id;
    String PrimaryKey, InOutId, EmpId, DateTime;
    String empattDid, flag, offline_flag = "";
    String get_prefix,responseCode;
    String response, response_att, myJson,outid;
    String Current_Location;
    String TabID;

    int Prev_Key, prev_key;
    int version_code;

    boolean send_data = false;
    boolean get_data = false;
    
    ImageView main_logo;
    ImageButton app_destroy;
    LinearLayout progress_layout;
    EditText ed_userName, ed_password;
    Button btn_login, btn_Cancel;
    PopupWindow pw;
    View view;

    ArrayList<String> date_array = new ArrayList<String>();
    ArrayList<String> inout_array = new ArrayList<String>();
    ArrayList<String> key_array = new ArrayList<String>();
    ArrayList<String> id_array = new ArrayList<String>();
    ArrayList<String> empattDid_arr = new ArrayList<String>();
    ArrayList<String> uid_array = new ArrayList<String>();

    Double latitude = 0.0, longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.changeProtocol();
        db = new DatabaseHandler(this);

        key_pref = getApplicationContext().getSharedPreferences(MyPREFERENCES_InOutKey, MODE_PRIVATE);
        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        pref_prefix = getSharedPreferences(MyPREFERENCES_prefix, MODE_PRIVATE);
        pref_dev = getSharedPreferences(MyPREFERENCES_devName, MODE_PRIVATE);

        Prev_Key = key_pref.getInt("key",0);
        Log.i("Prev_Key", ""+Prev_Key);

        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));

        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version_code = info.versionCode;
            Packagename = info.packageName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        receiver = new NetworkChange()
        {
            @Override
            protected void onNetworkChange()
            {
                if (receiver.isConnected)
                {
                    Log.i("Connected","Connected");
                    if (send_data)
                    {
                        Log.i("flag_send_data","flag_send_data");
                        outid = "3";
                        upload_Data();
                    }
                }
                else
                {
                    Log.i("Not Connected","Not Connected");
                    Toast.makeText(MainActivity.this, "You are Offline", Toast.LENGTH_SHORT).show();
                }
            }
        };

        Initialization();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        else
        {
            gps = new GPSTracker(getApplicationContext(), MainActivity.this);
            if (gps.canGetLocation())
            {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                Current_Location = gps.getlocation_Address();
            }

            if (internetConnection.hasConnection(MainActivity.this))
            {
                flag = "1";
                empattDid = "";
                //offline_flag = "";
                getUserDataNew();
            }
        }
    }

    public void Initialization()
    {
        btn_attendance = (Button)findViewById(R.id.btn_attendance);
        btn_registration = (Button)findViewById(R.id.btn_registration);
        btn_resetThumb = (Button)findViewById(R.id.btn_resetThumb);
        btn_syncData = (Button)findViewById(R.id.btn_syncData);
        btn_getData = (Button)findViewById(R.id.btn_getUserData);
        app_destroy = (ImageButton)findViewById(R.id.app_destroy);

        main_logo = (ImageView)findViewById(R.id.main_logo);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout_main);

        Picasso.with(MainActivity.this).load(logo).into(main_logo);

        app_destroy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setMessage("Do you want to close app?");
                alertDialog.setCancelable(false);
                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        outid = "2";
                        popup_window(v);
                        //android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });
        //getCheckVersion();

        main_logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setMessage("Do you want to Change Url?");
                alertDialog.setCancelable(false);
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        outid = "1";
                        popup_window(v);
                        //android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
                alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });

        btn_attendance.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btn_attendance.setBackgroundResource(R.drawable.attendance_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.admin_button);
                btn_getData.setBackgroundResource(R.drawable.admin_button);
                btn_syncData.setBackgroundResource(R.drawable.admin_button);

                btn_attendance.setTextColor(getResources().getColor(R.color.RedTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_getData.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_syncData.setTextColor(getResources().getColor(R.color.WhiteTextColor));

                Intent intent = new Intent(MainActivity.this, AttendanceActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.attendance_button);
                btn_getData.setBackgroundResource(R.drawable.admin_button);
                btn_syncData.setBackgroundResource(R.drawable.admin_button);

                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.RedTextColor));
                btn_getData.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_syncData.setTextColor(getResources().getColor(R.color.WhiteTextColor));

                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (session.isUserLoggedIn())
                    {
                        Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                        intent.putExtra("login_id", "1");
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                        intent.putExtra("login_id", "1");
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_resetThumb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.attendance_button);
                btn_getData.setBackgroundResource(R.drawable.admin_button);
                btn_syncData.setBackgroundResource(R.drawable.admin_button);

                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.RedTextColor));
                btn_getData.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_syncData.setTextColor(getResources().getColor(R.color.WhiteTextColor));

                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (session.isUserLoggedIn())
                    {
                        Intent intent = new Intent(MainActivity.this, ResetThumbActivity.class);
                        intent.putExtra("login_id", "2");
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                        intent.putExtra("login_id", "2");
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_getData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View v)
            {
                get_data = true;
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.admin_button);
                btn_getData.setBackgroundResource(R.drawable.attendance_button);
                btn_syncData.setBackgroundResource(R.drawable.admin_button);

                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_syncData.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_getData.setTextColor(getResources().getColor(R.color.RedTextColor));

                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    alertDialog.setMessage("Do you want to update user data?");
                    alertDialog.setCancelable(false);
                    alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            flag = "1";
                            //getUserData();
                            //getUserDataNew();
                            outid = "4";
                            popup_window(v);
                            //android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });
                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    alertDialog.show();

                    /*db.deleteAllEmpRecord();
                    pk = "0";
                    getUserData();*/

                    /*List<UserDetails_Model> contacts = db.getAllEmpData();
                    Log.i("MFS_Log contacts", "" + contacts);

                    if (contacts.isEmpty())
                    {
                        Log.i("MFS_Log if", "if");
                        pk = "0";
                        getUserData();
                    }
                    else
                    {
                        Log.i("MFS_Log else", "else");
                        for (UserDetails_Model cn : contacts)
                        {
                            String log = "PrimaryKey: "+cn.getPrimaryKey()+",uId: "+cn.getUid()+",cId: "+cn.getCid()+", Type: "+cn.getAttType()+" ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();
                            Log.i("Name: ", log);
                            String uid = cn.getUid();
                            uid_array.add(uid);
                        }

                        pk = uid_array.toString();
                        pk = pk.substring(1, pk.length() - 1);

                    *//*UserDetails_Model user_uid = db.check_userData();
                    Log.i("user_uid", ""+user_uid.getUid());
                    pk = user_uid.getUid();
                    Log.i("pk ==  ", pk);*//*
                        getUserData();
                    }*/
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        view = this.getCurrentFocus();

        btn_syncData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.admin_button);
                btn_getData.setBackgroundResource(R.drawable.admin_button);
                btn_syncData.setBackgroundResource(R.drawable.attendance_button);

                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_syncData.setTextColor(getResources().getColor(R.color.RedTextColor));
                btn_getData.setTextColor(getResources().getColor(R.color.WhiteTextColor));

                if (internetConnection.hasConnection(getApplicationContext())) {
                    outid = "3";
                    upload_Data();
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void delete_prevAttRecord()
    {
        SigninOut_Model temp_sm_signinout =  db.checkdata();

        if(temp_sm_signinout != null)
        {
            List<SigninOut_Model> contacts = db.getSigninoutData(0);
            Log.i("MFS_Log contacts", "" + contacts);

            if (!contacts.isEmpty())
            {
                for (SigninOut_Model cn : contacts)
                {
                    String date_data = "primary_key "+cn.getPrimaryKey()+", User_Id"+cn.getUserId()+", Date"+cn.getDate_Time();
                    Log.i("date_data_before", date_data);
                    //db.delete_prev_att_record(date_data, date);
                }

                db.deletePrev3DaysRecord();
                Log.i("MFS data", "data deleted");

                for (SigninOut_Model cn : contacts)
                {
                    String date_data = "primary_key "+cn.getPrimaryKey()+", User_Id"+cn.getUserId()+", Date"+cn.getDate_Time();
                    Log.i("date_data_after", date_data);
                }
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("requestCode",""+requestCode);//1

        switch (requestCode)
        {
            case 1:
            {
                Log.i("grantResults",""+grantResults.length);
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                {
                    gps = new GPSTracker(getApplicationContext(), MainActivity.this);

                    if (gps.canGetLocation())
                    {
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        Current_Location = gps.getlocation_Address();
                    }

                    if (internetConnection.hasConnection(MainActivity.this))
                    {
                        flag = "1";
                        empattDid = "";
                        //offline_flag = "";
                        getUserDataNew();
                    }
                }

                return;
            }
        }
    }

    public void upload_Data()
    {
        if (internetConnection.hasConnection(getApplicationContext()))
        {
            SigninOut_Model temp_sm_signinout =  db.checkdata();

            if(temp_sm_signinout != null)
            {
                Prev_Key = key_pref.getInt("key",0);
                Log.i("PrevKey_pref", ""+Prev_Key);
                List<SigninOut_Model> contacts = db.getSigninoutData(Prev_Key);
                Log.i("MFS_Log contacts", "" + contacts);

                if (!contacts.isEmpty())
                {
                    date_array.clear();
                    inout_array.clear();
                    id_array.clear();
                    key_array.clear();

                    int kkeay = 0;
                    for (SigninOut_Model cn : contacts)
                    {
                        String primarykey_data = cn.getPrimaryKey();
                        String date_data = cn.getDate_Time();
                        String in_out_data = cn.getSignInOutId();
                        String id_data = cn.getUserId();
                        Log.i("MFS_Log primarykey", primarykey_data);
                        Log.i("MFS_Log id", id_data);
                        Log.i("MFS_Log date", date_data);
                        Log.i("MFS_Log in_out", in_out_data);

                        Log.i("Prev_Key1", ""+Prev_Key);
                        //if (kkeay > Prev_Key)
                        //{
                        date_array.add(date_data);
                        inout_array.add(in_out_data);
                        id_array.add(id_data);
                        key_array.add(primarykey_data);
                        //}
                    }

                    if (date_array.isEmpty())
                    {
                        if (outid.equals("1"))
                        {
                            Toast.makeText(MainActivity.this, "No Attendance Records Found", Toast.LENGTH_SHORT).show();
                            session.logout_url();
                            key_editor = key_pref.edit();
                            key_editor.clear();
                            key_editor.commit();
                            db.delete_attendance_record();
                            Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else if (outid.equals("3"))
                        {
                            Toast.makeText(MainActivity.this, "No Attendance Records Found", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        PrimaryKey = key_array.toString();
                        EmpId = id_array.toString();
                        DateTime = date_array.toString();
                        InOutId = inout_array.toString();

                        PrimaryKey = PrimaryKey.substring(1, PrimaryKey.length() - 1);
                        EmpId = EmpId.substring(1, EmpId.length() - 1);
                        DateTime = DateTime.substring(1, DateTime.length() - 1);
                        InOutId = InOutId.substring(1, InOutId.length() - 1);

                        PrimaryKey = PrimaryKey.replace(", ", ",");
                        EmpId = EmpId.replace(", ", ",");
                        DateTime = DateTime.replace(", ", ",");
                        InOutId = InOutId.replace(", ", ",");

                        String url = "" + url_http + "" + Url + "/owner/hrmapi/offlinemakeattendancehitm?";
                        //String url = "" + url_http + "" + Url + "/owner/hrmapi/newofflinemakeattendancehitm?";
                        Log.i("url", url);
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("primarykey", PrimaryKey);
                        map.put("empId", EmpId);
                        map.put("datetime", DateTime);
                        map.put("signId", InOutId);

                        postData(url, map);

                        /*progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait", "Uploading Data... ", true);

                        new Thread(new Runnable() {
                            public void run() {
                                //sendSignInOutData();
                                String url = "" + url_http + "" + Url + "/owner/hrmapi/offlinemakeattendancehitm?";
                                Log.i("url", url);
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("primarykey", PrimaryKey);
                                map.put("empId", EmpId);
                                map.put("datetime", DateTime);
                                map.put("signId", InOutId);

                                postData(url, map);
                                //performPostCall(url, map);
                                //GetJSONData();

                            }
                        }).start();*/
                    }
                }
                else
                {
                    if (outid.equals("1"))
                    {
                        session.logout_url();
                        key_editor = key_pref.edit();
                        key_editor.clear();
                        key_editor.commit();

                        editor_dev = pref_dev.edit();
                        editor_dev.clear();
                        editor_dev.commit();

                        db.delete_attendance_record();
                        db.deleteAllEmpRecord();
                        Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else if (outid.equals("3"))
                    {
                        Toast.makeText(MainActivity.this, "No Attendance Records Found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else
            {
                if (outid.equals("1"))
                {
                    session.logout_url();
                    key_editor = key_pref.edit();
                    key_editor.clear();
                    key_editor.commit();

                    editor_dev = pref_dev.edit();
                    editor_dev.clear();
                    editor_dev.commit();

                    db.delete_attendance_record();
                    db.deleteAllEmpRecord();
                    Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                    startActivity(intent);
                    finish();
                }
                //Toast.makeText(MainActivity.this, "No Records Found", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
        }
    }

    public void popup_window(View v)
    {
        try
        {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.destroy_app_login, (ViewGroup) findViewById(R.id.destroy_login_layout));

            pw = new PopupWindow(layout, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            pw.setWidth(width-40);
            pw.showAtLocation(v, Gravity.CENTER, 0, 0);

            dimBehind(pw);

            ed_userName = (EditText)layout.findViewById(R.id.ed_userName_dest);
            ed_password = (EditText)layout.findViewById(R.id.ed_password_dest);
            btn_login = (Button) layout.findViewById(R.id.btn_signIn_dest);
            btn_Cancel = (Button) layout.findViewById(R.id.btn_Cancel_dest);

            if (outid.equals("1"))
            {
                btn_login.setText("Change");
            }
            else if (outid.equals("4"))
            {
                btn_login.setText("Update");
            }
            else {
                btn_login.setText("Close App");
            }

            btn_Cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pw.dismiss();
                }
            });

            btn_login.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    pw.dismiss();
                    UserName = ed_userName.getText().toString();
                    Password = ed_password.getText().toString();
                    if (internetConnection.hasConnection(getApplicationContext()))
                    {
                        if (UserName.equals("") && Password.equals(""))
                        {
                            Toast.makeText(MainActivity.this, "Please enter username & password", Toast.LENGTH_LONG).show();
                        }
                        else if (UserName.equals(""))
                        {
                            Toast.makeText(MainActivity.this, "Please enter username", Toast.LENGTH_LONG).show();
                        }
                        else if (Password.equals(""))
                        {
                            Toast.makeText(MainActivity.this, "Please enter password", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            in.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                            //in.hideSoftInputFromWindow(view.getWindowToken(), 0);

                            signIn();
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dimBehind(PopupWindow popupWindow)
    {
        View container;
        if (popupWindow.getBackground() == null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                container = (View) popupWindow.getContentView().getParent();
            }
            else
            {
                container = popupWindow.getContentView();
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                container = (View) popupWindow.getContentView().getParent().getParent();
            }
            else
            {
                container = (View) popupWindow.getContentView().getParent();
            }
        }

        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.4f;
        wm.updateViewLayout(container, p);
    }

    public void signIn()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            ProgressDialog progressDialog;
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = new ProgressDialog(MainActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Please wait...");
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/signInwithdeviceidoffline/?";

                    String query = String.format("email=%s&password=%s&android_devide_id=%s&devicelocation=%s&signinby=%s&logoutflag=%s",
                            URLEncoder.encode(UserName, "UTF-8"),
                            URLEncoder.encode(Password, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode("", "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"),
                            URLEncoder.encode("2", "UTF-8"));

                    url = new URL(Transurl + query);
                    Log.i("url", "" + url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response += line;
                        }
                    }
                    else {
                        response = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
                    e.printStackTrace();
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                Log.i("response", result);
                if (response.equals("[]"))
                {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);

                        String responsecode = object.getString("responseCode");

                        if (responsecode.equals("1"))
                        {
                            progressDialog.dismiss();

                            //session.createUserLoginSession(UserName, Password);

                            if (outid.equals("1"))
                            {
                                editor_prefix = pref_prefix.edit();
                                editor_prefix.clear();
                                editor_prefix.commit();

                                //db.deleteAllEmpRecord();
                                upload_Data();
                            }
                            else if (outid.equals("4"))
                            {
                                //db.deleteAllEmpRecord();
                                pk = "0";
                                flag = "1";
                                //offline_flag = "";
                                //getUserData();
                                getUserDataNew();
                            }
                            else {
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }
                        else
                        {
                            progressDialog.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setCancelable(true);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            });

                            alertDialog.show();
                        }
                    }

                    catch (JSONException e){
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public void getCheckVersion()
    {
        class GetCheckVersion extends AsyncTask<String, Void, String>
        {
            @Override
            protected void onPreExecute() {
                //progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Getting Thumb data...", true);
                //progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/getversion/?";

                    String query3 = String.format("apptype=%s", URLEncoder.encode("4", "UTF-8"));
                    URL url = new URL(leave_url + query3);
                    Log.i("url", ""+ url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response_version = "";
                            response_version += line;
                        }
                    }
                    else
                    {
                        response_version = "";
                    }
                }
                catch (Exception e){
                    Log.e("Exception", e.toString());
                }

                return response_version;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson1 = result;
                    Log.i("myJson", myJson1);

                    if (myJson1.equals("[]"))
                    {
                        //progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(myJson1);
                            //Log.i("jsonArray", "" + jsonArray);

                            JSONObject object = jsonArray.getJSONObject(0);
                            
                            int get_version = object.getInt("Version");
                            Log.i("get_version", ""+get_version);
                            Log.i("version_code", ""+version_code);

                            if (version_code != get_version)
                            {
                                //progressDialog.dismiss();
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setTitle("New Update");
                                alertDialog.setMessage("Please update your app");
                                alertDialog.setCancelable(false);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.hrgirdattendance"));
                                        startActivity(intent);
                                        finish();
                                    }
                                });

                                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(startMain);
                                        finish();
                                    }
                                });

                                alertDialog.show();
                            }
                            else {
                                /*getUserData();
                                getPrefixData();*/

                                flag = "1";
                                //getUserData();
                                //getUserDataNew();

                                uid_array.clear();

                                flag = "1";
                                //offline_flag = "";
                                //getUserData();
                                getUserDataNew();

                                /*List<UserDetails_Model> contacts = db.getAllEmpData();
                                if (contacts.isEmpty())
                                {
                                    pk = "0";
                                    flag = "1";
                                    //getUserData();
                                    getUserDataNew();
                                }
                                else
                                {
                                    for (UserDetails_Model cn : contacts)
                                    {
                                        String log = "PrimaryKey: "+cn.getPrimaryKey()+",uId: "+cn.getUid()+",cId: "+cn.getCid()+", Type: "+cn.getAttType()+" ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();
                                        Log.i("Name: ", log);
                                        String uid = cn.getUid();
                                        uid_array.add(uid);
                                    }

                                    pk = uid_array.toString();
                                    pk = pk.substring(1, pk.length() - 1);
                                    flag = "1";
                                    //getUserData();
                                    getUserDataNew();
                                }*/
                            }
                        }
                        catch (JSONException e) {
                            Log.e("JsonException", e.toString());
                        }
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetCheckVersion getCheckVersion = new GetCheckVersion();
        getCheckVersion.execute();
    }

    public void sendSignInOutData()
    {
        String url = "" + url_http + "" + Url + "/owner/hrmapi/offlinemakeattendancehitm?";
        Log.i("url", url);
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("primarykey", PrimaryKey));
        nameValuePairs.add(new BasicNameValuePair("empId", EmpId));
        nameValuePairs.add(new BasicNameValuePair("datetime", DateTime));
        nameValuePairs.add(new BasicNameValuePair("signId", InOutId));
        Log.i("nameValuePairs", ""+nameValuePairs);

        try
        {
            HttpParams httpParameters = new BasicHttpParams();
// Set the timeout in milliseconds until a connection is established.
            int timeoutConnection = 10000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
// Set the default socket timeout (SO_TIMEOUT)
// in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 10000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

            HttpClient httpClient = new DefaultHttpClient(httpParameters);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpClient.execute(httpPost);
            String st = EntityUtils.toString(response.getEntity());
// Set the default socket timeout (SO_TIMEOUT)
// in milliseconds which is the timeout for waiting for data.
            Log.i("st", "" + st);

            final String Str = "1";
            JSONArray json = new JSONArray(st);
            Log.i("json", "" + json);

//[{"empId":"115","datetime":"2017-07-21 12:21:58","responsecode":1,"signId":"2",
// "primarykey":"5","msg":"Offline Attendance Successfully Done "}]
            JSONObject jsonObject = json.getJSONObject(0);
            Log.i("jsonObject", "" + jsonObject);

            String responsecode = jsonObject.getString("responsecode");
            final String message = jsonObject.getString("msg");
            Log.i("message", "" + message);

            prev_key = jsonObject.getInt("primarykey");
            Log.i("prev_key", "" + prev_key);

            if (responsecode.equals("1"))
            {
                db.deletePrev3DaysRecord();
                //delete_prevAttRecord();

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        progressDialog.dismiss();
                        if (outid.equals("1"))
                        {
                            session.logout_url();
                            key_editor = key_pref.edit();
                            key_editor.clear();
                            key_editor.commit();
                            db.delete_attendance_record();
                            Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            key_editor = key_pref.edit();
                            key_editor.clear();
                            key_editor.putInt("key", prev_key);
                            key_editor.commit();

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setMessage(message);
                            alertDialog.setCancelable(true);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                            alertDialog.show();
                        }
                    }
                });
            }
            else
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        if (outid.equals("1"))
                        {
                            session.logout_url();
                            key_editor = key_pref.edit();
                            key_editor.clear();
                            key_editor.commit();
                            db.delete_attendance_record();
                            Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            key_editor = key_pref.edit();
                            key_editor.clear();
                            key_editor.putInt("key", prev_key);
                            key_editor.commit();
                        }
                    }
                });


            }
            Log.e("pass 1", "connection success ");
        }
        catch (SocketTimeoutException e)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                }
            });
            Log.e("Fail 1", e.toString());
        }
        catch (ConnectTimeoutException e)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                }
            });
            Log.e("Fail 1", e.toString());
        }
        catch (Exception e)
        {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
            Log.e("Fail 1", e.toString());
        }
    }

    public void getUserData()
    {
        class GetUserData extends AsyncTask<String, Void, String>
        {
            String response1;

            @Override
            protected void onPreExecute() {
                progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Getting Thumb data...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    // http://hrsaas.safegird.com/owner/hrmapi/getallempdata
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/getallempdata/?";
                  //  String leave_url = "http://hrsaas.safegird.com/owner/hrmapi/getallempdata";
                    String query3 = String.format("uId=%s", URLEncoder.encode(pk, "UTF-8"));
                    query3 = query3.replace("%2C+", ",");
                    URL url = new URL(leave_url+query3);
                    Log.i("url123", ""+ url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response1 = "";
                            response1 += line;
                        }
                    }
                    else
                    {
                        response1 = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response1;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson2 = result;
                    Log.i("myJson", myJson2);

                    progressDialog.dismiss();
                    if (get_prefix == null)
                    {
                        // Log.i("getPrefixData", "getPrefixData");
                        //Toast.makeText(MainActivity.this, "Data updated successfully", Toast.LENGTH_LONG).show();
                        getPrefixData();
                        outid = "3";
                        upload_Data();
                        send_data = true;
                    }
                    else {
                        //Toast.makeText(MainActivity.this, "Data updated successfully", Toast.LENGTH_LONG).show();
                        if (!get_data)
                        {
                            outid = "3";
                            upload_Data();
                        }

                        send_data = true;
                    }

                    if (myJson2.equals("[]"))
                    {
                        Toast.makeText(MainActivity.this, "No New Record Found", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            Toast.makeText(MainActivity.this, "Data updated successfully", Toast.LENGTH_LONG).show();
                            //  {"uId":8,"firstName":"Abhijeet","lastName":"Paithane",
                            // "cid":"Hrsaas6","mobile":"7066366244","Thumexp":["1Rk1SACAyMAAAAAEOAAABPAFiAMUAxQEAAAAoKICNAOHOAICJAQDRAECYA"]}
                            JSONArray jsonArray = new JSONArray(myJson2);
                            Log.i("jsonArray123", "" + jsonArray);
                            for(int i=0; i <jsonArray.length(); i++)
                            {
                                JSONObject object = jsonArray.getJSONObject(i);
                                Log.i("object123", "" + object);

                                String get_uId = object.getString("uId");
                                Log.i("get_uId",get_uId);
                                String get_firstName = object.getString("firstName");
                                Log.i("get_firstName",get_firstName);
                                String get_lastName = object.getString("lastName");
                                Log.i("get_lastName",get_lastName);
                                String get_cid = object.getString("cid");
                                Log.i("get_cid",get_cid);
                                String get_mobile = object.getString("mobile");
                                Log.i("get_mobile",get_mobile);

                                String get_attType = object.getString("attendancetype");
                                Log.i("get_attType",get_attType);

                                String get_applyshift = object.getString("applyshift");
                                Log.i("get_applyshift", get_applyshift);

                                JSONArray thumbexpr = object.getJSONArray("Thumexp");
                                Log.i("thumbexpr1143",thumbexpr+"");

                                String t1="",t2="",t3="",t4="";

                                for(int j = 0; j < thumbexpr.length(); j++)
                                {
                                    JSONObject object_thumb = thumbexpr.getJSONObject(j);
                                    Log.i("object_thumb", "" + object_thumb);
                                    String get_thumb = object_thumb.getString(j+1+"");
                                    Log.i("get_thumb",get_thumb);

                                    if(j+1 == 1){
                                        t1 = get_thumb;
                                        Log.i("t1",t1);
                                    }
                                    else if(j+1 == 2)
                                    {
                                        t2 = get_thumb;
                                        Log.i("t2",t2);
                                    }
                                    else if(j+1 == 3)
                                    {
                                        t3 = get_thumb;
                                        Log.i("t3",t3);
                                    }
                                    else if(j+1 == 4)
                                    {
                                        t4 = get_thumb;
                                        Log.i("t4",t4);
                                    }
                                    else{
                                        t1 = "";
                                        t2 = "";
                                        t3 = "";
                                        t4 = "";
                                    }

                                    //  thumbs.add(get_thumb);
                                }


                                db.addEmpData(new UserDetails_Model(null,get_uId,get_cid,get_attType,get_firstName,get_lastName,get_mobile, t1,t2,t3,t4,""));
                            }

                            Log.i("Reading: ", "Reading all contacts..");
                            List<UserDetails_Model> contacts = db.getAllEmpData();

                            for (UserDetails_Model cn : contacts) {
                                String log = "PrimaryKey: "+cn.getPrimaryKey()+",uId: "+cn.getUid()+",cId: "+cn.getCid()+", Type: "+cn.getAttType()+" ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();
                                // Writing Contacts to log
                                Log.i("Name: ", log);
                            }

                            /*Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();*/

                        }
                        catch (JSONException e) {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Log.e("JsonException123", e.toString());
                        }
                    }
                }
                else {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    //Toast.makeText(MainActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetUserData getUrlData = new GetUserData();
        getUrlData.execute();
    }

    public void getUserDataNew()
    {
        class GetUserData extends AsyncTask<String, Void, String>
        {
            String response1;

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Getting Thumb data...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/getallempdatadevicewiseoffline/?";
                    String query3 = String.format("deviceid=%s&flag=%s&empdevicearr=%s",
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode(flag, "UTF-8"),
                            URLEncoder.encode(empattDid, "UTF-8"));

                    query3 = query3.replace("%2C+",",");
                    URL url = new URL(leave_url+query3);
                    Log.i("url123", ""+ url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response1 = "";
                            response1 += line;
                        }
                    }
                    else
                    {
                        response1 = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("Exception", e.toString());
                }

                return response1;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson2 = result;
                    Log.i("myJson", myJson2);

                    GetDeviceName(android_id);

                    progressDialog.dismiss();

                    if (result.contains("<HTML><HEAD>"))
                    {
                        Toast.makeText(MainActivity.this, "Login to captive portal", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        if (myJson2.equals("[]"))
                        {
                            Toast.makeText(MainActivity.this, "No New Records Found", Toast.LENGTH_LONG).show();
                            List<UserDetails_Model> contacts = db.getAllEmpData();
                            for (UserDetails_Model cn : contacts)
                            {
                                String log = "PrimaryKey: "+cn.getPrimaryKey()+ ",uId: "+cn.getUid() +
                                        ", cId: "+cn.getCid()+ ", Type: "+cn.getAttType()+
                                        ", Name: " + cn.getFirstname() + " "+ cn.getLastname()+
                                        ", Phone: " + cn.getMobile_no()+ ", Shift: "+cn.getShift();
                                Log.i("Name: ", log);
                            }
                        }
                        else
                        {
                            try
                            {
                                // [{"empattDid":29,"uId":4,"firstName":"Amit","lastName":"Mhaske","cid":"Hrsaas2","mobile":"1202152102","status":"1","attendancetype":3,
                                JSONArray jsonArray = new JSONArray(myJson2);
                                Log.i("jsonArray123", "" + jsonArray);

                                empattDid_arr.clear();

                                for(int i=0; i <jsonArray.length(); i++)
                                {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    //Log.i("object123", "" + object);

                                    String get_status = object.getString("status");
                                    //Log.i("get_status",get_status);

                                    String empattDid = object.getString("empattDid");
                                    //Log.i("empattDid",empattDid);

                                    empattDid_arr.add(empattDid);

                                    if (get_status.equals("1"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);
                                        String get_firstName = object.getString("firstName");
                                        //Log.i("get_firstName",get_firstName);
                                        String get_lastName = object.getString("lastName");
                                        //Log.i("get_lastName",get_lastName);
                                        String get_cid = object.getString("cid");
                                        //Log.i("get_cid",get_cid);
                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);

                                        String get_attType = object.getString("attendancetype");
                                        //Log.i("get_attType",get_attType);
                                        String get_applyshift = object.getString("applyshift");
                                        Log.i("get_applyshift", get_applyshift);

                                        JSONArray thumbexpr = object.getJSONArray("Thumexp");
                                        //Log.i("thumbexpr1143",thumbexpr+"");

                                        String t1="",t2="",t3="",t4="";

                                        for(int j = 0; j < thumbexpr.length(); j++)
                                        {
                                            JSONObject object_thumb = thumbexpr.getJSONObject(j);
                                            //Log.i("object_thumb", "" + object_thumb);
                                            String get_thumb = object_thumb.getString(j+1+"");
                                            //Log.i("get_thumb",get_thumb);

                                            if(j+1 == 1)
                                            {
                                                t1 = get_thumb;
                                                //Log.i("t1",t1);
                                            }
                                            else if(j+1 == 2)
                                            {
                                                t2 = get_thumb;
                                                //Log.i("t2",t2);
                                            }
                                            else if(j+1 == 3)
                                            {
                                                t3 = get_thumb;
                                                //Log.i("t3",t3);
                                            }
                                            else if(j+1 == 4)
                                            {
                                                t4 = get_thumb;
                                                //Log.i("t4",t4);
                                            }
                                            else{
                                                t1 = "";
                                                t2 = "";
                                                t3 = "";
                                                t4 = "";
                                            }
                                        }

                                        if (db.checkEmpId(get_uId))
                                        {
                                            db.UpdateEmpAttType(new UserDetails_Model(t1, t2, t3, t4, get_attType, get_applyshift), get_uId);
                                        }
                                        else
                                        {
                                            db.addEmpData(new UserDetails_Model(null, get_uId, get_cid, get_attType, get_firstName, get_lastName, get_mobile, t1, t2, t3, t4,get_applyshift));
                                        }
                                    }
                                    else if (get_status.equals("2"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);

                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);

                                        String get_attType = object.getString("attendancetype");
                                        //Log.i("get_attType",get_attType);

                                        String get_applyshift = object.getString("applyshift");
                                        Log.i("get_applyshift", get_applyshift);

                                        JSONArray thumbexpr = object.getJSONArray("Thumexp");
                                        //Log.i("thumbexpr1143",thumbexpr+"");

                                        String t1="",t2="",t3="",t4="";

                                        for(int j = 0; j < thumbexpr.length(); j++)
                                        {
                                            JSONObject object_thumb = thumbexpr.getJSONObject(j);
                                            //Log.i("object_thumb", "" + object_thumb);
                                            String get_thumb = object_thumb.getString(j+1+"");
                                            //Log.i("get_thumb",get_thumb);

                                            if(j+1 == 1)
                                            {
                                                t1 = get_thumb;
                                                //Log.i("t1",t1);
                                            }
                                            else if(j+1 == 2)
                                            {
                                                t2 = get_thumb;
                                                //Log.i("t2",t2);
                                            }
                                            else if(j+1 == 3)
                                            {
                                                t3 = get_thumb;
                                                //Log.i("t3",t3);
                                            }
                                            else if(j+1 == 4)
                                            {
                                                t4 = get_thumb;
                                                //Log.i("t4",t4);
                                            }
                                            else{
                                                t1 = "";
                                                t2 = "";
                                                t3 = "";
                                                t4 = "";
                                            }
                                        }

                                        //Log.i("Insert: ", "Inserting ..");
                                        if (db.checkEmpId(get_uId))
                                        {
                                            db.UpdateEmpAttType(new UserDetails_Model(t1, t2, t3, t4, get_attType, get_applyshift), get_uId);
                                        }
                                    }
                                    else if (get_status.equals("3"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);

                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);

                                        if (db.checkEmpId(get_uId))
                                        {
                                            db.deleteEmpRecord(get_uId);
                                        }
                                    }
                                }

                                Toast.makeText(MainActivity.this, "Data updated successfully", Toast.LENGTH_LONG).show();

                                //Log.i("Reading: ", "Reading all contacts..");
                                List<UserDetails_Model> contacts = db.getAllEmpData();

                                for (UserDetails_Model cn : contacts)
                                {
                                    String log = "PrimaryKey: "+cn.getPrimaryKey()+",uId: "+cn.getUid()+",cId: "+cn.getCid()+", Type: "+cn.getAttType()+" ,Name: " + cn.getFirstname() +" "+ cn.getLastname()+ " ,Phone: " + cn.getMobile_no()+" ,Shift: "+cn.getShift();
                                    Log.i("Name: ", log);
                                }

                                flag = "2";
                                //offline_flag = "5";
                                empattDid = empattDid_arr.toString();
                                empattDid = empattDid.substring(1, (empattDid.length() -1));
                                Log.i("empattDid", empattDid);

                                getUserDataNew();
                            }
                            catch (JSONException e)
                            {
                                if (progressDialog != null && progressDialog.isShowing())
                                {
                                    progressDialog.dismiss();
                                }
                                Log.e("JsonException123", e.toString());
                            }
                        }
                    }
                }
                else
                {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        }

        GetUserData getUrlData = new GetUserData();
        getUrlData.execute();
    }

    public void getPrefixData()
    {
        class GetPrefixData extends AsyncTask<String, Void, String>
        {
            @Override
            protected void onPreExecute() {
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                { // http://hrsaas.safegird.com/owner/hrmapi/getprefix
                    String prefix_url = ""+url_http+""+Url+"/owner/hrmapi/getprefix?";

                    URL url = new URL(prefix_url);
                    Log.i("url", "" + url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response = "";
                            response += line;
                        }
                    }
                    else
                    {
                        response = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e){
                    Log.e("Exception", e.toString());
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson = result;
                    Log.i("myJson", myJson);

                    if (myJson.equals("[]"))
                    {
                        Toast.makeText(MainActivity.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(myJson);
                            //Log.i("jsonArray", "" + jsonArray);

                            JSONObject object = jsonArray.getJSONObject(0);

                            responseCode = object.getString("responseCode");
                            Log.i("responseCode_res",responseCode);

                            if (responseCode.equals("1"))
                            {
                                //progressDialog.dismiss();
                                editor_prefix = pref_prefix.edit();

                                get_prefix = object.getString("prefix");

                                editor_prefix.putString("prefix", get_prefix);
                                editor_prefix.putString("responseCode", responseCode);
                                editor_prefix.commit();

                            }
                            else
                            {
                                //progressDialog.dismiss();
                                editor_prefix.putString("responseCode", responseCode);
                                editor_prefix.commit();
                            }

                            outid = "3";
                            //upload_Data();

                        }
                        catch (JSONException e) {
                            //progressDialog.dismiss();
                            Log.e("JsonException", e.toString());
                        }
                    }
                }
                else {
                    //progressDialog.dismiss();
                    //Toast.makeText(MainActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetPrefixData getPrefixData = new GetPrefixData();
        getPrefixData.execute();
    }

    @Override
    public void onBackPressed()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void postData(final String requestURL, final HashMap<String, String> postDataParams)
    {
        class SendPostRequest extends AsyncTask<String, Void, String>
        {

            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Posting Data...", true);
                progressDialog.show();
            }

            protected String doInBackground(String... arg0)
            {

                try {
                    Log.i("requestURL", ""+requestURL);
                    Log.i("postDataParams", ""+postDataParams);

                    URL url = new URL(requestURL);
                    Log.i("url_post", ""+url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setUseCaches(false);
                    conn.setAllowUserInteraction(false);
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getPostDataString(postDataParams));

                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response_att = "";
                            response_att += line;
                        }
                    }
                    else
                    {
                        response_att = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }

                return response_att;
            }

            @Override
            protected void onPostExecute(String result)
            {
                Log.i("result", result);
                if (result != null)
                {
                    GetJSONData(result);
                }
                else
                {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        SendPostRequest request = new SendPostRequest();
        request.execute();
    }

    public String  performPostCall(String requestURL, HashMap<String, String> postDataParams)
    {
        URL url;
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK)
            {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null)
                {
                    response+=line;
                }
            }
            else
            {
                response="";
            }
        }
        catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                }
            });
            e.printStackTrace();
        }

        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet())
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public void GetJSONData(String result)
    {
        try
        {
            JSONArray json = new JSONArray(result);
            Log.i("json", "" + json);
//[{"empId":"115","datetime":"2017-07-21 12:21:58","responsecode":1,"signId":"2",
// "primarykey":"5","msg":"Offline Attendance Successfully Done "}]
            //[{"responsecode":0,"msg":"You are already signin","primarykey":"27"}]
            JSONObject jsonObject = json.getJSONObject(0);
            Log.i("jsonObject", "" + jsonObject);

            String responsecode = jsonObject.getString("responsecode");
            final String message = jsonObject.getString("msg");
            Log.i("message", "" + message);

            prev_key = jsonObject.getInt("primarykey");
            Log.i("prev_key", "" + prev_key);

            if (responsecode.equals("1"))
            {
                db.deletePrev3DaysRecord();
                //delete_prevAttRecord();
                progressDialog.dismiss();
                if (outid.equals("1"))
                {
                    session.logout_url();
                    key_editor = key_pref.edit();
                    key_editor.clear();
                    key_editor.commit();

                    editor_dev = pref_dev.edit();
                    editor_dev.clear();
                    editor_dev.commit();

                    db.delete_attendance_record();
                    db.deleteAllEmpRecord();
                    Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    key_editor = key_pref.edit();
                    key_editor.clear();
                    key_editor.putInt("key", prev_key);
                    key_editor.commit();

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    alertDialog.setMessage(message);
                    alertDialog.setCancelable(true);
                    alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alertDialog.show();
                }
            }
            else
            {
                progressDialog.dismiss();
                if (outid.equals("1"))
                {
                    session.logout_url();
                    key_editor = key_pref.edit();
                    key_editor.clear();
                    key_editor.commit();

                    editor_dev = pref_dev.edit();
                    editor_dev.clear();
                    editor_dev.commit();

                    db.delete_attendance_record();
                    db.deleteAllEmpRecord();
                    Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    key_editor = key_pref.edit();
                    key_editor.clear();
                    key_editor.putInt("key", prev_key);
                    key_editor.commit();
                }
            }
        }
        catch (JSONException e)
        {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(MainActivity.this, "JSON Exception", Toast.LENGTH_SHORT).show();
            Log.e("Fail 1", e.toString());
        }
    }

    public void GetDeviceName(final String device_id)
    {
        class SendDeviceID extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                /*progressDialog1 = new ProgressDialog(AttendanceActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog1.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog1.setTitle("Please wait");
                progressDialog1.setMessage("Receiving Device ID...");
                progressDialog1.show();*/
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/getnameofdeviceid/?";

                    String query = String.format("android_devide_id=%s",
                            URLEncoder.encode(device_id, "UTF-8"));

                    url = new URL(Transurl + query);
                    Log.i("url", "" + url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response += line;
                        }
                    }
                    else {
                        response = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog1.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog1.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog1.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                Log.i("response", result);
                if (result.equals("[]"))
                {
                    //progressDialog1.dismiss();
                    Toast.makeText(MainActivity.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);

                        String responsecode = object.getString("responseCode");

                        if (responsecode.equals("1"))
                        {
                            // progressDialog1.dismiss();

                            TabID = object.getString("responseMessage");
                            Log.i("TabID",TabID);

                            editor_dev = pref_dev.edit();
                            editor_dev.putString("TabID", TabID);
                            editor_dev.commit();
                        }
                        else
                        {
                            //progressDialog1.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setCancelable(true);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            });

                            alertDialog.show();
                        }
                    }
                    catch (JSONException e){
                        //progressDialog1.dismiss();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        SendDeviceID sendid = new SendDeviceID();
        sendid.execute();
    }
}
