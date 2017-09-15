package com.hrgirdattendance;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

import javax.net.ssl.HttpsURLConnection;

public class AttendanceNew extends AppCompatActivity implements MFS100Event {

    TextView lblMessage;
    EditText txtEventLog;

    byte[] Enroll_Template;
    byte[] Verify_Template;
    int mfsVer = 41;
    SharedPreferences settings;
    Context context;
    CommonMethod.ScannerAction scannerAction = CommonMethod.ScannerAction.Capture;

    int minQuality = 40;
    int timeout = 10000;
    MFS100 mfs100 = null;

    public static final String MyPREFERENCES_InOutKey = "MyPrefs_Key" ;
    ArrayList<String> date_array = new ArrayList<String>();
    ArrayList<String> inout_array = new ArrayList<String>();
    ArrayList<String> key_array = new ArrayList<String>();
    ArrayList<String> id_array = new ArrayList<String>();

    String PrimaryKey, InOutId, EmpId_sync, DateTime;
    int Prev_Key;

    SharedPreferences pref, shared_pref, key_pref;
    SharedPreferences.Editor editor, key_editor;

   /* static int app_mode_att = UrlActivity.app_mode;*/

    public static String _testKey = "t7L8wTG/iv02t+pgYrMQ7tt8qvU1z42nXpJDfAfsW592N4sKUHLd8A0MEV0GRxH+f4RgefEaMZALj7mgm/Thc0jNhR2CW9BZCTgeDPjC6q0W";


    public static final String MyPREFERENCES_url = "MyPrefs_url";
    SharedPreferences.Editor editor1;

    public static final String MyPREFERENCES = "MyPrefs";
    int PRIVATE_MODE = 0;

    public static final String MyPREFERENCES_prefix = "MyPrefs_prefix" ;
    SharedPreferences pref_prefix;

    String mahesh;

    String myJSON = null;
    String RegisteredBase64, MobileNo;
    String EmpId;
    String Sign_InOut_id = "1";
    String logout_id = "0";

    LinearLayout atndnc_logout;
    EditText ed_atndncLogout;
    Button btn_atndncLogout;
    Toolbar toolbar;
    EditText ed_MobNo;
    TextView txt_matchMsg, txt_Time, txt_success;
    ImageView img_Match;
    Button btn_signIn, btn_signOut;
    RadioButton rd_signIn, rd_signOut;
    RelativeLayout content_frame;
    CoordinatorLayout snackbarCoordinatorLayout;

    Calendar c;
    TextToSpeech textToSpeech;
    String ResponseCode, Message,response,myJson,get_prefix,responseCode;

    ProgressDialog progressDialog;

    static boolean logout_status = true;
    Timer timer;
    //AttendanceActivity.MyTimerTask myTimerTask;
    Handler someHandler;
    ConnectionDetector cd;
    String Url;
    String url_http;
    UserSessionManager session;
    CheckInternetConnection internetConnection;
    public static NetworkChange receiver;
    Snackbar snackbar;

    String android_id;
    boolean device_info = false;

    static int result_match = 0;
    DatabaseHandler db;

    Boolean emp_ext = false;
    boolean first_hit = false;

    Button btn_login, btn_Cancel;
    EditText ed_userName, ed_password;
    String UserName, Password;
    PopupWindow pw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        context = AttendanceNew.this.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mfsVer = Integer.parseInt(settings.getString("MFSVer",
                String.valueOf(mfsVer)));

        PubVar.sharedPrefernceDeviceMode = (SharedPreferences) context.getSharedPreferences(PubVar.strSpDeviceKey, Context.MODE_PRIVATE);

        mfs100 = new MFS100(this, mfsVer);
        mfs100.SetApplicationContext(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar_inner);
        TextView Header = (TextView) findViewById(R.id.header_text);
        ImageView img_logout = (ImageView) findViewById(R.id.img_logout);
        setSupportActionBar(toolbar);

        img_logout.setVisibility(View.GONE);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());
        db = new DatabaseHandler(this);
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();

        //--------------------------//
        key_pref = getApplicationContext().getSharedPreferences(MyPREFERENCES_InOutKey, MODE_PRIVATE);
        Prev_Key = key_pref.getInt("key",0);
        Log.i("PrevKey_pref", ""+Prev_Key);
        pref = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE);

        pref_prefix = getApplicationContext().getSharedPreferences(MyPREFERENCES_prefix, MODE_PRIVATE);
        get_prefix = pref_prefix.getString("prefix","");
        responseCode = pref_prefix.getString("responseCode","");
        //Log.i("get_prefix", get_prefix);
        Log.i("responseCode", responseCode);

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            Header.setText("ATTENDANCE");
            img_logout.setVisibility(View.GONE);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.abc_ic_ab_back_material));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Initialisation();

        receiver = new NetworkChange()
        {
            @Override
            protected void onNetworkChange()
            {
                if (receiver.isConnected)
                {
                    first_hit = true;
                    sync_data();
                    if (snackbar != null)
                    {
                        snackbar.dismiss();
                    }
                }
                else {
                    //internetConnection.showNetDisabledAlertToUser(AttendanceNew.this);
                    snackbar = Snackbar.make(snackbarCoordinatorLayout, "You are offline", Snackbar.LENGTH_INDEFINITE);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                    snackbar.show();
                }
            }
        };
    }

    public void Initialisation()
    {
        atndnc_logout = (LinearLayout) findViewById(R.id.atndnc_logout);
        ed_atndncLogout = (EditText) findViewById(R.id.ed_atndnc_logout);
        btn_atndncLogout = (Button) findViewById(R.id.btn_atndnc_logout);
        content_frame = (RelativeLayout) findViewById(R.id.content_frame);
        txt_Time = (TextView) findViewById(R.id.txtTime);
        snackbarCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.snackbarCoordinatorLayout);

        ed_MobNo = (EditText) findViewById(R.id.ed_match_mobNo);
        txt_matchMsg = (TextView) findViewById(R.id.txtMatch);

        txt_success = (TextView) findViewById(R.id.tv_signinsuccess);

        img_Match = (ImageView) findViewById(R.id.match_fingerprint);
        btn_signIn = (Button) findViewById(R.id.btn_atndnc_signIn);
        btn_signOut = (Button) findViewById(R.id.btn_atndnc_signOut);
        rd_signIn = (RadioButton) findViewById(R.id.radio_signIn);
        rd_signOut = (RadioButton) findViewById(R.id.radio_signOut);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        rd_signIn.setChecked(true);

        rd_signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sign_InOut_id = "1";
                Log.i("MFS_Log","Signin clicked -> Sign_InOut_id: " + Sign_InOut_id);
                rd_signIn.setButtonDrawable(getResources().getDrawable(R.drawable.checkedradiobtn));
                rd_signOut.setButtonDrawable(getResources().getDrawable(R.drawable.uncheckedradiobtn));
            }
        });

        rd_signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sign_InOut_id = "2";
                Log.i("MFS_Log","Signout clicked -> Sign_InOut_id:"+Sign_InOut_id);
                rd_signIn.setButtonDrawable(getResources().getDrawable(R.drawable.uncheckedradiobtn));
                rd_signOut.setButtonDrawable(getResources().getDrawable(R.drawable.checkedradiobtn));
            }
        });

        deviceData();

        someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy \nHH:mm:ss");
                sdf.setLenient(false);
                Date today = new Date();
                String time = sdf.format(today);
                txt_Time.setText(time);
                someHandler.postDelayed(this, 1000);
            }
        }, 1);

        ed_MobNo.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                String mobNo = ed_MobNo.getText().toString();
                Log.i("mobNo", mobNo);
                Log.i("mobNo_length", "" + mobNo.length());

                MobileNo = ed_MobNo.getText().toString();
                Log.i("MobileNo", MobileNo);

                if (mobNo.length() == 0) {
                    mfs100.StopCapture();
                    Log.i(" MFS_Log ed_MobNo", ed_MobNo.getText().toString());
                }

                if (mobNo.length() == 1) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InitScanner();
                        }
                    }).start();

                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    MobileNo = ed_MobNo.getText().toString();
                    StartSyncCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 4) {

                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    MobileNo = ed_MobNo.getText().toString();
                    StartSyncCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 5 || mobNo.length() == 6 || mobNo.length() == 7 || mobNo.length() == 8 || mobNo.length() == 9) {
                    mfs100.StopCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 10) {
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    MobileNo = ed_MobNo.getText().toString();
                    StartSyncCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void delete_prevAttRecord()
    {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date today = new Date();
            String date_time = sdf.format(today);
            Log.i("date_time", date_time);
            Date myDate = sdf.parse(date_time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(myDate);
            calendar.add(Calendar.DAY_OF_YEAR, -5);
            Date newDate = calendar.getTime();

            String date = sdf.format(newDate);
            Log.i("Prev_date_time", date);

            SigninOut_Model temp_sm_signinout =  db.checkdata();

            if(temp_sm_signinout != null)
            {
                List<SigninOut_Model> contacts = db.getSigninoutData(0);
                Log.i("MFS_Log contacts", "" + contacts);

                if (!contacts.isEmpty())
                {
                    for (SigninOut_Model cn : contacts)
                    {
                        String date_data = cn.getDate_Time();
                        Log.i("MFS_Log date_data", date_data);

                        //db.delete_prev_att_record(date_data, date);
                    }

                    db.delete_3daysE_record();
                    Log.i("MFS data", "data deleted");

                    for (SigninOut_Model cn : contacts)
                    {
                        String date_data = cn.getDate_Time();
                        Log.i("MFS_Log date_data_rem", date_data);
                    }
                }
            }

        }
        catch (ParseException e)
        {
            e.printStackTrace();
            Log.i("Exception Parse",e+"");
        }
    }


    public void deviceData()
    {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;

        String Androidversion = manufacturer + model + version + versionRelease;
    }

    Handler handler2;
    Runnable runnable;
    int i = 0;

    public void onControlClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.btnForLoop:
                Toast.makeText(AttendanceNew.this, "Loop for init->uninit->init... 500 times", Toast.LENGTH_LONG).show();
                i = 0;
                handler2 = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        // Log.e("1", ""+ (i+1));
                        if (i >= 500)
                        {
                            handler2.removeCallbacks(runnable);
                        }
                        else
                        {
                            if (i % 2 == 0) {
                                InitScanner();
                            }
                            else {
                                UnInitScanner();
                            }
                            i++;
                            handler2.postDelayed(runnable, 100);
                        }
                    }
                };
                handler2.post(runnable);
                break;

            default:
                break;
        }
    }

    private void InitScanner()
    {
        try
        {
            int ret = mfs100.Init();
            if (ret != 0)
            {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
                Log.i("info", "fail - " + (mfs100.GetErrorMsg(ret)));
            }
            else {
                SetTextonuiThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                //SetLogOnUIThread(info);
                Log.i("info", info);
            }
        }
        catch (Exception ex) {
            Toast.makeText(this, "Init failed, unhandled exception", Toast.LENGTH_LONG).show();
            SetTextonuiThread("Init failed, unhandled exception");
        }
    }

    public void sync_data()
    {
        Log.i("sync_data","sync_data");

        SigninOut_Model temp_sm_signinout =  db.checkdata();

        if(temp_sm_signinout != null)
        {
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
                    Log.i("MFS_Log primarykey_data", primarykey_data);
                    Log.i("MFS_Log date_data", date_data);
                    Log.i("MFS_Log id_data", id_data);
                    Log.i("MFS_Log in_out_data", in_out_data);

                    kkeay = Integer.parseInt(primarykey_data);

                    Log.i("PrevKey", ""+Prev_Key);
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
                    Toast.makeText(AttendanceNew.this, "No Records Found", Toast.LENGTH_SHORT).show();
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

                    Log.i("MFS_Log PrimaryKey", "" + PrimaryKey);
                    Log.i("MFS_Log EmpId", "" + EmpId);
                    Log.i("MFS_Log DateTime", "" + DateTime);
                    Log.i("MFS_Log InOutId", "" + InOutId);
                    if (first_hit) {
                        progressDialog = ProgressDialog.show(AttendanceNew.this, "Please Wait", "Uploading Data... ", true);
                    }

                    new Thread(new Runnable() {
                        public void run() {
                            sendSignInOutData();
                        }
                    }).start();
                }
            }
            else {
                Toast.makeText(AttendanceNew.this, "No Records Found", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(AttendanceNew.this, "No Records Found", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendSignInOutData()
    {
        long TIME_OUT_IN_SECONDS = 120;

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

            Prev_Key = jsonObject.getInt("primarykey");
            Log.i("Prev_Key", "" + Prev_Key);

            if (responsecode.equals("1"))
            {
                if (first_hit) {
                    delete_prevAttRecord();
                }
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                            Toast.makeText(AttendanceNew.this, message, Toast.LENGTH_SHORT).show();
                        }
                        key_editor = key_pref.edit();
                        key_editor.putInt("key", Prev_Key);
                        key_editor.commit();
                    }
                });
            }
            else
            {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(AttendanceNew.this, message, Toast.LENGTH_SHORT).show();
                }

                key_editor = key_pref.edit();
                key_editor.putInt("key", Prev_Key);
                key_editor.commit();
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
                        Toast.makeText(AttendanceNew.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
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
                        Toast.makeText(AttendanceNew.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            Log.e("Fail 1", e.toString());
        }
        catch (Exception e)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        Toast.makeText(AttendanceNew.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            Log.e("Fail 1", e.toString());
        }
    }

    private void StartSyncCapture()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                SetTextonuiThread("");
                try
                {
                    FingerData fingerData = new FingerData();
                    //int ret = mfs100.AutoCapture(fingerData, timeout, true, false);

                    int ret = mfs100.StartCapture(minQuality, timeout, true);
                    if (ret != 0)
                    {
                        SetTextonuiThread(mfs100.GetErrorMsg(ret));
                    }
                    else {
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(
                                fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);

                        img_Match.post(new Runnable() {
                            @Override
                            public void run() {
                                img_Match.setImageBitmap(bitmap);
                                img_Match.refreshDrawableState();
                            }
                        });

                        SetTextonuiThread("Capture Success");
                        String log = "\nQuality: " + fingerData.Quality()
                                + "\nNFIQ: " + fingerData.Nfiq()
                                + "\nWSQ Compress Ratio: "
                                + fingerData.WSQCompressRatio()
                                + "\nImage Dimensions (inch): "
                                + fingerData.InWidth() + "\" X "
                                + fingerData.InHeight() + "\""
                                + "\nImage Area (inch): " + fingerData.InArea()
                                + "\"" + "\nResolution (dpi/ppi): "
                                + fingerData.Resolution() + "\nGray Scale: "
                                + fingerData.GrayScale() + "\nBits Per Pixal: "
                                + fingerData.Bpp() + "\nWSQ Info: "
                                + fingerData.WSQInfo();

                        Log.i("MFS_Log","In SyncStart Capture success"+ log);
                        //SetLogOnUIThread(log);

                        //////////////////// Extract ISO Image
                        byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
                        byte[] isoImage = null;
                        int dataLen = mfs100.ExtractISOImage(fingerData.RawData(), tempData);
                        if (dataLen <= 0)
                        {
                            if (dataLen == 0)
                            {
                                SetTextonuiThread("Failed to extract ISO Image");
                            }
                            else {
                                SetTextonuiThread(mfs100.GetErrorMsg(dataLen));
                            }
                            return;
                        }
                        else {
                            isoImage = new byte[dataLen];
                            System.arraycopy(tempData, 0, isoImage, 0,
                                    dataLen);
                        }

                        Log.i("MFS_Log","fingerData "+fingerData);

                        Log.i("app_mode_att 1 ",UrlActivity.app_mode+"");

                        getalldata(fingerData);
                    }
                }
                catch (Exception ex) {
                    Log.i("MFS_Log Exception 1",ex+"");
                    SetTextonuiThread("Error");
                }
            }
        }).start();
    }

    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
            } else {
                //SetLogOnUIThread("Uninit Success");
                SetTextonuiThread("Uninit Success");
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    private void SetTextonuiThread(final String str) {
    }

    @Override
    public void OnPreview(FingerData fingerData) {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0, fingerData.FingerImage().length);
        img_Match.post(new Runnable() {
            @Override
            public void run() {
                img_Match.setImageBitmap(bitmap);
                img_Match.refreshDrawableState();

            }
        });
        SetTextonuiThread("Quality: " + fingerData.Quality());
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData) {
//		SetLogOnUIThread("EndTime: " + getCurrentTime());
        Log.i("MFS_Log capture_cmplt", "capture_cmplt");
        if (status) {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(
                    fingerData.FingerImage(), 0,
                    fingerData.FingerImage().length);
            img_Match.post(new Runnable() {
                @Override
                public void run() {
                    img_Match.setImageBitmap(bitmap);
                    img_Match.refreshDrawableState();
                }
            });
            SetTextonuiThread("Capture Success");
            String log = "\nQuality: " + fingerData.Quality() + "\nNFIQ: "
                    + fingerData.Nfiq() + "\nWSQ Compress Ratio: "
                    + fingerData.WSQCompressRatio()
                    + "\nImage Dimensions (inch): " + fingerData.InWidth()
                    + "\" X " + fingerData.InHeight() + "\""
                    + "\nImage Area (inch): " + fingerData.InArea() + "\""
                    + "\nResolution (dpi/ppi): " + fingerData.Resolution()
                    + "\nGray Scale: " + fingerData.GrayScale()
                    + "\nBits Per Pixal: " + fingerData.Bpp() + "\nWSQ Info: "
                    + fingerData.WSQInfo();
            //SetLogOnUIThread(log);
            Log.i("MFS_ capture_cmplt_log", log);

            getalldata(fingerData);
        }
        else {
            SetTextonuiThread("Error: " + errorCode + "(" + errorMsg + ")");
        }
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission)
    {
        int ret = 0;
        if (!hasPermission)
        {
            SetTextonuiThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279)
        {
            if (pid == 34323)
            {
                ret = mfs100.LoadFirmware();
                if (ret != 0)
                {
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                }
                else {
                    SetTextonuiThread("Loadfirmware success");
                }
            }
            else if (pid == 4101)
            {
                //Added by Milan Sheth on 19-Dec-2016
                String strDeviceMode = PubVar.sharedPrefernceDeviceMode.getString(PubVar.strSpDeviceKey, "public");
                if (strDeviceMode.toLowerCase().equalsIgnoreCase("public"))
                {
                    ret = mfs100.Init("");
                    if (ret == -1322)
                    {
                        ret = mfs100.Init(_testKey);
                        if (ret == 0)
                        {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                            //showSuccessLog();
                        }
                    }
                    else if (ret == 0) {
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                        //showSuccessLog();
                    }
                }
                else {
                    ret = mfs100.Init(_testKey);
                    if (ret == -1322)
                    {
                        ret = mfs100.Init("");
                        if (ret == 0)
                        {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                            //showSuccessLog();
                        }
                    }
                    else if (ret == 0) {
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                        //showSuccessLog();
                    }
                }

                if (ret != 0)
                {
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                }
            }
        }
    }

    @Override
    public void OnDeviceDetached() {
        UnInitScanner();
        SetTextonuiThread("Device removed");
    }

    @Override
    public void OnHostCheckFailed(String err) {
        try {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
        catch (Exception ex) {
        }
    }

    public void getalldata(final FingerData fingerData)
    {
        class GetUserData extends AsyncTask<String, Void, String>
        {
            String response1;

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    Log.i("MFS_Log ", "In getalldata");
                }
                catch (Exception e) {
                    Log.i("MFS_Log Exception 2", e.toString());
                }

                return response1;
            }

            @Override
            protected void onPostExecute(String result)
            {
                //progressDialog.dismiss();
                try
                {
                    String t1 = "", t2 = "", t3 = "", t4 = "", uid,
                            ufname, ulname, u_mobile_nu, ucid="", att_type="";

                    // Reading all contacts
                    Log.i("Reading: ", "Reading all contacts..");
                    Log.i("MFS_Log ", "Reading");
                    List<UserDetails_Model> contacts = db.getAllContacts();

                    Log.i("MFS_Log contacts", ""+contacts);

                    result_match = 0;

                    for (UserDetails_Model cn : contacts)
                    {
                        String log = "Id: " + cn.getUid() + " ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();

                        u_mobile_nu = cn.getMobile_no();
                        uid = cn.getUid();
                        ucid = cn.getCid();
                        att_type = cn.getAttType();
                        Log.i("MFS_Log", log);
                        Log.i("MFS_Log u_mobile_nu", u_mobile_nu);
                        Log.i("MFS_Log uid", uid);
                        Log.i("MFS_Log ucid", ucid);
                        Log.i("MFS_Log ucid", att_type);

                        if (att_type.equals("1"))
                        {
                            emp_ext = true;

                            Log.i("emp_ext", emp_ext + "");
                            t1 = cn.getThumb1();
                            t2 = cn.getThumb2();
                            t3 = cn.getThumb3();
                            t4 = cn.getThumb4();

                            ufname = cn.getFirstname();
                            ulname = cn.getLastname();

                            String thumbs[] = {t1, t2, t3, t4};

                            // Writing Contacts to log
                            Log.i("Att Name: ", log);

                            for (int i = 0; i < thumbs.length; i++)
                            {
                                if (result_match <= 1000)
                                {
                                    RegisteredBase64 = thumbs[i];
                                    Log.i("MFS_ RegisteredBase64", RegisteredBase64);

                                    EmpId = cn.getUid();
                                    Log.i("MFS_Log EmpId", EmpId);

                                    Enroll_Template = Base64.decode(RegisteredBase64, Base64.DEFAULT);
                                    Log.i("MFS_Log Enroll_Template", "" + Enroll_Template);

                                    Verify_Template = new byte[fingerData.ISOTemplate().length];
                                    System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                                            fingerData.ISOTemplate().length);

                                    Log.i("MFS_Log Verify_Template", "" + Verify_Template);

                                    result_match = mfs100.MatchISO(Enroll_Template, Verify_Template);

                                    //sgfplib.MatchIsoTemplate(mRegisterTemplate, 0, mVerifyTemplate, 0, SGFDxSecurityLevel.SL_NORMAL, matched);

                                    Log.i("MFS_Log result_match", "" + result_match);

                                    if (result_match >= 1400)
                                    {
                                        Log.i("app_mode_att 1 ", UrlActivity.app_mode + "");
                                        make_offline_attendance(uid, ufname, ulname, u_mobile_nu, Sign_InOut_id);
                                        break;

                                    }
                                    else {
                                        Log.i("MFS_Log NOT MATCHED!!", "NOT MATCHED!!");
                                    }
                                }
                            }

                            if (result_match <= 1000)
                            {
                                txt_matchMsg.setText("Unsuccessful");
                                //textToSpeech.speak("Sorry Thumb Not Matched", TextToSpeech.QUEUE_FLUSH, null);
                                txt_matchMsg.setTextColor(Color.RED);
                                textToSpeech.speak("Sorry thumb not matched!!", TextToSpeech.QUEUE_FLUSH, null);
                                txt_success.setText("Sorry thumb not matched!!\n");
                                Log.i("MFS_Log NOT MATCHED!!", "NOT MATCHED!!");

                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.i("MFS_Log start", "start");
                                        txt_matchMsg.setText("");
                                        txt_success.setText("");
                                        //ed_MobNo.setText("");
                                        img_Match.setImageResource(R.drawable.imagefinger);
                                        scannerAction = CommonMethod.ScannerAction.Capture;
                                        StartSyncCapture();
                                        //img_Match.setBackground(getResources().getDrawable(R.drawable.imagefinger));
                                    }
                                }, 3000);
                            }
                        }
                        else
                        {
                            emp_ext = true;
                            txt_matchMsg.setText("Unsuccessful");
                            //textToSpeech.speak("Sorry Thumb Not Matched", TextToSpeech.QUEUE_FLUSH, null);
                            txt_matchMsg.setTextColor(Color.RED);
                            textToSpeech.speak("You are not authorized for Thumb!!", TextToSpeech.QUEUE_FLUSH, null);
                            txt_success.setText("You are not authorized for Thumb!!\n");
                            Log.i("MFS_Log Employee!!", "You are not authorise for thumb!!");

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i("MFS_Log Exist", "You are not authorise for thumb");
                                    txt_matchMsg.setText("");
                                    txt_success.setText("");
                                    ed_MobNo.setText("");
                                    img_Match.setImageResource(R.drawable.imagefinger);
                                }
                            }, 3000);
                        }
                        break;
                    }

                    Log.i("emp_ext",emp_ext+"");
                    if(!emp_ext)
                    {
                        txt_matchMsg.setText("Unsuccessful");
                        //textToSpeech.speak("Sorry Thumb Not Matched", TextToSpeech.QUEUE_FLUSH, null);
                        txt_matchMsg.setTextColor(Color.RED);
                        textToSpeech.speak("Employee Not Exist!!", TextToSpeech.QUEUE_FLUSH, null);
                        txt_success.setText("Employee Not Exist!!\n");
                        Log.i("MFS_Log Employee!!", "Employee Not Exist!!");

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Log.i("MFS_Log Exist", "EMP Not Exist");
                                txt_matchMsg.setText("");
                                txt_success.setText("");
                                img_Match.setImageResource(R.drawable.imagefinger);
                            }
                        }, 3000);
                        emp_ext = false;
                    }
                }
                catch (Exception e) {
                    Log.i("MFS_Log Exception 3", e + "");
                }
            }
        }

        GetUserData getUrlData = new GetUserData();
        getUrlData.execute();
    }

    public void sync_data_check_internet()
    {
        if (internetConnection.hasConnection(getApplicationContext()))
        {
            first_hit = false;
            sync_data();
        }
    }

    public void make_offline_attendance(String uid, String fname, String lname, String mobileno,String inout)
    {
        String firstName = fname;
        String lastName = lname;
        String empName = firstName + lastName;
        //String date_time = txt_Time.getText().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setLenient(false);
        Date today = new Date();
        String date_time = sdf.format(today);
        Log.i("txt_Time date_time",date_time);
        SigninOut_Model sm = new SigninOut_Model();

        sm.setUserId(uid);
        sm.setDate_Time(date_time);
        sm.setSignInOutId(Sign_InOut_id);
        Log.i("getUserId ",sm.getUserId());
        Log.i("getDate_Time ",sm.getDate_Time());
        Log.i("getSignInOutId ",sm.getSignInOutId());

        if (inout.equals("1"))
        {
            Log.i("else2", "else");
            db.adddata_signinout(sm);

            Log.i("Reading: ", "Reading sign in data..");

            List<SigninOut_Model> contacts = db.getSigninoutData(0);

            Log.i("MFS_Log contacts", "" + contacts);

            for (SigninOut_Model cn : contacts)
            {
                String log = "PrimaryKey: " + cn.getPrimaryKey() + ", Id: " + cn.getUserId() + ", DateTime: " + cn.getDate_Time() + ", SignInUot: " + cn.getSignInOutId();
                Log.i("MFS_Log", log);
            }

            ed_MobNo.setText("");
            txt_matchMsg.setText("Successful");
            txt_matchMsg.setTextColor(getResources().getColor(R.color.GreenColor));
            txt_success.setText("Sign In Successfully");

            textToSpeech.speak("Welcome " + fname, TextToSpeech.QUEUE_FLUSH, null);
            Log.i("MFS_Log MATCHED", "MATCHED");
            ed_MobNo.setText("");
            sync_data_check_internet();
            Log.i("MFS_Log Successful", "Successful");
        }
        else if (inout.equals("2"))
        {
            Log.i("else if", "else if");

            db.adddata_signinout(sm);
            // Reading all contacts
            Log.i("Reading: ", "Reading sign out data..");
            Log.i("MFS_Log ", "Reading SignOut");
            List<SigninOut_Model> contacts = db.getSigninoutData(0);

            Log.i("MFS_Log contacts", "" + contacts);

            for (SigninOut_Model cn : contacts)
            {
                String log = "PrimaryKey: " + cn.getPrimaryKey() + ", Id: " + cn.getUserId() + ", DateTime: " + cn.getDate_Time() + ", SignInUot: " + cn.getSignInOutId();
                Log.i("MFS_Log", log);
            }

            ed_MobNo.setText("");
            txt_matchMsg.setText("Successful");
            txt_matchMsg.setTextColor(getResources().getColor(R.color.GreenColor));
            txt_success.setText("Sign Out Successfully");

            textToSpeech.speak("Bye Bye " + fname, TextToSpeech.QUEUE_FLUSH, null);
            Log.i("MFS_Log MATCHED", "MATCHED");
            ed_MobNo.setText("");

            sync_data_check_internet();
            Log.i("MFS_Log Successful1", "Successful");
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("MFS_Log start", "start");
                txt_matchMsg.setText("");
                txt_success.setText("");
                img_Match.setImageResource(R.drawable.imagefinger);
            }
        }, 5000);
    }

    public void popup_window(View v)
    {
        try {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            LayoutInflater inflater = (LayoutInflater) AttendanceNew.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.destroy_app_login, (ViewGroup) findViewById(R.id.destroy_login_layout));

            pw = new PopupWindow(layout, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            pw.setWidth(width-40);
            pw.showAtLocation(v, Gravity.CENTER, 0, 0);

            dimBehind(pw);

            ed_userName = (EditText)layout.findViewById(R.id.ed_userName_dest);
            ed_password = (EditText)layout.findViewById(R.id.ed_password_dest);
            btn_login = (Button) layout.findViewById(R.id.btn_signIn_dest);
            btn_Cancel = (Button) layout.findViewById(R.id.btn_Cancel_dest);
            btn_login.setText("SignOut");
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
                            Toast.makeText(AttendanceNew.this, "Please enter username & password", Toast.LENGTH_LONG).show();
                        }
                        else if (UserName.equals(""))
                        {
                            Toast.makeText(AttendanceNew.this, "Please enter username", Toast.LENGTH_LONG).show();
                        }
                        else if (Password.equals(""))
                        {
                            Toast.makeText(AttendanceNew.this, "Please enter password", Toast.LENGTH_LONG).show();
                        }
                        else {
                            signIn();
                        }
                    }
                    else {
                        Toast.makeText(AttendanceNew.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                container = (View) popupWindow.getContentView().getParent();
            }
            else {
                container = popupWindow.getContentView();
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container = (View) popupWindow.getContentView().getParent().getParent();
            }
            else {
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
                progressDialog = new ProgressDialog(AttendanceNew.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Please wait...");
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/signIn/?";

                    String query = String.format("email=%s&password=%s&android_devide_id=%s&signinby=%s",
                            URLEncoder.encode(UserName, "UTF-8"),
                            URLEncoder.encode(Password, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"));

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
                catch (Exception e) {
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
                    Toast.makeText(AttendanceNew.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
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

                            /*mfs100.StopCapture();
                            Intent intent = new Intent(AttendanceNew.this, MainActivity.class);
                            startActivity(intent);
                            finish();*/
                            //session.createUserLoginSession(UserName, Password);
                        }
                        else
                        {
                            progressDialog.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(AttendanceNew.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
                        progressDialog.dismiss();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
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

    protected void onStop() {
        UnInitScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mfs100 != null) {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        mfs100.StopCapture();
        Intent intent = new Intent(AttendanceNew.this, MainActivity.class);
        startActivity(intent);
        finish();

       /* AlertDialog.Builder alertDialog = new AlertDialog.Builder(AttendanceNew.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        alertDialog.setMessage("Do you want to Sign Out?");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                popup_window(btn_atndncLogout.getRootView());
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();*/
    }
}
