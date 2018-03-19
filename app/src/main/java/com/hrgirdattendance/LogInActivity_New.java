package com.hrgirdattendance;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;
import com.squareup.picasso.Picasso;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

//import SecuGen.FDxSDKPro.JSGFPLib;
//import SecuGen.FDxSDKPro.SGFDxDeviceName;
//import SecuGen.FDxSDKPro.SGFDxErrorCode;

/**
 * Created by admin on 25-11-2016.
 */
public class LogInActivity_New extends AppCompatActivity implements MFS100Event {
    public static final String MyPREFERENCES_url = "MyPrefs_url";
    public static final String MyPREFERENCES = "MyPrefs";
    SharedPreferences shared_pref;
    int PRIVATE_MODE = 0;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    ConnectionDetector cd;
    UserSessionManager session;
    CheckInternetConnection internetConnection;
    Toolbar toolbar;
    GPSTracker gps;
    ProgressDialog progressDialog;

    String Url, logo;
    String url_http;
    String myJSON = null;
    String UserName, Password;
    String Current_Location = "";
    String android_id;
    String Login_id;
    String RegisteredBase64;

    EditText ed_userName, ed_password;
    TextView txt_forgotPass;
    Button btn_signIn;
    LinearLayout progress_layout;
    ImageView logo_login;
    LinearLayout poweredby_layout;

    LinearLayout login_btns, thumb_login_ll, password_login_ll;
    LinearLayout ll_thumb_login, signIn_layout;
    TextView txt_result, txt_quality_per, txt_quality_success;
    TextView txt_using_otp, txt_password;
    ImageView img_in_mark;
    ProgressBar progress_quality;
    Button btn_thumb_signIn;
    TextToSpeech textToSpeech;

    Double latitude = 0.0, longitude = 0.0;
    int result_match = 0;
    byte[] Enroll_Template;
    byte[] Verify_Template;
    int mfsVer = 41;
    SharedPreferences settings;
    Context context;
    CommonMethod.ScannerAction scannerAction = CommonMethod.ScannerAction.Capture;

    int minQuality = 30;
    int timeout = 5000;//capture time out 10000
    MFS100 mfs100 = null;

    public static String _testKey = "t7L8wTG/iv02t+pgYrMQ7tt8qvU1z42nXpJDfAfsW592N4sKUHLd8A0MEV0GRxH+f4RgefEaMZALj7mgm/Thc0jNhR2CW9BZCTgeDPjC6q0W";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_new);
        context = LogInActivity_New.this.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mfsVer = Integer.parseInt(settings.getString("MFSVer", String.valueOf(mfsVer)));

        PubVar.sharedPrefernceDeviceMode = (SharedPreferences) context.getSharedPreferences(PubVar.strSpDeviceKey, Context.MODE_PRIVATE);

        mfs100 = new MFS100(this, mfsVer);
        mfs100.SetApplicationContext(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar_inner);
        TextView Header = (TextView) findViewById(R.id.header_text);
        ImageView img_logout = (ImageView) findViewById(R.id.img_logout);
        toolbar.setBackgroundColor(getResources().getColor(R.color.GreyBgColor));
        setSupportActionBar(toolbar);

        img_logout.setVisibility(View.GONE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            Header.setText("");
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.abc_ic_ab_back_material));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        internetConnection = new CheckInternetConnection(getApplicationContext());

        Login_id = getIntent().getStringExtra("login_id");

        gps = new GPSTracker(getApplicationContext(), LogInActivity_New.this);
        if (gps.canGetLocation()) {
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            Current_Location = gps.getlocation_Address();

            if (Current_Location == null) {
                Current_Location = "";
            }
        }

        ed_userName = (EditText) findViewById(R.id.ed_userName);
        ed_password = (EditText) findViewById(R.id.ed_password);
        btn_signIn = (Button) findViewById(R.id.btn_signIn);
        progress_layout = (LinearLayout) findViewById(R.id.progress_layout);
        txt_forgotPass = (TextView) findViewById(R.id.forgot_pass);
        poweredby_layout = (LinearLayout) findViewById(R.id.layout_poweredby_login);

        login_btns = (LinearLayout) findViewById(R.id.login_btns);
        thumb_login_ll = (LinearLayout) findViewById(R.id.thumb_login_ll);
        password_login_ll = (LinearLayout) findViewById(R.id.password_login_ll);
        ll_thumb_login = (LinearLayout) findViewById(R.id.ll_thumb_login);
        signIn_layout = (LinearLayout) findViewById(R.id.signIn_layout);

        txt_using_otp = (TextView) findViewById(R.id.txt_using_otp);
        txt_password = (TextView) findViewById(R.id.txt_password);
        txt_result = (TextView) findViewById(R.id.txt_result);
        txt_quality_per = (TextView) findViewById(R.id.txt_quality_per);
        txt_quality_success = (TextView) findViewById(R.id.txt_quality_success);

        img_in_mark = (ImageView) findViewById(R.id.img_in);
        btn_thumb_signIn = (Button) findViewById(R.id.btn_thumb_signIn);

        session = new UserSessionManager(getApplicationContext());
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.changeProtocol();

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));

        logo_login = (ImageView) findViewById(R.id.logo_login);
        Picasso.with(getApplicationContext()).load(logo).into(logo_login);

        progress_quality = (ProgressBar) findViewById(R.id.progressBar_quality);
        progress_quality.setMax(100);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        txt_forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogInActivity_New.this, ForgotPassword.class);
                startActivity(intent);
                finish();
            }
        });

        ed_userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poweredby_layout.setVisibility(View.GONE);
            }
        });

        img_in_mark.setVisibility(View.GONE);
        txt_result.setText("");
        txt_quality_success.setVisibility(View.INVISIBLE);

        thumb_login_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                password_login_ll.setBackgroundResource(0);
                thumb_login_ll.setBackgroundResource(R.drawable.otp_active_btn);
                txt_using_otp.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                txt_password.setTextColor(getResources().getColor(R.color.BlackTextColor));
                ll_thumb_login.setVisibility(View.VISIBLE);
                signIn_layout.setVisibility(View.GONE);
            }
        });

        password_login_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thumb_login_ll.setBackgroundResource(0);
                password_login_ll.setBackgroundResource(R.drawable.password_active_btn);
                txt_using_otp.setTextColor(getResources().getColor(R.color.BlackTextColor));
                txt_password.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                ll_thumb_login.setVisibility(View.GONE);
                signIn_layout.setVisibility(View.VISIBLE);
            }
        });

        btn_thumb_signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_in_mark.setVisibility(View.VISIBLE);
                if (internetConnection.hasConnection(LogInActivity_New.this)) {
                    btn_thumb_signIn.setEnabled(false);
                    //btn_signOut.setEnabled(false);
                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    StartSyncCapture();
                } else {
                    Toast.makeText(LogInActivity_New.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserName = ed_userName.getText().toString();
                Password = ed_password.getText().toString();

                if (internetConnection.hasConnection(getApplicationContext())) {
                    if (UserName.equals("") && Password.equals("")) {
                        ed_userName.setError("Please enter email/mobile");
                        ed_password.setError("Please enter password");
                        txtChange();
                    } else if (UserName.equals("")) {
                        ed_userName.setError("Please enter email/mobile");
                        txtChange();
                    } else if (Password.equals("")) {
                        ed_password.setError("Please enter password");
                        txtChange();
                    } else {
                        signIn();
                    }
                } else {
                    Toast.makeText(LogInActivity_New.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        deviceData();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("requestCode",""+requestCode );//1

        switch (requestCode)
        {
            case 1:
            {
                Log.i("grantResults",""+grantResults.length );
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                {
                    gps = new GPSTracker(getApplicationContext(), LogInActivity_New.this);

                    if (gps.canGetLocation())
                    {
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        Current_Location = gps.getlocation_Address();
                        if (Current_Location == null)
                        {
                            Current_Location = "";
                        }
                    }
                }
                return;
            }
        }
    }

    public void deviceData() {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
    }

    public void txtChange() {
        ed_userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ed_userName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ed_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ed_password.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void signIn()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
               /* progressDialog = new ProgressDialog(LogInActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setTitle("Please wait");
                progressDialog.setMessage("Signing In...");
                progressDialog.show();*/

                progressDialog = ProgressDialog.show(LogInActivity_New.this, "Please wait", "Signing In...", true);
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
                            URLEncoder.encode(Current_Location, "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"),
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
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(LogInActivity_New.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LogInActivity_New.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LogInActivity_New.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("response", result);
                if (response.equals("[]"))
                {
                    progressDialog.dismiss();
                    Toast.makeText(LogInActivity_New.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        JSONObject object = json.getJSONObject(0);
                        String responsecode = object.getString("responseCode");

                        if (responsecode.equals("1"))
                        {
                            progressDialog.dismiss();

                            session.createUserLoginSession(UserName, Password);

                            String uId = object.getString("uId");
                            String firstName = object.getString("firstName");
                            String lastName = object.getString("lastName");
                            String Name = firstName + lastName;
                            String email = object.getString("email");
                            String mobile = object.getString("mobile");
                            String subuserid = object.getString("subuserid");

                            pref = getApplicationContext().getSharedPreferences(MyPREFERENCES, PRIVATE_MODE);
                            editor = pref.edit();
                            editor.putString("password", Password);
                            editor.putString("uId", uId);
                            editor.commit();

                            if (Login_id.equals("1"))
                            {
                                Intent intent = new Intent(LogInActivity_New.this, RegistrationActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else
                            {
                                Intent intent = new Intent(LogInActivity_New.this, ResetThumbActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                        else
                        {
                            progressDialog.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(LogInActivity_New.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
    protected void onResume() {
        super.onResume();
        //IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //registerReceiver(receiver, filter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                InitScanner();
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        mfs100.StopCapture();
        Intent intent = new Intent(LogInActivity_New.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    Handler handler2;
    Runnable runnable;
    int i = 0;

    public void onControlClicked(View v) {
        switch (v.getId()) {
            case R.id.btnForLoop:
                Toast.makeText(LogInActivity_New.this, "Loop for init->uninit->init... 500 times", Toast.LENGTH_LONG).show();
                i = 0;
                handler2 = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        // Log.e("1", ""+ (i+1));
                        if (i >= 500) {
                            handler2.removeCallbacks(runnable);
                        } else {
                            if (i % 2 == 0) {
                                InitScanner();
                            } else {
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

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
                Log.i("info", "fail - " + (mfs100.GetErrorMsg(ret)));
            } else {
                SetTextonuiThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                Log.i("info", info);
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Init failed, unhandled exception", Toast.LENGTH_LONG).show();
            SetTextonuiThread("Init failed, unhandled exception");
        }
    }

    private void StartSyncCapture() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SetTextonuiThread("");
                try {
                    FingerData fingerData = new FingerData();

                    int ret = mfs100.StartCapture(minQuality, timeout, true);
                    if (ret != 0) {
                        SetTextonuiThread(mfs100.GetErrorMsg(ret));
                    } else {
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
                        //SetLogOnUIThread(log);

                        //////////////////// Extract ISO Image
                        byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
                        byte[] isoImage = null;
                        int dataLen = mfs100.ExtractISOImage(fingerData.RawData(), tempData);
                        if (dataLen <= 0) {
                            if (dataLen == 0) {
                                SetTextonuiThread("Failed to extract ISO Image");
                            } else {
                                SetTextonuiThread(mfs100.GetErrorMsg(dataLen));
                            }
                            return;
                        } else {
                            isoImage = new byte[dataLen];
                            System.arraycopy(tempData, 0, isoImage, 0,
                                    dataLen);
                        }

                        //getThumbExpression(fingerData);
                        //SetData2(fingerData,ansiTemplate,isoImage,wsqImage);
                    }
                } catch (Exception ex) {
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
        txt_quality_per.post(new Runnable() {
            public void run() {
                if (str.equalsIgnoreCase("Capture Success")) {
                    btn_signIn.setEnabled(true);
                    //btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    txt_quality_success.setText(str);
                } else if (str.equalsIgnoreCase("Error: -1140(Timeout)")) {
                    btn_signIn.setEnabled(true);
                    //btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText("Please press thumb properly");
                    img_in_mark.setVisibility(View.GONE);
                    //img_out_mark.setVisibility(View.GONE);
                    mfs100.StopCapture();
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                            progress_quality.getProgressDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.DST);
                        }
                    }, 2000);
                } else if (str.equalsIgnoreCase("No Device Connected")) {
                    btn_signIn.setEnabled(true);
                    //btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText(str);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_in_mark.setVisibility(View.GONE);
                    //img_out_mark.setVisibility(View.GONE);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mfs100.StopCapture();
                            Log.i("start", "start");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                } else if (str.equalsIgnoreCase("Permission denied")) {
                    btn_signIn.setEnabled(true);
                    //btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText(str);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_in_mark.setVisibility(View.GONE);
                    //img_out_mark.setVisibility(View.GONE);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mfs100.StopCapture();
                            Log.i("start", "start");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                } else if (str.equalsIgnoreCase("Device removed")) {
                    btn_signIn.setEnabled(true);
                    //btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText(str);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_in_mark.setVisibility(View.GONE);
                    //img_out_mark.setVisibility(View.GONE);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mfs100.StopCapture();
                            Log.i("start", "start");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                } else if (str.contains("Error: -1319(Capturing stopped)")) {
                    btn_signIn.setEnabled(true);
                    // btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                } else if (str.contains("Error")) {
                    txt_result.setText("");
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    //img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                } else {
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    txt_quality_per.setText(str + "%");
                }

                Log.i("str", str);

                String regexStr = "^[0-9]*$";
                try {
                    int progress = Integer.parseInt(str);
                    progress_quality.setProgress(progress);
                    if (progress < minQuality) {
                        progress_quality.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.ADD);
                    } else {
                        progress_quality.getProgressDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.DST);
                    }
                } catch (NumberFormatException e) {
                    Log.i("", " is not a number");
                }
            }
        });
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {
        int ret = 0;
        if (!hasPermission) {
            SetTextonuiThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279) {
            if (pid == 34323) {
                ret = mfs100.LoadFirmware();
                if (ret != 0) {
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                } else {
                    SetTextonuiThread("Loadfirmware success");
                }
            } else if (pid == 4101) {
                //Added by Milan Sheth on 19-Dec-2016
                String strDeviceMode = PubVar.sharedPrefernceDeviceMode.getString(PubVar.strSpDeviceKey, "public");
                if (strDeviceMode.toLowerCase().equalsIgnoreCase("public")) {
                    ret = mfs100.Init("");
                    if (ret == -1322) {
                        ret = mfs100.Init(_testKey);
                        if (ret == 0) {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                            //showSuccessLog();
                        }
                    } else if (ret == 0) {
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                        //showSuccessLog();
                    }
                } else {
                    ret = mfs100.Init(_testKey);
                    if (ret == -1322) {
                        ret = mfs100.Init("");
                        if (ret == 0) {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                            //showSuccessLog();
                        }
                    } else if (ret == 0) {
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                        //showSuccessLog();
                    }
                }

                if (ret != 0) {
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                }
            }
        }
    }

    @Override
    public void OnPreview(FingerData fingerData) {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0, fingerData.FingerImage().length);

        // Log.e("OnPreview.Quality", String.valueOf(fingerData.Quality()));
        SetTextonuiThread("" + fingerData.Quality());
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData)
    {
        Log.i("capture_cmplt", "capture_cmplt");
        if (status) {
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

            getThumbExpression(fingerData);

        } else {
            SetTextonuiThread("Error: " + errorCode + "(" + errorMsg + ")");
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
        } catch (Exception ex) {

        }
    }

    public void MatchThumb(final FingerData fingerData)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<UserDetails_Model> contacts = null;
                Log.i("MFS_Log contacts", "" + contacts);

                result_match = 0;

                for (UserDetails_Model cn : contacts) {
                    if (result_match < 800) {
                        String t1 = cn.getThumb1();
                        String t2 = cn.getThumb2();
                        String t3 = cn.getThumb3();
                        String t4 = cn.getThumb4();

                        String thumbs[] = {t1, t2, t3, t4};
                        Log.i("thumbs", "" + Arrays.toString(thumbs));

                        for (int i = 0; i < thumbs.length; i++) {
                            if (result_match < 800) {
                                RegisteredBase64 = thumbs[i];
                                if (RegisteredBase64 != null) {
                                    Log.i("MFS_ RegisteredBase64", RegisteredBase64);

                                    String EmpId = cn.getUid();
                                    Log.i("MFS_Log EmpId", EmpId);

                                    Enroll_Template = Base64.decode(RegisteredBase64, Base64.DEFAULT);
                                    Log.i("MFS_Log Enroll_Template", "" + Enroll_Template);

                                    Verify_Template = new byte[fingerData.ISOTemplate().length];
                                    System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                                            fingerData.ISOTemplate().length);

                                    String CaptureBase64 = Base64.encodeToString(Verify_Template, Base64.NO_WRAP);
                                    Log.i("CaptureBase64", CaptureBase64);

                                    Log.i("MFS_Log Verify_Template", "" + Verify_Template);

                                    result_match = mfs100.MatchISO(Enroll_Template, Verify_Template);
                                    Log.i("MFS_Log result_match", "" + result_match);

                                    if (result_match >= 800) {
                                        Log.i("MFS_Log MATCHED!!", "MATCHED!!");
                                        //makeAttendance();
                                        break;
                                    } else {
                                        Log.i("MFS_Log NOT MATCHED!!", "NOT MATCHED!!");
                                    }
                                }
                            }
                        }
                    } else {
                        break;
                    }
                }

                if (result_match < 800) {
                    btn_thumb_signIn.setEnabled(true);
                    img_in_mark.setVisibility(View.GONE);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    txt_result.setText("Sorry thumb not matched");
                    txt_result.setTextColor(getResources().getColor(R.color.RedTextColor));
                    textToSpeech.speak("Sorry thumb not matched!!", TextToSpeech.QUEUE_FLUSH, null);
                    Log.i("THUMB NOT MATCHED!!", "THUMB NOT MATCHED!!");

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            txt_result.setText("");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                        }
                    }, 3000);
                }
            }
        });
    }

    public void getThumbExpression(final FingerData fingerData)
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                        String Transurl = "" + url_http + "" + Url + "/owner/hrmapi/getallempthumbdata";

                    url = new URL(Transurl);
                    Log.i("url", "" + url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    int responseCode = conn.getResponseCode();
                    Log.i("responseCode", "" + responseCode);

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null) {
                            response += line;
                        }
                    } else {
                        response = "";
                    }
                } catch (SocketTimeoutException e1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LogInActivity_New.this, "Slow internet conncetion", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", "" + e1.toString());
                } catch (ConnectTimeoutException e1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LogInActivity_New.this, "Slow internet conncetion", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", "" + e1.toString());
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LogInActivity_New.this, "Slow internet conncetion", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", "" + e.toString());
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("result", "" + result);
                if (result.equals("[]"))
                {
                    Toast.makeText(LogInActivity_New.this, "Bad internet connection", Toast.LENGTH_LONG).show();
                }
                else
                    {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);
                        result_match = 0;

                        for (int j = 0; j < json.length(); j++)
                        {
                            if (result_match <= 800)
                            {
                                JSONObject jsonObj = json.getJSONObject(j);

                                try
                                {
                                    JSONArray array = jsonObj.getJSONArray("Thumexp");
                                    Log.i("array", "" + array);

                                    for (int i = 0; i < array.length(); i++)
                                    {
                                        JSONObject object = array.getJSONObject(i);
                                        //Log.i("object", ""+object);
                                        if (result_match <= 800)
                                        {
                                            RegisteredBase64 = object.getString("thumb");
                                            Log.i("RegisteredBase64", RegisteredBase64);

                                            String EmpId = jsonObj.getString("uId");
                                            Log.i("EmpId", EmpId);

                                            Enroll_Template = Base64.decode(RegisteredBase64, Base64.DEFAULT);
                                            Log.i("Enroll_Template", "" + Enroll_Template);

                                            Verify_Template = new byte[fingerData.ISOTemplate().length];
                                            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                                                    fingerData.ISOTemplate().length);

                                            result_match = mfs100.MatchISO(Enroll_Template, Verify_Template);

                                            Log.i("result_match", "" + result_match);

                                            if (result_match >= 800)
                                            {
                                                Log.i("match_result_match", "" + result_match);
                                                btn_thumb_signIn.setEnabled(true);
                                                img_in_mark.setVisibility(View.GONE);
                                                txt_quality_per.setText("0%");
                                                progress_quality.setProgress(0);
                                                txt_result.setText("Login Successfully");
                                                txt_result.setTextColor(getResources().getColor(R.color.GreenColor));
                                                textToSpeech.speak("Login Successfully", TextToSpeech.QUEUE_FLUSH, null);
                                                Log.i("Login Successfully", "Login Successfully");
                                                ThumbSignIn(EmpId);
                                                break;
                                            }
                                            else {
                                                Log.i("NOT MATCHED!!", "NOT MATCHED!!");
                                            }
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (result_match <= 800)
                        {
                            btn_thumb_signIn.setEnabled(true);
                            img_in_mark.setVisibility(View.GONE);
                            txt_quality_per.setText("0%");
                            progress_quality.setProgress(0);
                            txt_result.setText("Sorry thumb not matched");
                            txt_result.setTextColor(getResources().getColor(R.color.RedTextColor));
                            textToSpeech.speak("Sorry thumb not matched!!", TextToSpeech.QUEUE_FLUSH, null);
                            Log.i("THUMB NOT MATCHED!!", "THUMB NOT MATCHED!!");

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    txt_result.setText("");
                                    txt_quality_success.setVisibility(View.INVISIBLE);
                                }
                            }, 3000);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public void ThumbSignIn(final String EmpId)
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(LogInActivity_New.this, "Please wait", "Signing In...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/signInwithdeviceidthumboffline/?";

                    String query = String.format("uId=%s&android_devide_id=%s&devicelocation=%s&signinby=%s",
                            URLEncoder.encode(EmpId, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode(Current_Location, "UTF-8"),
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
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(LogInActivity_New.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LogInActivity_New.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(LogInActivity_New.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("response", result);
                if (response.equals("[]"))
                {
                    progressDialog.dismiss();
                    Toast.makeText(LogInActivity_New.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        JSONObject object = json.getJSONObject(0);
                        String responsecode = object.getString("responseCode");
                        progressDialog.dismiss();

                        if (responsecode.equals("1"))
                        {
                            session.createUserLoginSession("", "");

                            /*String uId = object.getString("uId");
                            String firstName = object.getString("firstName");
                            String lastName = object.getString("lastName");
                            String Name = firstName + lastName;
                            String email = object.getString("email");
                            String mobile = object.getString("mobile");
                            String subuserid = object.getString("subuserid");*/

                            if (Login_id.equals("1"))
                            {
                                Intent intent = new Intent(LogInActivity_New.this, RegistrationActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else
                            {
                                Intent intent = new Intent(LogInActivity_New.this, ResetThumbActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                        else
                        {
                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(LogInActivity_New.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
}
