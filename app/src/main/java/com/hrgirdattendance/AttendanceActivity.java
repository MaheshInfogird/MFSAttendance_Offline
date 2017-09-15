package com.hrgirdattendance;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;
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

public class AttendanceActivity extends AppCompatActivity implements MFS100Event
{
    byte[] Enroll_Template;
    byte[] Verify_Template;
    int mfsVer = 41;
    SharedPreferences settings;
    Context context;
    CommonMethod.ScannerAction scannerAction = CommonMethod.ScannerAction.Capture;

    int minQuality = 40;
    int timeout = 10000;
    MFS100 mfs100 = null;

    public static String _testKey = "t7L8wTG/iv02t+pgYrMQ7tt8qvU1z42nXpJDfAfsW592N4sKUHLd8A0MEV0GRxH+f4RgefEaMZALj7mgm/Thc0jNhR2CW9BZCTgeDPjC6q0W";

    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String MyPREFERENCES_prefix = "MyPrefs_prefix" ;
    public static final String MyPREFERENCES_InOutKey = "MyPrefs_Key" ;
    SharedPreferences pref_prefix;
    SharedPreferences  shared_pref, pref;
    SharedPreferences  key_pref;
    SharedPreferences.Editor key_editor;

    String RegisteredBase64;
    String CaptureBase64;
    String EmpId;
    String Sign_InOut_id = "1";
    String Url;
    String url_http;
    String logo;
    String android_id;
    String PrimaryKey, InOutId, DateTime;

    String aa;

    Toolbar toolbar;
    CoordinatorLayout snackbarCoordinatorLayout;
    TextView txt_date, txt_time, txt_time_a, txt_result, txt_att_name, txt_quality_per, txt_quality_success;
    ImageView img_thumb_result, img_in_mark, img_out_mark;
    Button btn_signIn, btn_signOut;
    ProgressBar progress_quality;
    Snackbar snackbar;

    TextToSpeech textToSpeech;
    ProgressDialog progressDialog;
    Handler someHandler;
    ConnectionDetector cd;
    UserSessionManager session;
    CheckInternetConnection internetConnection;
    public static NetworkChange receiver;
    DatabaseHandler db;

    int result_match = 0;
    int Prev_Key, prev_key;

    boolean first_hit = false;
    Boolean emp_ext = false;

    ArrayList<String> date_array = new ArrayList<String>();
    ArrayList<String> inout_array = new ArrayList<String>();
    ArrayList<String> key_array = new ArrayList<String>();
    ArrayList<String> id_array = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_new);
        context = AttendanceActivity.this.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mfsVer = Integer.parseInt(settings.getString("MFSVer", String.valueOf(mfsVer)));

        PubVar.sharedPrefernceDeviceMode = (SharedPreferences) context.getSharedPreferences(PubVar.strSpDeviceKey, Context.MODE_PRIVATE);

        mfs100 = new MFS100(this, mfsVer);
        mfs100.SetApplicationContext(this);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner_att);
        ImageView img_logo = (ImageView)findViewById(R.id.img_logo_att);
        setSupportActionBar(toolbar);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());

        db = new DatabaseHandler(this);
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));

        key_pref = getApplicationContext().getSharedPreferences(MyPREFERENCES_InOutKey, MODE_PRIVATE);
        Prev_Key = key_pref.getInt("key",0);
        Log.i("PrevKey_pref", ""+Prev_Key);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Picasso.with(AttendanceActivity.this).load(logo).into(img_logo);
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
        deviceData();

        if (internetConnection.hasConnection(AttendanceActivity.this))
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InitScanner();
                }
            }).start();
        }
        else {
            //internetConnection.showNetDisabledAlertToUser(AttendanceActivity.this);
        }

        receiver = new NetworkChange()
        {
            @Override
            protected void onNetworkChange()
            {
                if (receiver.isConnected)
                {
                    if (snackbar != null)
                    {
                        snackbar.dismiss();
                    }
                }
                else
                {
                    snackbar = Snackbar.make(snackbarCoordinatorLayout, "", Snackbar.LENGTH_INDEFINITE);
                    Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
                    TextView textView = (TextView) layout.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setVisibility(View.INVISIBLE);
                    LayoutInflater inflater = LayoutInflater.from(snackbar.getContext());
                    View snackView = inflater.inflate(R.layout.snackbar_layout, null);
                    layout.addView(snackView, 0);
                    layout.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                    snackbar.show();
                }
            }
        };
    }

    public void Initialisation()
    {
        txt_date = (TextView)findViewById(R.id.txt_att_date);
        txt_time = (TextView)findViewById(R.id.txt_att_time);
        txt_time_a = (TextView)findViewById(R.id.txt_att_time_a);
        txt_result = (TextView)findViewById(R.id.txt_att_result);
        txt_att_name = (TextView)findViewById(R.id.txt_att_name);
        txt_quality_per = (TextView)findViewById(R.id.txt_att_quality_per);
        txt_quality_success = (TextView)findViewById(R.id.txt_att_quality_success);

        img_thumb_result = (ImageView)findViewById(R.id.img_thumb_result);
        img_in_mark = (ImageView)findViewById(R.id.img_in);
        img_out_mark = (ImageView)findViewById(R.id.img_out);

        btn_signIn = (Button)findViewById(R.id.btn_att_signIn);
        btn_signOut = (Button)findViewById(R.id.btn_att_signOut);

        snackbarCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.snackbarCoordinatorLayout_att);

        progress_quality = (ProgressBar)findViewById(R.id.progressBar_quality);
        progress_quality.setMax(100);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if(status != TextToSpeech.ERROR)
                {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        String currentDate = sdf.format(calendar.getTime());

        try {
            Date date = sdf.parse(currentDate);
            SimpleDateFormat outFormat = new SimpleDateFormat("EEE");
            String day = outFormat.format(date);
            Log.i("current_date", day+", "+currentDate);

            txt_date.setText(day+", "+currentDate);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        img_in_mark.setVisibility(View.GONE);
        img_out_mark.setVisibility(View.GONE);
        txt_result.setText("");
        txt_att_name.setText("");
        txt_quality_success.setVisibility(View.INVISIBLE);

        btn_signIn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sign_InOut_id = "1";
                img_in_mark.setVisibility(View.VISIBLE);
                img_out_mark.setVisibility(View.GONE);
                if (internetConnection.hasConnection(AttendanceActivity.this))
                {
                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    StartSyncCapture();
                }
                else {
                    Toast.makeText(AttendanceActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_signOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sign_InOut_id = "2";
                img_in_mark.setVisibility(View.GONE);
                img_out_mark.setVisibility(View.VISIBLE);
                if (internetConnection.hasConnection(AttendanceActivity.this))
                {
                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    StartSyncCapture();
                }
                else {
                    Toast.makeText(AttendanceActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
                SimpleDateFormat sdf1 = new SimpleDateFormat("a");
                sdf.setLenient(false);
                Date today = new Date();
                String time = sdf.format(today);
                String time_a = sdf1.format(today);
                String str = time_a.replace("AM", "am").replace("PM","pm");
                txt_time.setText(time);
                txt_time_a.setText(str);
                someHandler.postDelayed(this, 1000);
            }
        }, 10);
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
                    String date_data = cn.getDate_Time();
                    Log.i("MFS_Log date_data", date_data);
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

    public void deviceData()
    {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
    }

    Handler handler2;
    Runnable runnable;
    int i = 0;

    public void onControlClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.btnForLoop:
                Toast.makeText(AttendanceActivity.this, "Loop for init->uninit->init... 500 times", Toast.LENGTH_LONG).show();
                i = 0;
                handler2 = new Handler();
                runnable = new Runnable()
                {
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
                Log.i("info", "fail - "+(mfs100.GetErrorMsg(ret)));
            }
            else
            {
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

                    int ret = mfs100.StartCapture(minQuality, timeout, true);
                    if (ret != 0)
                    {
                        SetTextonuiThread(mfs100.GetErrorMsg(ret));
                    }
                    else
                    {
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(
                                fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);

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

                        byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height())+1078];
                        byte[] isoImage = null;
                        int dataLen = mfs100.ExtractISOImage(fingerData.RawData(),tempData);
                        if(dataLen<=0)
                        {
                            if(dataLen==0)
                            {
                                SetTextonuiThread("Failed to extract ISO Image");
                            }
                            else
                            {
                                SetTextonuiThread(mfs100.GetErrorMsg(dataLen));
                            }
                            return;
                        }
                        else
                        {
                            isoImage = new byte[dataLen];
                            System.arraycopy(tempData, 0, isoImage, 0,
                                    dataLen);
                        }

                        //getalldata(fingerData);
                    }
                }
                catch (Exception ex) {
                    SetTextonuiThread("Error");
                }
            }
        }).start();
    }

    private void UnInitScanner()
    {
        try
        {
            int ret = mfs100.UnInit();
            if (ret != 0)
            {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
            }
            else {
                //SetLogOnUIThread("Uninit Success");
                SetTextonuiThread("Uninit Success");
            }
        }
        catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    private void SetTextonuiThread(final String str)
    {
        txt_quality_per.post(new Runnable() {
            public void run() {
                if (str.equalsIgnoreCase("Capture Success"))
                {
                    txt_result.setText("");
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    txt_quality_success.setText(str);
                }
                else if (str.equalsIgnoreCase("Error: -1319(Capturing stopped)"))
                {
                    txt_result.setText("");
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                    txt_quality_success.setVisibility(View.INVISIBLE);
                }
                else if (str.equalsIgnoreCase("Error: -1140(Timeout)"))
                {
                    txt_result.setText("");
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText("Error :- Please press thumb properly");

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Log.i("start", "start");
                            txt_quality_per.setText("0%");
                            progress_quality.setProgress(0);
                            img_in_mark.setVisibility(View.GONE);
                            img_out_mark.setVisibility(View.GONE);
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                }
                else if (str.equalsIgnoreCase("Error"))
                {

                }
                else {
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    txt_quality_per.setText(str+"%");
                }

                Log.i("str",str);

                String regexStr = "^[0-9]*$";
                try
                {
                    int progress = Integer.parseInt(str);
                    progress_quality.setProgress(progress);
                }
                catch (NumberFormatException e) {
                    Log.i(""," is not a number");
                }
            }
        });
    }

    @Override
    public void OnPreview(FingerData fingerData)
    {
        SetTextonuiThread(""+fingerData.Quality());
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData)
    {
        Log.i("capture_cmplt", "capture_cmplt");
        if (status)
        {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(
                    fingerData.FingerImage(), 0,
                    fingerData.FingerImage().length);

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
            Log.i("capture_cmplt_log", log);

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
                } else {
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
                else
                {
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
                    else if (ret == 0)
                    {
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
    public void OnDeviceDetached()
    {
        UnInitScanner();
        SetTextonuiThread("Device removed");
    }

    @Override
    public void OnHostCheckFailed(String err)
    {
        try
        {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
        catch (Exception ex) {

        }
    }

    public void sync_data()
    {
        Log.i("sync_data","sync_data");

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
                    Toast.makeText(AttendanceActivity.this, "No Records Found", Toast.LENGTH_SHORT).show();
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
                        progressDialog = ProgressDialog.show(AttendanceActivity.this, "Please Wait", "Uploading Data... ", true);
                    }

                    new Thread(new Runnable() {
                        public void run() {
                            //sendSignInOutData();
                        }
                    }).start();
                }
            }
            else {
                Toast.makeText(AttendanceActivity.this, "No Records Found", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(AttendanceActivity.this, "No Records Found", Toast.LENGTH_SHORT).show();
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
                try
                {
                    String t1 = "", t2 = "", t3 = "", t4 = "", uid, ucid="", att_type="",ufname="", ulname="", u_mobile_nu="";

                    Log.i("Reading: ", "Reading all contacts..");
                    List<UserDetails_Model> contacts = db.getAllContacts();

                    Log.i("MFS_Log contacts", ""+contacts);

                    result_match = 0;

                    for (UserDetails_Model cn : contacts)
                    {
                        if (result_match <= 1000)
                        {
                            String log = "Id: " + cn.getUid() + " ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();

                            uid = cn.getUid();
                            ucid = cn.getCid();
                            ufname = cn.getFirstname();
                            ulname = cn.getLastname();
                            u_mobile_nu = cn.getMobile_no();
                            att_type = cn.getAttType();
                            Log.i("MFS_Log", log);
                            Log.i("MFS_Log uid", uid);
                            Log.i("MFS_Log ucid", ucid);
                            Log.i("MFS_Log ucid", att_type);

                            Log.i("emp_ext", emp_ext + "");
                            t1 = cn.getThumb1();
                            t2 = cn.getThumb2();
                            t3 = cn.getThumb3();
                            t4 = cn.getThumb4();

                            String thumbs[] = {t1, t2, t3, t4};
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

                                    CaptureBase64 = android.util.Base64.encodeToString(Verify_Template, android.util.Base64.NO_WRAP);
                                    Log.i("CaptureBase64", CaptureBase64);
                                    Log.i("MFS_Log Verify_Template", "" + Verify_Template);

                                    result_match = mfs100.MatchISO(Enroll_Template, Verify_Template);

                                    //sgfplib.MatchIsoTemplate(mRegisterTemplate, 0, mVerifyTemplate, 0, SGFDxSecurityLevel.SL_NORMAL, matched);

                                    Log.i("MFS_Log result_match", "" + result_match);

                                    if (result_match >= 1400)
                                    {
                                        if (att_type.equals("1"))
                                        {
                                            //emp_ext = true;
                                            Log.i("MFS_Log Match", "authorise for thumb!!");
                                            make_offline_attendance(uid, ufname, ulname, u_mobile_nu, Sign_InOut_id);
                                        }
                                        else
                                        {
                                            //emp_ext = true;
                                            txt_result.setText("You are not authorized for Thumb");
                                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                                            textToSpeech.speak("You are not authorized for Thumb!!", TextToSpeech.QUEUE_FLUSH, null);
                                            Log.i("MFS_Log Employee!!", "You are not authorise for thumb!!");

                                            final Handler handler = new Handler();
                                            handler.postDelayed(new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    Log.i("MFS_Log Exist", "You are not authorise for thumb");
                                                    txt_quality_per.setText("0%");
                                                    txt_result.setText("");
                                                    progress_quality.setProgress(0);
                                                    img_in_mark.setVisibility(View.GONE);
                                                    img_out_mark.setVisibility(View.GONE);
                                                    txt_quality_success.setVisibility(View.INVISIBLE);
                                                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                                                }
                                            }, 3000);
                                        }

                                        break;
                                    }
                                    else
                                    {
                                        Log.i("MFS_Log NOT MATCHED!!", "NOT MATCHED!!");
                                    }
                                }
                            }
                        }
                    }

                    if (result_match <= 1000)
                    {
                        txt_result.setText("Sorry thumb not matched");
                        img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                        textToSpeech.speak("Sorry thumb not matched!!", TextToSpeech.QUEUE_FLUSH, null);
                        Log.i("MFS_Log NOT MATCHED!!", "NOT MATCHED!!");

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.i("MFS_Log start", "start");
                                txt_quality_per.setText("0%");
                                txt_result.setText("");
                                progress_quality.setProgress(0);
                                img_in_mark.setVisibility(View.GONE);
                                img_out_mark.setVisibility(View.GONE);
                                txt_quality_success.setVisibility(View.INVISIBLE);
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                            }
                        }, 3000);
                    }

                    //Log.i("emp_ext",emp_ext+"");
                    /*if(!emp_ext)
                    {
                        txt_result.setText("Employee Not Exist!!");
                        textToSpeech.speak("Employee Not Exist!!", TextToSpeech.QUEUE_FLUSH, null);
                        //txt_success.setText("Employee Not Exist!!\n");
                        Log.i("MFS_Log Employee!!", "Employee Not Exist!!");

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Log.i("MFS_Log Exist", "EMP Not Exist");
                                txt_result.setText("");
                                txt_quality_per.setText("0%");
                                progress_quality.setProgress(0);
                                img_in_mark.setVisibility(View.GONE);
                                img_out_mark.setVisibility(View.GONE);
                                txt_quality_success.setVisibility(View.INVISIBLE);
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                            }
                        }, 3000);
                        emp_ext = false;
                    }*/
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
        String empName = firstName +" "+ lastName;

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

        String dt_att = sm.getDate_Time();
        Log.i("dt_att",dt_att);

        if (inout.equals("1"))
        {
            Log.i("if","if");
            db.adddata_signinout(sm);

            List<SigninOut_Model> contacts = db.getSigninoutData(0);
            Log.i("MFS_Log contacts", "" + contacts);
            for (SigninOut_Model cn : contacts)
            {
                String log = "PrimaryKey: " + cn.getPrimaryKey()+", Id: " + cn.getUserId() + ", DateTime: " + cn.getDate_Time() + ", SignInUot: " + cn.getSignInOutId();
                Log.i("MFS_Log", log);
            }

            txt_att_name.setText(empName);
            txt_result.setText("Sign In Successfully");
            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_green));
            textToSpeech.speak("Sign In Successfully", TextToSpeech.QUEUE_FLUSH, null);
            Log.i("MFS_Log Employee!!", "Sign In Successfully!!");
        }
        else if (inout.equals("2"))
        {
            Log.i("else if","else if");
            db.adddata_signinout(sm);

            List<SigninOut_Model> contacts = db.getSigninoutData(0);
            Log.i("MFS_Log contacts", "" + contacts);

            for (SigninOut_Model cn : contacts)
            {
                String log = "PrimaryKey: " + cn.getPrimaryKey()+", Id: " + cn.getUserId() + ", DateTime: " + cn.getDate_Time() + ", SignInUot: " + cn.getSignInOutId();
                Log.i("MFS_Log", log);
            }

            txt_att_name.setText(empName);
            txt_result.setText("Sign Out Successfully");
            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_green));
            textToSpeech.speak("Sign Out Successfully", TextToSpeech.QUEUE_FLUSH, null);
            Log.i("MFS_Log Employee!!", "Sign Out Successfully!!");
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                txt_result.setText("");
                txt_att_name.setText("");
                txt_quality_per.setText("0%");
                progress_quality.setProgress(0);
                img_in_mark.setVisibility(View.GONE);
                img_out_mark.setVisibility(View.GONE);
                txt_quality_success.setVisibility(View.INVISIBLE);
                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
            }
        }, 5000);
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
            int timeoutConnection = 10000;
            int timeoutSocket = 10000;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
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

            prev_key = jsonObject.getInt("primarykey");
            Log.i("prev_key", "" + prev_key);

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
                            Toast.makeText(AttendanceActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                        key_editor = key_pref.edit();
                        key_editor.putInt("key", prev_key);
                        key_editor.commit();
                    }
                });
            }
            else
            {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    Toast.makeText(AttendanceActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                key_editor = key_pref.edit();
                key_editor.putInt("key", prev_key);
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
                        Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            Log.e("Fail 1", e.toString());
        }
    }

    protected void onStop()
    {
        UnInitScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        if (mfs100 != null)
        {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
        if (internetConnection.hasConnection(AttendanceActivity.this))
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InitScanner();
                }
            }).start();
        }
        else {
            //internetConnection.showNetDisabledAlertToUser(AttendanceActivity.this);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed()
    {
        //UnInitScanner();
        mfs100.StopCapture();
        Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }
}
