package com.hrgirdattendance;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class ResetThumbActivity extends AppCompatActivity implements MFS100Event {

    byte[] Enroll_Template;
    byte[] Verify_Template;
    int mfsVer = 41;
    SharedPreferences settings;
    Context context;
    CommonMethod.ScannerAction scannerAction = CommonMethod.ScannerAction.Capture;

    int minQuality = 50;
    int timeout = 5000;
    MFS100 mfs100 = null;

    public static String _testKey = "t7L8wTG/iv02t+pgYrMQ7tt8qvU1z42nXpJDfAfsW592N4sKUHLd8A0MEV0GRxH+f4RgefEaMZALj7mgm/Thc0jNhR2CW9BZCTgeDPjC6q0W";

    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    public static final String MyPREFERENCES = "MyPrefs" ;
    SharedPreferences shared_pref;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    public static NetworkChange receiver;
    CheckInternetConnection internetConnection;
    ConnectionDetector cd;
    UserSessionManager session;
    TextToSpeech textToSpeech;
    DatabaseHandler db;
    Toolbar toolbar;
    ProgressDialog progressDialog;

    List<String> RegisteredThumbs;

    Bitmap bitmap,bitmap1;

    String myJSON = null;
    String RegisteredBase64_1 = null, RegisteredBase64_2 = null, RegisteredBase64_3 = null, RegisteredBase64_4 = null;
    String emp_id, cid, MobileNo;
    String str_RegisteredThumbs;
    String Url, url_http;
    String android_id, logout_id = "0";

    EditText ed_MobNo;
    TextView txt_empName, txt_empId;
    ImageView img_register1, img_register2, img_register3, img_register4;
    ImageView img_check1, img_check2, img_check3, img_check4;
    Button btn_ViewDetails;
    LinearLayout reg_logout, progress_layout;
    EditText ed_regLogout;
    Button btn_regLogout, btn_resetThumb, btn_nextThumb;
    Snackbar snackbar;
    CoordinatorLayout snackbarCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_thumb);

        context = ResetThumbActivity.this.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mfsVer = Integer.parseInt(settings.getString("MFSVer", String.valueOf(mfsVer)));

        PubVar.sharedPrefernceDeviceMode = (SharedPreferences) context.getSharedPreferences(PubVar.strSpDeviceKey, Context.MODE_PRIVATE);

        mfs100 = new MFS100(this, mfsVer);
        mfs100.SetApplicationContext(this);

        db = new DatabaseHandler(this);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner);
        TextView Header = (TextView)findViewById(R.id.header_text);
        ImageView img_logout = (ImageView)findViewById(R.id.img_logout);
        setSupportActionBar(toolbar);

        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.changeProtocol();

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());

        pref = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE);

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));

        if (!internetConnection.hasConnection(ResetThumbActivity.this))
        {
            internetConnection.showNetDisabledAlertToUser(ResetThumbActivity.this);
        }

        Initialisation();
        deviceData();

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Header.setText("RESET THUMB");
            img_logout.setVisibility(View.VISIBLE);
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
                    snackbar = Snackbar.make(snackbarCoordinatorLayout, "Please check your internet connection", Snackbar.LENGTH_INDEFINITE);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                    snackbar.show();
                }
            }
        };


        img_logout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setTitle("Logout / Stop");
                alertDialog.setMessage("Do you want to Logout / Stop capture");
                alertDialog.setCancelable(true);
                alertDialog.setPositiveButton("Stop Capture", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mfs100.StopCapture();
                    }
                });
                alertDialog.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout_id = "1";
                        editor = pref.edit();
                        editor.clear();
                        editor.commit();
                        session.logoutUser();
                        UnInitScanner();

                        Intent intent = new Intent(ResetThumbActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });

                alertDialog.show();
            }
        });

    }

    public void Initialisation()
    {
        reg_logout = (LinearLayout)findViewById(R.id.reset_logout);
        ed_regLogout = (EditText)findViewById(R.id.ed_reset_logout);
        btn_regLogout = (Button)findViewById(R.id.btn_reset_logout);
        snackbarCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.snackbarCoordinatorLayout_reset);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout_reset_tmb);

        ed_MobNo = (EditText)findViewById(R.id.ed_reset_mobNo);
        txt_empName = (TextView)findViewById(R.id.txt_reset_empName);
        txt_empId = (TextView)findViewById(R.id.txt_reset_empId);
        img_register1 = (ImageView)findViewById(R.id.img_reset_finger1);
        img_register2 = (ImageView)findViewById(R.id.img_reset_finger2);
        img_register3 = (ImageView)findViewById(R.id.img_reset_finger3);
        img_register4 = (ImageView)findViewById(R.id.img_reset_finger4);
        btn_ViewDetails = (Button)findViewById(R.id.btn_reset);
        btn_resetThumb = (Button)findViewById(R.id.btn_reset_thumb1);
        btn_nextThumb = (Button)findViewById(R.id.btn_reset_nxtthumb);

        img_check1 = (ImageView)findViewById(R.id.imagecheckreset1);
        img_check2 = (ImageView)findViewById(R.id.imagecheckreset2);
        img_check3 = (ImageView)findViewById(R.id.imagecheckreset3);
        img_check4 = (ImageView)findViewById(R.id.imagecheckreset4);

        btn_ViewDetails.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InitScanner();
                    }
                }).start();

                btn_nextThumb.setBackgroundDrawable(getResources().getDrawable(R.drawable.login_button));
                btn_nextThumb.setEnabled(true);

                MobileNo = ed_MobNo.getText().toString();
                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (!ed_MobNo.getText().toString().equals(""))
                    {
                        if (MobileNo.length() > 9)
                        {
                            txt_empId.setText("");
                            txt_empName.setText("");
                            img_register1.setImageResource(R.drawable.imagefinger);
                            img_register2.setImageResource(R.drawable.imagefinger);
                            img_register3.setImageResource(R.drawable.imagefinger);
                            img_register4.setImageResource(R.drawable.imagefinger);
                            RegisteredBase64_1 = null;
                            RegisteredBase64_2 = null;
                            RegisteredBase64_3 = null;
                            RegisteredBase64_4 = null;
                            str_RegisteredThumbs = "";
                            //RegisteredThumbs.clear();

                            img_check1.setVisibility(View.INVISIBLE);
                            img_check2.setVisibility(View.INVISIBLE);
                            img_check3.setVisibility(View.INVISIBLE);
                            img_check4.setVisibility(View.INVISIBLE);

                            InputMethodManager in = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            in.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);

                            getEmpDetails();
                        }
                        else
                        {
                            ed_MobNo.setError("Mobile no. must be greater than 9 digits");
                        }
                    }
                    else
                    {
                        ed_MobNo.setError("Please enter mobile no.");
                    }
                }
                else
                {
                    Toast.makeText(ResetThumbActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_resetThumb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (RegisteredBase64_1 != null && RegisteredBase64_2 != null)
                    {
                        MobileNo = ed_MobNo.getText().toString();

                        if (RegisteredBase64_3 != null && RegisteredBase64_4 != null)
                        {
                            str_RegisteredThumbs = RegisteredBase64_1 + ", " + RegisteredBase64_2 + ", " + RegisteredBase64_3 + ", " + RegisteredBase64_4;
                        }
                        else if (RegisteredBase64_3 != null)
                        {
                            str_RegisteredThumbs = RegisteredBase64_1 + ", " + RegisteredBase64_2 + ", " + RegisteredBase64_3;
                        }
                        else
                        {
                            str_RegisteredThumbs = RegisteredBase64_1 + ", " + RegisteredBase64_2;
                        }

                        resetThumbRegistration();
                        RegisteredThumbs = new ArrayList<String>();
                        RegisteredThumbs.clear();
                        RegisteredThumbs.add(RegisteredBase64_1);
                        RegisteredThumbs.add(RegisteredBase64_2);
                        RegisteredThumbs.add(RegisteredBase64_3);
                        RegisteredThumbs.add(RegisteredBase64_4);
                        Log.i("str_RegisteredThumbs", "" + str_RegisteredThumbs);

                        img_check1.setVisibility(View.INVISIBLE);
                        img_check2.setVisibility(View.INVISIBLE);
                        img_check3.setVisibility(View.INVISIBLE);
                        img_check4.setVisibility(View.INVISIBLE);
                    }
                    else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                        alertDialog.setTitle("Please Register Minimum Two Thumbs");
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
                    Toast.makeText(ResetThumbActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

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
                Log.i("mobNo_length", ""+mobNo.length());

                if (mobNo.length() > 9)
                {
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else
                {
                    mfs100.StopCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        btn_nextThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mfs100.StopCapture();
                if (RegisteredBase64_4 == null)
                {
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    StartSyncCapture();
                }
            }
        });

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
                Toast.makeText(ResetThumbActivity.this,
                        "Loop for init->uninit->init... 500 times",
                        Toast.LENGTH_LONG).show();
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
                            if (i % 2 == 0)
                            {
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

    private void StartAsyncCapture()
    {
        SetTextonuiThread("");

        try
        {
            int ret = mfs100.StartCapture(minQuality, timeout, true);
            if (ret != 0)
            {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
            }
            else {
                SetTextonuiThread("Place finger on scanner");
            }
        }
        catch (Exception ex) {
            SetTextonuiThread("Error");
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
                        Log.i("SetTextonuiThread", ""+mfs100.GetErrorMsg(ret));
                    }
                    else
                    {
                        //bitmap = null;
                        bitmap = BitmapFactory.decodeByteArray(
                                fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);
                        Log.i("bitmap", ""+bitmap);

                        if (RegisteredBase64_1 == null)
                        {
                            img_register1.post(new Runnable() {
                                @Override
                                public void run() {
                                    img_register1.setImageBitmap(bitmap);
                                }
                            });
                        }
                        else  if (RegisteredBase64_2 == null)
                        {
                            img_register2.post(new Runnable() {
                                @Override
                                public void run() {
                                    img_register2.setImageBitmap(bitmap);
                                }
                            });
                        }
                        else  if (RegisteredBase64_3 == null)
                        {
                            img_register3.post(new Runnable() {
                                @Override
                                public void run() {
                                    img_register3.setImageBitmap(bitmap);
                                }
                            });
                        }
                        else
                        {
                            img_register4.post(new Runnable() {
                                @Override
                                public void run() {
                                    img_register4.setImageBitmap(bitmap);
                                }
                            });
                        }

                        SetTextonuiThread("Capture Success");
                        Log.i("Capture Success", "Capture Success");
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
                        byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height())+1078];
                        byte[] isoImage = null;
                        int dataLen = mfs100.ExtractISOImage(fingerData.RawData(),tempData);
                        Log.i("dataLen", "dataLen");
                        Log.i("dataLen", ""+dataLen);
                        if(dataLen<=0)
                        {
                            if(dataLen==0)
                            {
                                SetTextonuiThread("Failed to extract ISO Image");
                                Log.i("ailed to extract", "Failed to extract");
                            }
                            else
                            {
                                SetTextonuiThread(mfs100.GetErrorMsg(dataLen));
                                Log.i("Capture Fail", ""+mfs100.GetErrorMsg(dataLen));
                            }
                            return;
                        }
                        else
                        {
                            isoImage = new byte[dataLen];
                            System.arraycopy(tempData, 0, isoImage, 0, dataLen);

                            Log.i("isoImage", "isoImage");
                            Log.i("isoImage", ""+isoImage);
                        }

                        SetData(fingerData);
                        //getThumbExpression(fingerData);
                        //SetData2(fingerData,ansiTemplate,isoImage,wsqImage);
                    }
                } catch (Exception ex) {
                    SetTextonuiThread("Error");
                    Log.i("Error", ""+ex);
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
            else
            {
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
        /*lblMessage.post(new Runnable() {
            public void run() {
                lblMessage.setText(str);
            }
        });*/
    }

    @Override
    public void OnPreview(FingerData fingerData)
    {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(
                fingerData.FingerImage(), 0, fingerData.FingerImage().length);

        if (RegisteredBase64_1 == null)
        {
            img_register1.post(new Runnable() {
                @Override
                public void run() {
                    img_register1.setImageBitmap(bitmap);
                    //img_register1.refreshDrawableState();
                    //img_check1.setVisibility(View.VISIBLE);

                }
            });
        }
        else if (RegisteredBase64_2 == null)
        {
            img_register2.post(new Runnable() {
                @Override
                public void run() {
                    img_register2.setImageBitmap(bitmap);
                    //img_register2.refreshDrawableState();
                    //img_check2.setVisibility(View.VISIBLE);

                }
            });
        }
        else if (RegisteredBase64_3 == null)
        {
            img_register3.post(new Runnable() {
                @Override
                public void run() {
                    img_register3.setImageBitmap(bitmap);
                    //img_register2.refreshDrawableState();
                    //img_check3.setVisibility(View.VISIBLE);

                }
            });
        }
        else
        {
            img_register4.post(new Runnable() {
                @Override
                public void run() {
                    img_register4.setImageBitmap(bitmap);
                    //img_register2.refreshDrawableState();
                    //img_check4.setVisibility(View.VISIBLE);

                }
            });
        }
        // Log.e("OnPreview.Quality", String.valueOf(fingerData.Quality()));
        SetTextonuiThread("Quality: " + fingerData.Quality());
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData)
    {
        Log.i("capture_cmplt", "capture_cmplt");
        if (status)
        {
            bitmap1 = BitmapFactory.decodeByteArray(
                    fingerData.FingerImage(), 0,
                    fingerData.FingerImage().length);

            if (RegisteredBase64_1 == null)
            {
                img_check1.post(new Runnable() {
                    @Override
                    public void run() {
                        img_check1.setVisibility(View.VISIBLE);
                    }
                });
            }
            else if (RegisteredBase64_2 == null)
            {
                img_check2.post(new Runnable() {
                    @Override
                    public void run() {
                        img_check2.setVisibility(View.VISIBLE);
                    }
                });
            }
            else if (RegisteredBase64_3 == null)
            {
                img_check3.post(new Runnable() {
                    @Override
                    public void run() {
                        img_check3.setVisibility(View.VISIBLE);
                    }
                });
            }
            else
            {
                img_check4.post(new Runnable() {
                    @Override
                    public void run() {
                        img_check4.setVisibility(View.VISIBLE);
                        btn_nextThumb.setBackgroundDrawable(getResources().getDrawable(R.drawable.login_button_disabled));
                        btn_nextThumb.setEnabled(false);
                    }
                });
            }

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
            Log.i("capture_cmplt_log", log);

            SetData(fingerData);
            //getThumbExpression(fingerData);
        }
        else {
            SetTextonuiThread("Error: " + errorCode + "(" + errorMsg + ")");
        }
    }

    public void SetData(FingerData fingerData)
    {
        //Log.i("RegisteredBase64_1", RegisteredBase64_1);

        if (scannerAction.equals(CommonMethod.ScannerAction.Capture))
        {
            if (RegisteredBase64_1 == null)
            {
                Enroll_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0, fingerData.ISOTemplate().length);
                //Log.i("Enroll_Template1", Arrays.toString(Enroll_Template));
                RegisteredBase64_1 = Base64.encodeToString(Enroll_Template, Base64.DEFAULT);
                Log.i("RegisteredBase64_1", RegisteredBase64_1);
            }
            else  if (RegisteredBase64_2 == null)
            {
                Enroll_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0, fingerData.ISOTemplate().length);
                //Log.i("Enroll_Template2", Arrays.toString(Enroll_Template));
                RegisteredBase64_2 = Base64.encodeToString(Enroll_Template, Base64.DEFAULT);
                Log.i("RegisteredBase64_2", RegisteredBase64_2);
            }
            else  if (RegisteredBase64_3 == null)
            {
                Enroll_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0, fingerData.ISOTemplate().length);
                //Log.i("Enroll_Template3", Arrays.toString(Enroll_Template));
                RegisteredBase64_3 = Base64.encodeToString(Enroll_Template, Base64.DEFAULT);
                Log.i("RegisteredBase64_3", RegisteredBase64_3);
            }
            else
            {
                Enroll_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0, fingerData.ISOTemplate().length);
                //Log.i("Enroll_Template4", Arrays.toString(Enroll_Template));
                RegisteredBase64_4 = Base64.encodeToString(Enroll_Template, Base64.DEFAULT);
                Log.i("RegisteredBase64_4", RegisteredBase64_4);
            }
        }
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission)
    {
        int ret = 0;
        if (!hasPermission)
        {
            Log.i("Permission denied","Permission denied");
            SetTextonuiThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279)
        {
            Log.i("Permission denied1","Permission denied1");
            if (pid == 34323)
            {
                ret = mfs100.LoadFirmware();
                if (ret != 0)
                {
                    Log.i("Permission denied",""+mfs100.GetErrorMsg(ret));
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                }
                else
                {
                    SetTextonuiThread("Loadfirmware success");
                    Log.i("Loadfirmware success","Loadfirmware success");
                }
            }
            else if (pid == 4101)
            {

                //Added by Milan Sheth on 19-Dec-2016
                String strDeviceMode = PubVar.sharedPrefernceDeviceMode.getString(PubVar.strSpDeviceKey, "public");

                Log.i("strDeviceMode","strDeviceMode");
                Log.i("strDeviceMode",strDeviceMode);
                if (strDeviceMode.toLowerCase().equalsIgnoreCase("public"))
                {
                    ret = mfs100.Init("");
                    if (ret == -1322)
                    {
                        Log.i("strDeviceMode1","strDeviceMode1");
                        ret = mfs100.Init(_testKey);
                        if (ret == 0)
                        {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                            //showSuccessLog();
                        }
                    }
                    else if (ret == 0)
                    {
                        Log.i("strDeviceMode2","strDeviceMode2");
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                        //showSuccessLog();
                    }
                }
                else
                {
                    Log.i("strDeviceMode3","strDeviceMode3");
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
                    Log.i("strDeviceMode5",""+mfs100.GetErrorMsg(ret));
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
            //SetLogOnUIThread(err);
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
        catch (Exception ex) {

        }
    }

    public void getEmpDetailsOld()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(ResetThumbActivity.this, "Please wait", "Getting Employee Details...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/resetempdetails/?";

                    String query = String.format("mobile=%s", URLEncoder.encode(MobileNo, "UTF-8"));
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
                if (progressDialog!=null && progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }

                myJSON = result;
                Log.i("response", result);

                if (response.equals("[]"))
                {
                    ed_MobNo.setText("");
                    ed_MobNo.setError("Wrong mobile no.");
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);
//[{"firstName":"Tazzim","lastName":"Khan","uId":142,"responsecode":1,"msg":"Employee details"}]
                        String responsecode = object.getString("responsecode");

                        if (responsecode.equals("1"))
                        {
                            String emp_firstname = object.getString("firstName");
                            String emp_lastname = object.getString("lastName");
                            String emp_name = emp_firstname + " " + emp_lastname;
                            emp_id = object.getString("uId");
                            cid = object.getString("cid");

                            txt_empName.setText(emp_name);
                            //txt_empId.setText(emp_id);
                            txt_empId.setText(cid);

                            scannerAction = CommonMethod.ScannerAction.Capture;
                            StartSyncCapture();

                            /*final Handler handler = new Handler();
                            handler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Log.i("start", "start");
                                    scannerAction = CommonMethod.ScannerAction.Capture;
                                    StartSyncCapture();
                                }
                            }, 6000);*/
                        }
                        else if (responsecode.equals("2"))
                        {
                            String msg = object.getString("msg");
                            String message = msg.substring(2, msg.length()-2);
                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    img_register1.setImageResource(R.drawable.imagefinger);
                                    img_register2.setImageResource(R.drawable.imagefinger);
                                    img_register3.setImageResource(R.drawable.imagefinger);
                                    img_register4.setImageResource(R.drawable.imagefinger);

                                    txt_empName.setText("");
                                    txt_empId.setText("");
                                    ed_MobNo.setText("");
                                }
                            });
                            alertDialog.show();
                        }
                        else if (responsecode.equals("0"))
                        {
                            String msg = object.getString("msg");
                            String message = msg.substring(2, msg.length()-2);

                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ed_MobNo.setText("");
                                }
                            });
                            alertDialog.show();
                        }
                    }
                    catch (JSONException e){
                        Toast.makeText(ResetThumbActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                        Log.e("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public void resetThumbRegistrationOld()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute() {
                //progressDialog1 = ProgressDialog.show(ResetThumbActivity.this, "Please wait", "Resetting thumb...", true);
                //progressDialog1.show();
                //progress_layout.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/resetthum/?";

                    String query = String.format("empId=%s&thumexp=%s", URLEncoder.encode(emp_id, "UTF-8"), URLEncoder.encode(str_RegisteredThumbs, "UTF-8"));
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
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("response", result);

                //progress_layout.setVisibility(View.GONE);

                if (response.equals("[]"))
                {
                    Toast.makeText(ResetThumbActivity.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                }
                else
                {
//                    db.addEmpData(new UserDetails_Model(null,get_uId,get_cid,get_attType,get_firstName,get_lastName,get_mobile,t1,t2,t3,t4));
                    db.UpdateEmpData(new UserDetails_Model(RegisteredBase64_1,RegisteredBase64_2,RegisteredBase64_3,RegisteredBase64_4), MobileNo);
                    Toast.makeText(ResetThumbActivity.this, "Thumbs Registered Successfully", Toast.LENGTH_LONG).show();
                    textToSpeech.speak("Thumbs Registered Successfully!", TextToSpeech.QUEUE_FLUSH, null);

                    img_register1.setImageResource(R.drawable.imagefinger);
                    img_register2.setImageResource(R.drawable.imagefinger);
                    img_register3.setImageResource(R.drawable.imagefinger);
                    img_register4.setImageResource(R.drawable.imagefinger);
                    txt_empName.setText("");
                    txt_empId.setText("");
                    ed_MobNo.setText("");

                    RegisteredBase64_1 = null;
                    RegisteredBase64_2 = null;
                    RegisteredBase64_3 = null;
                    RegisteredBase64_4 = null;
                    str_RegisteredThumbs = "";
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public void getEmpDetails()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(ResetThumbActivity.this, "Please wait", "Getting Employee Details...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/resetempdetails/?";

                    String query = String.format("mobile=%s", URLEncoder.encode(MobileNo, "UTF-8"));
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
                            progressDialog.dismiss();
                            Toast.makeText(ResetThumbActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ResetThumbActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ResetThumbActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (progressDialog != null && progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }

                myJSON = result;
                Log.i("response", result);

                if (response.equals("[]"))
                {
                    ed_MobNo.setText("");
                    ed_MobNo.setError("Wrong mobile no.");
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);

                        String responsecode = object.getString("responsecode");

                        if (responsecode.equals("1"))
                        {
                            String emp_firstname = object.getString("firstName");
                            String emp_lastname = object.getString("lastName");
                            String emp_name = emp_firstname + " " + emp_lastname;
                            emp_id = object.getString("uId");
                            String cid = object.getString("cid");

                            if (db.checkEmpId(emp_id))
                            {
                                Log.i("true", ""+db.checkEmpId(emp_id));
                                txt_empName.setText(emp_name);
                                txt_empId.setText(cid);
                                scannerAction = CommonMethod.ScannerAction.Capture;
                                StartSyncCapture();
                            }
                            else
                            {
                                Log.i("false", ""+db.checkEmpId(emp_id));
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setTitle("EMP Thumbs not registered with this device");
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        ed_MobNo.setText("");
                                    }
                                });
                                alertDialog.show();
                            }
                        }
                        else if (responsecode.equals("2"))
                        {
                            String msg = object.getString("msg");
                            String message = msg.substring(2, msg.length()-2);
                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    img_register1.setImageResource(R.drawable.imagefinger);
                                    img_register2.setImageResource(R.drawable.imagefinger);
                                    img_register3.setImageResource(R.drawable.imagefinger);
                                    img_register4.setImageResource(R.drawable.imagefinger);

                                    txt_empName.setText("");
                                    txt_empId.setText("");
                                    ed_MobNo.setText("");
                                }
                            });
                            alertDialog.show();
                        }
                        else if (responsecode.equals("0"))
                        {
                            String msg = object.getString("msg");
                            String message = msg.substring(2, msg.length()-2);

                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    ed_MobNo.setText("");
                                }
                            });
                            alertDialog.show();
                        }
                    }
                    catch (JSONException e){
                        Toast.makeText(ResetThumbActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                        Log.e("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public void resetThumbRegistration()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute() {
                //progressDialog1 = ProgressDialog.show(ResetThumbActivity.this, "Please wait", "Resetting thumb...", true);
                //progressDialog1.show();
                //progress_layout.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/resetthumoffline/?";

                    String query = String.format("empId=%s&thumexp=%s&deviceid=%s",
                            URLEncoder.encode(emp_id, "UTF-8"),
                            URLEncoder.encode(str_RegisteredThumbs, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"));

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
                            //progressDialog.dismiss();
                            Toast.makeText(ResetThumbActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            //progressDialog.dismiss();
                            Toast.makeText(ResetThumbActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e){
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //progressDialog.dismiss();
                            Toast.makeText(ResetThumbActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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

                //progress_layout.setVisibility(View.GONE);

                if (response.equals("[]"))
                {
                    Toast.makeText(ResetThumbActivity.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                }
                else
                {
                    /*if (db.checkEmpId(emp_id))
                    {*/
                        //db.UpdateEmpData(new UserDetails_Model(RegisteredBase64_1,RegisteredBase64_2,RegisteredBase64_3,RegisteredBase64_4), emp_id);
                        textToSpeech.speak("Thumbs Updated Successfully!", TextToSpeech.QUEUE_FLUSH, null);

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                        alertDialog.setTitle("Thumbs Updated Successfully");
                        alertDialog.setCancelable(false);
                        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                img_register1.setImageResource(R.drawable.imagefinger);
                                img_register2.setImageResource(R.drawable.imagefinger);
                                img_register3.setImageResource(R.drawable.imagefinger);
                                img_register4.setImageResource(R.drawable.imagefinger);
                                txt_empName.setText("");
                                txt_empId.setText("");
                                ed_MobNo.setText("");

                                RegisteredBase64_1 = null;
                                RegisteredBase64_2 = null;
                                RegisteredBase64_3 = null;
                                RegisteredBase64_4 = null;
                                str_RegisteredThumbs = "";
                            }
                        });
                        alertDialog.show();
                    /*}
                    else
                    {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ResetThumbActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                        alertDialog.setTitle("Employee not authorized for this device");
                        alertDialog.setCancelable(false);
                        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                img_register1.setImageResource(R.drawable.imagefinger);
                                img_register2.setImageResource(R.drawable.imagefinger);
                                img_register3.setImageResource(R.drawable.imagefinger);
                                img_register4.setImageResource(R.drawable.imagefinger);
                                txt_empName.setText("");
                                txt_empId.setText("");
                                ed_MobNo.setText("");

                                RegisteredBase64_1 = null;
                                RegisteredBase64_2 = null;
                                RegisteredBase64_3 = null;
                                RegisteredBase64_4 = null;
                                str_RegisteredThumbs = "";
                            }
                        });
                        alertDialog.show();
                    }*/
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    protected void onStop()
    {
        //logout_id = "1";

        //session.logoutUser();

        //UnInitScanner();

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        if (mfs100 != null) {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        return;
    }
}
