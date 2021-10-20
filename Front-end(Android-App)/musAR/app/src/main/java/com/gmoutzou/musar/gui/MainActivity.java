package com.gmoutzou.musar.gui;

import com.gmoutzou.musar.R;

import com.gmoutzou.musar.agent.O2AInterface;
import com.gmoutzou.musar.agent.ClientProfileAgent;
import com.gmoutzou.musar.utils.CallbackInterface;
import com.gmoutzou.musar.utils.DatabaseHelper;
import com.special.ResideMenu.ResideMenu;
import com.special.ResideMenu.ResideMenuItem;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.logging.Level;
import java.util.regex.Pattern;

import jade.android.AndroidHelper;
import jade.android.MicroRuntimeService;
import jade.android.MicroRuntimeServiceBinder;
import jade.android.RuntimeCallback;
import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import prof.onto.Information;
import prof.onto.MakeOperation;
import prof.onto.ProfileVocabulary;

public class MainActivity extends FragmentActivity implements
        View.OnClickListener, ProfileVocabulary, CallbackInterface {

    private final static int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1;
    private final static int ACCESS_NETWORK_STATE_PERMISSION_CODE = 2;
    private final static int ACCESS_WIFI_STATE_PERMISSION_CODE = 3;
    private final static int CAMERA_PERMISSION_CODE = 4;
    private final static int GET_PRIMARY_ACCOUNT_PERMISSION_CODE = 5;

    private Pattern emailPattern = Patterns.EMAIL_ADDRESS;
    private Logger logger = Logger.getJADELogger(this.getClass().getName());
    private MicroRuntimeServiceBinder microRuntimeServiceBinder;
    private ServiceConnection serviceConnection;
    private O2AInterface o2AInterface;
    private String agentname, host, port, primaryAccount, androidId;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase db;
    private SharedPreferences sharedpreferences;
    private prof.onto.Profile mProfile;
    private boolean profileIsSet = false;

    private ResideMenu resideMenu;
    private ResideMenuItem itemHome;
    private ResideMenuItem itemProfile;
    private ResideMenuItem itemSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logger.log(Level.INFO, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMenu();
        if (savedInstanceState == null) {
            changeFragment(new HomeFragment());
        }
        mDbHelper = new DatabaseHelper(getApplicationContext());
        db = mDbHelper.getWritableDatabase();
        mProfile = null;
        o2AInterface = null;
        sharedpreferences = getApplicationContext()
                .getSharedPreferences(SettingsFragment.MY_PREFERENCES, Context.MODE_PRIVATE);
        if (sharedpreferences.contains(SettingsFragment.HOST_KEY) && !sharedpreferences.getString(SettingsFragment.HOST_KEY, "").equals("")) {
            host = sharedpreferences.getString(SettingsFragment.HOST_KEY, "");
        } else {
            host = SettingsFragment.DEFAULT_HOST;
        }
        if (sharedpreferences.contains(SettingsFragment.PORT_KEY) && !sharedpreferences.getString(SettingsFragment.PORT_KEY, "").equals("")) {
            port = sharedpreferences.getString(SettingsFragment.PORT_KEY, "");
        } else {
            port = SettingsFragment.DEFAULT_PORT;
        }
        androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        requestReadExternalStoragePermission();
    }

    @Override
    protected void onDestroy() {
        logger.log(Level.INFO, "onDestroy()");
        mDbHelper.close();
        super.onDestroy();
    }

    public void requestReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
        } else {
            requestAccessNetworkStatePermission();
        }
    }

    public void requestAccessNetworkStatePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, ACCESS_NETWORK_STATE_PERMISSION_CODE);
        } else {
            requestAccessWifiStatePermission();
        }
    }

    public void requestAccessWifiStatePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, ACCESS_WIFI_STATE_PERMISSION_CODE);
        } else {
            requestCameraPermission();
        }
    }

    public void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            requestGetPrimaryAccountPermission();
        }
    }

    public void requestGetPrimaryAccountPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.GET_ACCOUNTS}, GET_PRIMARY_ACCOUNT_PERMISSION_CODE);
        } else {
            setUserProfile();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestAccessNetworkStatePermission();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    } else {
                        warningDialog(1);
                    }
                }
                break;
            case ACCESS_NETWORK_STATE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestAccessWifiStatePermission();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_NETWORK_STATE)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, ACCESS_NETWORK_STATE_PERMISSION_CODE);
                    } else {
                        warningDialog(1);
                    }
                }
                break;
            case ACCESS_WIFI_STATE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, ACCESS_WIFI_STATE_PERMISSION_CODE);
                    } else {
                        warningDialog(1);
                    }
                }
                break;
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestGetPrimaryAccountPermission();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    } else {
                        warningDialog(1);
                    }
                }
                break;
            case GET_PRIMARY_ACCOUNT_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUserProfile();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.GET_ACCOUNTS)) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.GET_ACCOUNTS}, GET_PRIMARY_ACCOUNT_PERMISSION_CODE);
                    } else {
                        warningDialog(1);
                    }
                }
                break;
            default:
                break;
        }
    }

    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        /*
        String ssid = "";
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            ssid = connectionInfo.getSSID();
        }
        return ssid;
        */
        return networkInfo.isConnected();
    }

    private void warningDialog(int i) {
        switch (i) {
            case 0:
                new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.app_name)
                .setIcon(R.mipmap.ic_launcher)
                .setMessage("Έξοδος από την εφαρμογή?")
                        .setCancelable(false)
                        .setPositiveButton("Ναι", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_HOME);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Όχι", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .show();
                break;
            case 1:
                new AlertDialog.Builder(this)
                        .setTitle("")
                        .setMessage("Για τη λειτουργία της εφαρμογής απαιτείται η χορήγηση της συγκεκριμένης άδειας!")
                        .setCancelable(false)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                dialog.cancel();
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(1);
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        warningDialog(0);
    }

    private void setPrimaryAccount() {
        primaryAccount = "default";
        Account[] accounts = AccountManager.get(MainActivity.this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                primaryAccount = account.name;
                break;
            }
        }
    }

    private void setUserProfile() {
        setPrimaryAccount();
        initAgentCommunication();
        mProfile = mDbHelper.getProfileFromDB(db, primaryAccount);
        if (mProfile != null) {
            logger.log(Level.INFO, "Profile from local DB: " + mProfile.getName());
            profileIsSet = true;
        } else {
            new Thread(){
                public void run(){
                    while (o2AInterface == null) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, e.getMessage());
                        }
                    }
                    makeNewOperation(READ_PROFILE, primaryAccount, null);
                }
            }.start();
        }
    }

    private void initAgentCommunication() {
        /*
        logger.log(Level.INFO, "WiFi SSID " + getCurrentSsid(this));
        if (getCurrentSsid(this).equals("\"TP-LINK_A9EB79\"")) {
            startContainerAndAgent(agentname, host, port, agentStartupCallback);
        }
        */
        agentname = primaryAccount.replace("@", "[at]") + "-" + androidId;
        if (isWiFiConnected(this)) {
            startContainerAndAgent(agentname, host, port, agentStartupCallback);
        }
    }

    public void startARApplication() {
        String mClassToLaunchPackage = getPackageName();
        String mClassToLaunch = mClassToLaunchPackage + ".";
        if (mProfile.getName().equals("Pro-A")) {
            mClassToLaunch += "app.model3d.Object3D.Object3D";
        } else if (mProfile.getName().equals("Pro-B")){
            mClassToLaunch += "app.video.VideoPlayback.VideoPlayback";
        }
        logger.log(Level.INFO, "Start Activity: " + mClassToLaunch);
        Intent intent = new Intent();
        intent.setClassName(mClassToLaunchPackage, mClassToLaunch);
        intent.putExtra("PROFILE", mProfile.getName());
        startActivity(intent);
    }

    public void makeNewOperation(int type, String account, String userChoice) {
        MakeOperation op = new MakeOperation();
        op.setType(type);
        op.setAccount(account);
        op.setUserChoice(userChoice);
        if (o2AInterface != null) {
            o2AInterface.requestProfileOperation(op);
        }
    }

    public String getPrimaryAccount() {
        return primaryAccount;
    }

    public boolean isProfileSet() {
        return profileIsSet;
    }

    private RuntimeCallback<AgentController> agentStartupCallback = new RuntimeCallback<AgentController>() {
        @Override
        public void onSuccess(AgentController agent) {
            o2AInterface = new O2AInterface(agentname);
            if (o2AInterface != null) {
                o2AInterface.registerCallbackInterface(MainActivity.this);
            }
        }

        @Override
        public void onFailure(Throwable throwable) {
            logger.log(Level.INFO, "agentname already in use!");
        }
    };

    private void startContainerAndAgent(final String agentname,
                                        final String host,
                                        final String port,
                                        final RuntimeCallback<AgentController> agentStartupCallback) {

        final Properties profile = new Properties();
        profile.setProperty(Profile.MAIN_HOST, host);
        profile.setProperty(Profile.MAIN_PORT, port);
        profile.setProperty(Profile.MAIN, Boolean.FALSE.toString());
        profile.setProperty(Profile.JVM, Profile.ANDROID);

        if (AndroidHelper.isEmulator()) {
            // Emulator: this is needed to work with emulated devices
            profile.setProperty(Profile.LOCAL_HOST, AndroidHelper.LOOPBACK);
        } else {
            profile.setProperty(Profile.LOCAL_HOST,
                    AndroidHelper.getLocalIPAddress());
        }
        // Emulator: this is not really needed on a real device
        profile.setProperty(Profile.LOCAL_PORT, "2000");
        if (microRuntimeServiceBinder == null) {
            serviceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    microRuntimeServiceBinder = (MicroRuntimeServiceBinder) service;
                    logger.log(Level.INFO, "Gateway successfully bound to MicroRuntimeService");
                    startContainer(agentname, profile, agentStartupCallback);
                }

                public void onServiceDisconnected(ComponentName className) {
                    microRuntimeServiceBinder = null;
                    logger.log(Level.INFO, "Gateway unbound from MicroRuntimeService");
                }
            };
            logger.log(Level.INFO, "Binding Gateway to MicroRuntimeService...");
            // Bind Service
            bindService(new Intent(MainActivity.this, MicroRuntimeService.class),
                    serviceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
            logger.log(Level.INFO, "MicroRumtimeGateway already binded to service");
            startContainer(agentname, profile, agentStartupCallback);
        }
    }

    private void startContainer(final String agentname, Properties profile,
                                final RuntimeCallback<AgentController> agentStartupCallback) {
        if (!MicroRuntime.isRunning()) {
            microRuntimeServiceBinder.startAgentContainer(profile, new RuntimeCallback<Void>() {
                        @Override
                        public void onSuccess(Void thisIsNull) {
                            logger.log(Level.INFO, "Successfully start of the container...");
                            startAgent(agentname, agentStartupCallback);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            logger.log(Level.SEVERE, "Failed to start the container...");
                        }
                    });
        } else {
            startAgent(agentname, agentStartupCallback);
        }
    }

    private void startAgent(final String agentname,
                            final RuntimeCallback<AgentController> agentStartupCallback) {
        microRuntimeServiceBinder.startAgent(
                agentname,
                ClientProfileAgent.class.getName(),
                new Object[] { getApplicationContext() },
                new RuntimeCallback<Void>() {
                    @Override
                    public void onSuccess(Void thisIsNull) {
                        logger.log(Level.INFO, "Successfully start of the "
                                + ClientProfileAgent.class.getName() + "...");
                        try {
                            agentStartupCallback.onSuccess(MicroRuntime.getAgent(agentname));
                        } catch (ControllerException e) {
                            // Should never happen
                            agentStartupCallback.onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.log(Level.SEVERE, "Failed to start the "
                                + ClientProfileAgent.class.getName() + "...");
                        agentStartupCallback.onFailure(throwable);
                    }
                });
    }

    private void stopContainerAndAgent() {
        microRuntimeServiceBinder.stopAgentContainer(new RuntimeCallback<Void>() {
            @Override
            public void onSuccess(Void thisIsNull) {
                logger.log(Level.INFO, "Successfully stop of the  " + agentname);
                unbindService(serviceConnection);
                o2AInterface = null;
            }

            @Override
            public void onFailure(Throwable throwable) {
                logger.log(Level.SEVERE, "Failed to stop the "
                        + ClientProfileAgent.class.getName()
                        + "...");
                agentStartupCallback.onFailure(throwable);
            }
        });
    }

    @Override
    public void callbackMethod(Information info) {
        if (info.getStatus().getCode() == SUCCESS) {
            if ((mProfile = info.getProfile()) != null) {
                logger.log(Level.INFO, "Profile from ProfileManager agent: " + mProfile.getName());
                switch (info.getType()) {
                    case CREATE_PROFILE:
                        if (mDbHelper.insertProfileToDB(db, mProfile)) {
                            profileIsSet = true;
                            changeFragment(new HomeFragment());
                        } else {
                            Toast.makeText(this, "Κάτι πήγε στραβά..!", Toast.LENGTH_LONG);
                        }
                        break;
                    case READ_PROFILE:
                        if (mDbHelper.insertProfileToDB(db, mProfile)) {
                            profileIsSet = true;
                        } else {
                            Toast.makeText(this, "Κάτι πήγε στραβά..!", Toast.LENGTH_LONG);
                        }
                        break;
                    case UPDATE_PROFILE:
                        if (mDbHelper.updateProfileInDB(db, mProfile)) {
                            profileIsSet = true;
                            changeFragment(new HomeFragment());
                        } else {
                            Toast.makeText(this, "Κάτι πήγε στραβά..!", Toast.LENGTH_LONG);
                        }
                        break;
                    case DELETE_PROFILE:
                        mDbHelper.deleteProfileFromDB(db, mProfile);
                        profileIsSet = false;
                        break;
                    default:
                        break;
                }
            }
        } else {
            logger.log(Level.WARNING, info.getStatus().getMessage());
        }
    }

    private void setUpMenu() {

        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.menu_background);
        resideMenu.attachToActivity(this);
        resideMenu.setMenuListener(menuListener);
        resideMenu.setScaleValue(0.6f);

        itemHome     = new ResideMenuItem(this, R.drawable.icon_home,     "Home");
        itemProfile  = new ResideMenuItem(this, R.drawable.icon_profile,  "Profile");
        itemSettings = new ResideMenuItem(this, R.drawable.icon_settings, "Settings");

        itemHome.setOnClickListener(this);
        itemProfile.setOnClickListener(this);
        itemSettings.setOnClickListener(this);

        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemProfile, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSettings, ResideMenu.DIRECTION_RIGHT);

        findViewById(R.id.title_bar_left_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });
        findViewById(R.id.title_bar_right_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resideMenu.openMenu(ResideMenu.DIRECTION_RIGHT);
            }
        });
    }

    public void changeFragment(Fragment targetFragment){
        resideMenu.clearIgnoredViewList();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, targetFragment, "fragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public ResideMenu getResideMenu(){
        return resideMenu;
    }

    private ResideMenu.OnMenuListener menuListener = new ResideMenu.OnMenuListener() {
        @Override
        public void openMenu() {
            //Toast.makeText(getApplicationContext(), "Menu is opened!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void closeMenu() {
            //Toast.makeText(getApplicationContext(), "Menu is closed!", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return resideMenu.dispatchTouchEvent(ev);
    }

    @Override
    public void onClick(View view) {
        if (view == itemHome){
            changeFragment(new HomeFragment());
        } else if (view == itemProfile){
            changeFragment(new ProfileFragment());
        } else if (view == itemSettings){
            changeFragment(new SettingsFragment());
        }
        resideMenu.closeMenu();
    }
}
