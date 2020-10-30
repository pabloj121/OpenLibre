package de.dorianscholz.openlibre.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.opencsv.CSVReader;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import de.dorianscholz.openlibre.R;
import de.dorianscholz.openlibre.model.BloodGlucoseData;
import de.dorianscholz.openlibre.model.GlucoseData;
import de.dorianscholz.openlibre.model.RawTagData;
import de.dorianscholz.openlibre.model.ReadingData;
import de.dorianscholz.openlibre.model.SensorData;
import de.dorianscholz.openlibre.service.NfcVReaderTask;
import de.dorianscholz.openlibre.service.SensorExpiresNotificationKt;
import de.dorianscholz.openlibre.service.TidepoolSynchronization;
import de.dorianscholz.openlibre.ui.login.LoginActivity;

import de.dorianscholz.openlibre.ui.login.LoginActivityKt;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

import static de.dorianscholz.openlibre.OpenLibre.openLibreDataPath;
import static de.dorianscholz.openlibre.OpenLibre.realmConfigProcessedData;
import static de.dorianscholz.openlibre.OpenLibre.realmConfigRawData;
import static de.dorianscholz.openlibre.model.AlgorithmUtil.getDurationBreakdown;
import static de.dorianscholz.openlibre.model.SensorData.START_DATE;
import static de.dorianscholz.openlibre.service.NfcVReaderTask.processRawData;



public class MainActivity extends AppCompatActivity implements LogFragment.OnScanDataListener {

    private static final String LOG_ID = "OpenLibre::" + MainActivity.class.getSimpleName();
    private static final String DEBUG_SENSOR_TAG_ID = "e007a00000111111";
    private static final int PENDING_INTENT_TECH_DISCOVERED = 1;

    public long mLastScanTime = 0;
    private NfcAdapter mNfcAdapter;

    private Realm mRealmRawData;
    private Realm mRealmProcessedData;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private boolean mContinuousSensorReadingFlag = false;
    private Tag mLastNfcTag;
    private MainActivity mainActivity;

    private FirebaseAuth auth;
    private boolean logged = false; // not used !

    private FirebaseAnalytics mFirebaseAnalytics;

    private LocalDate startDate;
    private String startDateString;

    TextView importPathFile;
    Button filePicker;
    Intent importFileIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        // initial_date = (Date) Calendar.getInstance().getTime();
        startDate = LocalDate.now();
        startDateString = startDate.getDayOfMonth() + "/" + startDate.getMonth() + "/" + startDate.getYear();

        mRealmRawData = Realm.getInstance(realmConfigRawData);
        mRealmProcessedData = Realm.getInstance(realmConfigProcessedData);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the FirebaseAuthentication instance.
        auth = FirebaseAuth.getInstance();
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (auth.getCurrentUser() != null) {
            // already signed in
            logged = true;
        }

        // Go to the previous screen after press on the back arrow !
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getApplicationContext());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        mNfcAdapter = ((NfcManager) this.getSystemService(Context.NFC_SERVICE)).getDefaultAdapter();
        if (mNfcAdapter != null) {
            Log.d(LOG_ID, "Got NFC adapter");
            if (!mNfcAdapter.isEnabled()) {
                Toast.makeText(this, getResources().getString(R.string.error_nfc_disabled), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(LOG_ID,"No NFC adapter found!");
            Toast.makeText(this, getResources().getString(R.string.error_nfc_device_not_supported), Toast.LENGTH_LONG).show();
        }

        importPathFile = (TextView) findViewById(R.id.importPathFile);
        filePicker = (Button) findViewById(R.id.import_button);

        filePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                importFileIntent = new Intent(importFileIntent.ACTION_GET_CONTENT);
                importFileIntent.setType("*/*"); // ???
                // 99 is an ID that I know is unused
                startActivityForResult(importFileIntent, 99);
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();

        FirebaseUser user = auth.getCurrentUser();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter == null) {
            mNfcAdapter = ((NfcManager) this.getSystemService(Context.NFC_SERVICE)).getDefaultAdapter();
        }

        if (mNfcAdapter != null) {
            try {
                mNfcAdapter.isEnabled();
            } catch (NullPointerException e) {
                // Drop NullPointerException
            }
            try {
                mNfcAdapter.isEnabled();
            } catch (NullPointerException e) {
                // Drop NullPointerException
            }

            PendingIntent pi = createPendingResult(PENDING_INTENT_TECH_DISCOVERED, new Intent(), 0);
            if (pi != null) {
                try {
                    mNfcAdapter.enableForegroundDispatch(this, pi,
                        new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED) },
                        new String[][] { new String[]{"android.nfc.tech.NfcV"} }
                    );
                } catch (NullPointerException e) {
                    // Drop NullPointerException
                }
            }
        }

        // One of the checks before the user interaction is the remaining sensor time
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);
        RealmResults<SensorData> sensorDataResults = realmProcessedData.where(SensorData.class).
                findAllSorted(START_DATE, Sort.DESCENDING);

        if (sensorDataResults.size() > 0){
            SensorData sensorData = sensorDataResults.first();
            long timeLeft = sensorData.getTimeLeft();
            long three_days = TimeUnit.DAYS.toMillis(3);

            // If the remaining time is less than 3 days, the user will see the notification
            // showing the remaining sensor time each time the user enter to the application
            if(timeLeft < three_days){
                Intent intent = new Intent(this, SensorExpiresNotificationKt.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.sensor_expired, Toast.LENGTH_SHORT).show();
            }
        }

        realmProcessedData.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            try {
                // Disable foreground dispatch:
                mNfcAdapter.disableForegroundDispatch(this);
            } catch (NullPointerException e) {
                // Drop NullPointerException
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealmProcessedData.close();
        mRealmRawData.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // show debug menu only in developer mode
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean developerMode = settings.getBoolean("pref_developer_mode", false);
        MenuItem debugMenuItem = menu.findItem(R.id.action_debug_menu);
        debugMenuItem.setVisible(developerMode);

        String tidepoolUsername = settings.getString("pref_tidepool_username", "");
        String tidepoolPassword = settings.getString("pref_tidepool_password", "");
        MenuItem tidepoolMenuItem = menu.findItem(R.id.action_tidepool_status);
        tidepoolMenuItem.setVisible((!tidepoolUsername.equals("") && !tidepoolPassword.equals("")));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;

        } else if (id == R.id.action_show_last_scan) {
            RealmResults<ReadingData> readingDataResults = mRealmProcessedData.where(ReadingData.class).
                    findAllSorted(ReadingData.DATE, Sort.DESCENDING);
            if (readingDataResults.size() == 0) {
                Toast.makeText(this, "No scan data available!", Toast.LENGTH_LONG).show();
            } else {
                ((DataPlotFragment) mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan))
                        .showScan(readingDataResults.first());
                mViewPager.setCurrentItem(getResources().getInteger(R.integer.viewpager_page_show_scan));
            }
            return true;

        } else if (id == R.id.action_show_full_history) {
            List<GlucoseData> history = mRealmProcessedData.where(GlucoseData.class).
                    equalTo(GlucoseData.IS_TREND_DATA, false).
                    findAllSorted(GlucoseData.DATE, Sort.ASCENDING);
            ((DataPlotFragment) mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan))
                    .clearScanData();
            ((DataPlotFragment) mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan))
                    .showHistory(history);
            mViewPager.setCurrentItem(getResources().getInteger(R.integer.viewpager_page_show_scan));
            return true;

        } else if (id == R.id.action_enter_blood_glucose) {
            DialogFragment bloodGlucoseInputFragment = new BloodGlucoseInputFragment();
            bloodGlucoseInputFragment.show(getSupportFragmentManager(), "enterglucose");

            // Controlar tamaño del historial que se va guardando en cada momento
            // List<BloodGlucoseData> history = mRealmProcessedData.where(BloodGlucoseData.class).findAllSorted(BloodGlucoseData.DATE, Sort.ASCENDING);

            // System.out.println("Tamaño del historial: " + history.size());

            return true;

        } else if (id == R.id.action_show_fpu_calculator) {
            DialogFragment fpuCalculatorFragment = new FPUCalculatorFragment();
            fpuCalculatorFragment.show(getSupportFragmentManager(), "fpucalculator");
            return true;

        } else if (id == R.id.action_tidepool_status) {
            new TidepoolStatusFragment().show(getSupportFragmentManager(), "tidepoolstatus");
            return true;

        } else if (id == R.id.action_show_sensor_status) {
            new SensorStatusFragment().show(getSupportFragmentManager(), "sensorstatus");
            return true;

        } else if (id == R.id.action_export) {
            new ExportFragment().show(getSupportFragmentManager(), "export");
            return true;

        } else if (id == R.id.action_about) {
            new AboutFragment().show(getSupportFragmentManager(), "about");
            return true;

        } else if (id == R.id.action_debug_make_crash) {
            throw new RuntimeException("DEBUG: test crash");

        } else if (id == R.id.action_debug_cont_nfc_reading) {
            mContinuousSensorReadingFlag = !mContinuousSensorReadingFlag;
            return true;

        } else if (id == R.id.action_debug_clear_plot) {
            ((DataPlotFragment) mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan)).mPlot.clear();
            return true;

        } else if (id == R.id.action_debug_plot_readings) {
            List<ReadingData> readingDataList = mRealmProcessedData
                    .where(ReadingData.class)
                    .isNotEmpty(ReadingData.HISTORY)
                    .findAllSorted(ReadingData.DATE, Sort.ASCENDING);
            // plot only the last 100 readings
            ArrayList<ReadingData> readingDataLimtedList = new ArrayList<>();
            for (int i = readingDataList.size(); i > Math.max(readingDataList.size() - 100, 0) ; i--) {
                readingDataLimtedList.add(readingDataList.get(i-1));
            }
            ((DataPlotFragment) mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan))
                    .showMultipleScans(readingDataLimtedList);
            mViewPager.setCurrentItem(getResources().getInteger(R.integer.viewpager_page_show_scan));
            return true;

        } else if (id == R.id.action_debug_value2) {
            byte[] data = {(byte) 0x63, (byte) 0x3b, (byte) 0x20, (byte) 0x12, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x57, (byte) 0x00, (byte) 0x07, (byte) 0x06, (byte) 0xd6, (byte) 0x06, (byte) 0xc8, (byte) 0x50, (byte) 0x5a, (byte) 0x80, (byte) 0xd6, (byte) 0x06, (byte) 0xc8, (byte) 0x20, (byte) 0x5a, (byte) 0x80, (byte) 0xe4, (byte) 0x06, (byte) 0xc8, (byte) 0x18, (byte) 0x5a, (byte) 0x80, (byte) 0xe3, (byte) 0x06, (byte) 0xc8, (byte) 0x2c, (byte) 0x5a, (byte) 0x80, (byte) 0xea, (byte) 0x06, (byte) 0xc8, (byte) 0x34, (byte) 0x5a, (byte) 0x80, (byte) 0xea, (byte) 0x06, (byte) 0xc8, (byte) 0x40, (byte) 0x5a, (byte) 0x80, (byte) 0xf8, (byte) 0x06, (byte) 0x88, (byte) 0x2e, (byte) 0x1a, (byte) 0x82, (byte) 0x0d, (byte) 0x07, (byte) 0xc8, (byte) 0xdc, (byte) 0x59, (byte) 0x80, (byte) 0x0c, (byte) 0x07, (byte) 0xc8, (byte) 0x30, (byte) 0x5a, (byte) 0x80, (byte) 0x07, (byte) 0x07, (byte) 0xc8, (byte) 0x58, (byte) 0x5a, (byte) 0x80, (byte) 0x06, (byte) 0x07, (byte) 0xc8, (byte) 0x50, (byte) 0x5a, (byte) 0x80, (byte) 0x01, (byte) 0x07, (byte) 0xc8, (byte) 0x5c, (byte) 0x5a, (byte) 0x80, (byte) 0xec, (byte) 0x06, (byte) 0xc8, (byte) 0x68, (byte) 0x5a, (byte) 0x80, (byte) 0xde, (byte) 0x06, (byte) 0xc8, (byte) 0x74, (byte) 0x5a, (byte) 0x80, (byte) 0xd6, (byte) 0x06, (byte) 0xc8, (byte) 0x7c, (byte) 0x5a, (byte) 0x80, (byte) 0xd3, (byte) 0x06, (byte) 0xc8, (byte) 0x48, (byte) 0x5a, (byte) 0x80, (byte) 0x62, (byte) 0x05, (byte) 0xc8, (byte) 0xb4, (byte) 0x59, (byte) 0x80, (byte) 0x73, (byte) 0x05, (byte) 0xc8, (byte) 0x78, (byte) 0x59, (byte) 0x80, (byte) 0xdb, (byte) 0x05, (byte) 0xc8, (byte) 0x1c, (byte) 0x59, (byte) 0x80, (byte) 0x36, (byte) 0x06, (byte) 0xc8, (byte) 0x68, (byte) 0x59, (byte) 0x80, (byte) 0xb9, (byte) 0x06, (byte) 0xc8, (byte) 0x98, (byte) 0x59, (byte) 0x80, (byte) 0x07, (byte) 0x07, (byte) 0xc8, (byte) 0x58, (byte) 0x5a, (byte) 0x80, (byte) 0x28, (byte) 0x06, (byte) 0xc8, (byte) 0xa8, (byte) 0x5b, (byte) 0x80, (byte) 0xe8, (byte) 0x05, (byte) 0xc8, (byte) 0xb0, (byte) 0x9b, (byte) 0x80, (byte) 0x78, (byte) 0x05, (byte) 0xc8, (byte) 0x90, (byte) 0x5b, (byte) 0x80, (byte) 0xd4, (byte) 0x04, (byte) 0xc8, (byte) 0xe4, (byte) 0x9b, (byte) 0x80, (byte) 0xb8, (byte) 0x04, (byte) 0xc8, (byte) 0x30, (byte) 0x9c, (byte) 0x80, (byte) 0xed, (byte) 0x04, (byte) 0xc8, (byte) 0xd4, (byte) 0x5b, (byte) 0x80, (byte) 0x2d, (byte) 0x05, (byte) 0xc8, (byte) 0xb8, (byte) 0x5b, (byte) 0x80, (byte) 0x76, (byte) 0x05, (byte) 0xc8, (byte) 0x38, (byte) 0x9c, (byte) 0x80, (byte) 0x1e, (byte) 0x05, (byte) 0xc8, (byte) 0x50, (byte) 0xa0, (byte) 0x80, (byte) 0xa7, (byte) 0x04, (byte) 0xc8, (byte) 0xa4, (byte) 0x60, (byte) 0x80, (byte) 0xbd, (byte) 0x04, (byte) 0xc8, (byte) 0xe0, (byte) 0x5b, (byte) 0x80, (byte) 0x96, (byte) 0x04, (byte) 0xc8, (byte) 0xf0, (byte) 0x9c, (byte) 0x80, (byte) 0x4f, (byte) 0x04, (byte) 0xc8, (byte) 0xcc, (byte) 0x9e, (byte) 0x80, (byte) 0xfe, (byte) 0x03, (byte) 0xc8, (byte) 0xd4, (byte) 0x5c, (byte) 0x80, (byte) 0xc5, (byte) 0x03, (byte) 0xc8, (byte) 0x78, (byte) 0x9c, (byte) 0x80, (byte) 0xae, (byte) 0x03, (byte) 0xc8, (byte) 0x2c, (byte) 0x9c, (byte) 0x80, (byte) 0xb4, (byte) 0x03, (byte) 0xc8, (byte) 0x08, (byte) 0x5b, (byte) 0x80, (byte) 0xc6, (byte) 0x03, (byte) 0xc8, (byte) 0x44, (byte) 0x5a, (byte) 0x80, (byte) 0xfc, (byte) 0x03, (byte) 0xc8, (byte) 0x80, (byte) 0x9b, (byte) 0x80, (byte) 0x66, (byte) 0x04, (byte) 0xc8, (byte) 0xec, (byte) 0x9a, (byte) 0x80, (byte) 0xcb, (byte) 0x04, (byte) 0xc8, (byte) 0x9c, (byte) 0x5a, (byte) 0x80, (byte) 0x0c, (byte) 0x05, (byte) 0xc8, (byte) 0x4c, (byte) 0x5a, (byte) 0x80, (byte) 0x1b, (byte) 0x05, (byte) 0xc8, (byte) 0x7c, (byte) 0x9a, (byte) 0x80, (byte) 0x33, (byte) 0x05, (byte) 0xc8, (byte) 0x64, (byte) 0x9a, (byte) 0x80, (byte) 0x25, (byte) 0x05, (byte) 0xc8, (byte) 0x74, (byte) 0x5a, (byte) 0x80, (byte) 0x41, (byte) 0x05, (byte) 0xc8, (byte) 0xac, (byte) 0x59, (byte) 0x80, (byte) 0x28, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0xe8, (byte) 0x50, (byte) 0x00, (byte) 0x01, (byte) 0x3b, (byte) 0x05, (byte) 0x15, (byte) 0x51, (byte) 0x14, (byte) 0x07, (byte) 0x96, (byte) 0x80, (byte) 0x5a, (byte) 0x00, (byte) 0xed, (byte) 0xa6, (byte) 0x06, (byte) 0x3c, (byte) 0x1a, (byte) 0xc8, (byte) 0x04, (byte) 0x04, (byte) 0x7a, (byte) 0x6e, (byte) 0x9e, (byte) 0x42, (byte) 0x21, (byte) 0x83, (byte) 0xf2, (byte) 0x90, (byte) 0x07, (byte) 0x00, (byte) 0x06, (byte) 0x08, (byte) 0x02, (byte) 0x24, (byte) 0x0c, (byte) 0x43, (byte) 0x17, (byte) 0x3c};
            onShowScanData(processRawData(DEBUG_SENSOR_TAG_ID, data));
            return true;

        } else if (id == R.id.action_debug_not_ready) {
            // sensor not ready yet
            byte[] data = {(byte) 0x63, (byte) 0x3b, (byte) 0x20, (byte) 0x12, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x2e, (byte) 0x0b, (byte) 0x00, (byte) 0x11, (byte) 0x9e, (byte) 0x80, (byte) 0x52, (byte) 0x61, (byte) 0x00, (byte) 0xa4, (byte) 0x88, (byte) 0x80, (byte) 0x66, (byte) 0x60, (byte) 0x80, (byte) 0xbb, (byte) 0x84, (byte) 0x80, (byte) 0xba, (byte) 0x9f, (byte) 0x80, (byte) 0xa3, (byte) 0x03, (byte) 0xc8, (byte) 0x9c, (byte) 0x9f, (byte) 0x80, (byte) 0x8b, (byte) 0x03, (byte) 0xc8, (byte) 0x44, (byte) 0x9f, (byte) 0x80, (byte) 0xb7, (byte) 0x03, (byte) 0x88, (byte) 0x02, (byte) 0x9f, (byte) 0x80, (byte) 0xee, (byte) 0x03, (byte) 0xc8, (byte) 0x0c, (byte) 0x9e, (byte) 0x80, (byte) 0x0e, (byte) 0x04, (byte) 0xc8, (byte) 0x9c, (byte) 0x9d, (byte) 0x80, (byte) 0x1e, (byte) 0x04, (byte) 0xc8, (byte) 0xf8, (byte) 0x9d, (byte) 0x80, (byte) 0x2e, (byte) 0x04, (byte) 0xc8, (byte) 0x2c, (byte) 0x9e, (byte) 0x80, (byte) 0x3b, (byte) 0x04, (byte) 0xc8, (byte) 0x3c, (byte) 0xde, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0xdb, (byte) 0x00, (byte) 0x01, (byte) 0x3b, (byte) 0x05, (byte) 0xd1, (byte) 0x51, (byte) 0x14, (byte) 0x07, (byte) 0x96, (byte) 0x80, (byte) 0x5a, (byte) 0x00, (byte) 0xed, (byte) 0xa6, (byte) 0x02, (byte) 0x70, (byte) 0x1a, (byte) 0xc8, (byte) 0x04, (byte) 0x54, (byte) 0xd9, (byte) 0x66, (byte) 0x9e, (byte) 0x42, (byte) 0x21, (byte) 0x83, (byte) 0xf2, (byte) 0x90, (byte) 0x07, (byte) 0x00, (byte) 0x06, (byte) 0x08, (byte) 0x02, (byte) 0x24, (byte) 0x0c, (byte) 0x43, (byte) 0x17, (byte) 0x3c};
            onShowScanData(processRawData(DEBUG_SENSOR_TAG_ID, data));
            return true;

        } else if (id == R.id.action_debug_export_data) {
            Log.d(LOG_ID, "Exporting data to: " + openLibreDataPath);
            /*
            // export raw data
            File file = new File(openLibreDataPath, "openlibre-db-export.txt");
            FileOutputStream stream;
            try {
                stream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return true;
            }
            try {
                RealmResults<RawTagData> rawTagDataResults = mRealmProcessedData.where(RawTagData.class).findAll();
                for (RawTagData rawTagData : rawTagDataResults) {
                    stream.write(rawTagData.id.getBytes());
                    stream.write("\n".getBytes());
                    stream.write(mFormatDateTimeSec.format(new Date(rawTagData.date)).getBytes());
                    stream.write("\n".getBytes());
                    stream.write(bytesToHexString(rawTagData.data).getBytes());
                    stream.write("\n".getBytes());
                }
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            // export realm db to json
            Gson gson = new Gson();
            File jsonFile = new File(openLibreDataPath, "openlibre-db-export.json");
            try {
                JsonWriter writer = new JsonWriter(new FileWriter(jsonFile));
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("rawTagData");
                writer.beginArray();
                for (RawTagData rawTagData : mRealmRawData.where(RawTagData.class).findAll()) {
                    rawTagData = mRealmRawData.copyFromRealm(rawTagData);
                    gson.toJson(rawTagData, RawTagData.class, writer);
                }
                writer.endArray();
                writer.name("readingData");
                writer.beginArray();
                for (ReadingData readingData : mRealmProcessedData.where(ReadingData.class).findAll()) {
                    readingData = mRealmProcessedData.copyFromRealm(readingData);
                    gson.toJson(readingData, ReadingData.class, writer);
                }
                writer.endArray();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }

            /*
            // copy realm db
            File source = new File(mRealmProcessedData.getPath());
            mRealmProcessedData.close();
            File destination = new File(openLibreDataPath, "default.realm");
            try {
                FileUtils.copyFile(source, destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRealmProcessedData = Realm.getDefaultInstance();
            */

            return true;

        } else if (id == R.id.action_reparse_raw_data) {
            // Delete complete Realm with processed data and parse raw data again

            // close Realm instance
            mRealmProcessedData.close();

            // destroy log fragment to close its Realm instance
            Fragment logFragment = mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_fragment_log);
            getSupportFragmentManager().beginTransaction().remove(logFragment).commitNow();

            // delete Realm file
            Realm.deleteRealm(realmConfigProcessedData);

            // create new Realm instance
            mRealmProcessedData = Realm.getInstance(realmConfigProcessedData);

            // reparse raw data into new Realm
            mRealmProcessedData.beginTransaction();
            for (RawTagData rawTagData : mRealmRawData.where(RawTagData.class)
                    .findAllSorted(RawTagData.DATE, Sort.ASCENDING)) {
                mRealmProcessedData.copyToRealmOrUpdate(new ReadingData(rawTagData));
            }
            mRealmProcessedData.commitTransaction();
            return true;

        } else if (id == R.id.action_delete_debug_data) {
            mRealmRawData.beginTransaction();
            mRealmRawData.where(RawTagData.class).contains(RawTagData.ID, DEBUG_SENSOR_TAG_ID).findAll().deleteAllFromRealm();
            mRealmRawData.commitTransaction();

            mRealmProcessedData.beginTransaction();
            mRealmProcessedData.where(ReadingData.class).contains(ReadingData.ID, DEBUG_SENSOR_TAG_ID).findAll().deleteAllFromRealm();
            mRealmProcessedData.where(GlucoseData.class).contains(GlucoseData.ID, DEBUG_SENSOR_TAG_ID).findAll().deleteAllFromRealm();
            mRealmProcessedData.where(SensorData.class).contains(SensorData.ID, DEBUG_SENSOR_TAG_ID).findAll().deleteAllFromRealm();
            mRealmProcessedData.commitTransaction();
            return true;

        } else if ( id == R.id.action_google){
            // If logged in a previous time, the user does not need to log in again
            if (auth.getCurrentUser() != null){
                Toast.makeText(this, R.string.already_login, Toast.LENGTH_SHORT).show();
                mViewPager.setCurrentItem(getResources().getInteger(R.integer.viewpager_page_fragment_agenda));

                AgendaFragment.newInstance();
            } else { // not logged
                Intent intent = new Intent(this, LoginActivity.class);

                startActivity(intent);
                finish();
            }

        } else if ( id == R.id.action_complete_glucose_data) {
            GlucoseFormFragment form =  GlucoseFormFragment.newInstance();
            form.show(getSupportFragmentManager(), "glucoseformfragment");

            return true;
        } else if ( id == R.id.action_report) {
            Intent intent = new Intent(this, DataVisualizationActivity.class);
            startActivity(intent);

            return true;
        }
        /*
        * else if (id== R.id.action_prediction) {
            //List<GlucoseData> history = mRealmProcessedData.where(GlucoseData.class).
                    //equalTo(GlucoseData.IS_TREND_DATA, false).
                    //findAllSorted(GlucoseData.DATE, Sort.ASCENDING);

            List<ReadingData> history = mRealmProcessedData.where(ReadingData.class).
                    findAllSorted(ReadingData.DATE, Sort.ASCENDING);

            trainModel(history);

            return true;
        }
        */
        
        return super.onOptionsItemSelected(item);
    }

    public boolean trainModel(List<ReadingData> history){
        // The model will be trained every seven days, halfway through the use of the sensor
        LocalDate localDate = LocalDate.now();
        String current = localDate.getDayOfMonth() + "/" + localDate.getMonth() + "/" + localDate.getYear();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        Date firstDate = new Date();
        try {
            firstDate = sdf.parse(startDateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Date secondDate = new Date();

        System.out.println("date: " + secondDate);
        try {
            secondDate = sdf.parse(current);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("date: " + secondDate);

        long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        // Repasar si aqui va el mViewPager !
        // export(mViewPager);

        // Each five days, the model trains with the new data and is rebuilt
        //if (diff > 7){
        // El modelo se debe entrenar con los datos nuevos
        String data = dataToCSV(history);

        initPython();

        Python python = Python.getInstance();
        PyObject pythonFile = python.getModule("prediction"); // prediction.py
        PyObject results = pythonFile.callAttr("main", data);
        String r = results.toString();
        /*
        Intent intent = new Intent(this, AlgorithmActivity.class);
        // Tengo que pasarle a la activity las opciones de menú
        startActivity(intent);
        return true;
        */
        
        /*
        }else {
            Toast.makeText(this, "Aún no hay información suficiente", Toast.LENGTH_SHORT);
            return false;
        }
         */
        return true; // Esta linea esta a voleo, revisar !
    }


    public void initPython(){
        // "context" must be an Activity, Service or Application object from your app.
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    public String dataToCSV(List<ReadingData> history){
        StringBuilder data = new StringBuilder();

        // Cabecera de la función !
        // class is equal to risk
        data.append("id,timezone,date,glucose,horario_comer,food_type,sport,trend,is_trend,stress,class\n");
        // comprobar fecha  5 días!
        for (ReadingData read: history) {
            RealmList<GlucoseData> trend = read.getTrend();

            for (GlucoseData glucose: trend) {
                data.append(glucose.getId() + "," + glucose.getTimezoneOffsetInMinutes() + "," + glucose.getDate() + ","
                        + glucose.glucose() + "," + glucose.getHorario_comer() + "," + glucose.getFood_type() + "," +
                        glucose.isSport() + "," + "" + "," + glucose.isTrendData() + "," + glucose.isStress() +
                        "," + glucose.getRisk()+ "\n");
            }
        }
        return data.toString();
    }

    // Define the risk controls
    public void dataTreatment(List<ReadingData> history){
        for (ReadingData reading: history){

        }

        for(int i = 0;i < history.size(); ++i){
            List<GlucoseData> glucoseList = history.get(i).getHistory();

            for (int j = 0; j < glucoseList.size()-1; ++j){
                float glucose = glucoseList.get(j+1).glucose();
                // String nuevo = Float.toString(glucose);
                glucoseList.get(j).setGlucoseLevelRaw(Float.toString(glucose));
            }
            // history.get(i).setHistory(glucose);

        }

    }




    public void export(View view){
        // Check permissions
        Intent intent = new Intent(this, SensorExpiresNotificationKt.class);
        startActivity(intent);


        // Tratar los datos antes de escribirlos !
        List<ReadingData> history2 = mRealmProcessedData.where(ReadingData.class).
                findAllSorted(ReadingData.DATE, Sort.ASCENDING);

        StringBuilder data = new StringBuilder();

        // Cabecera de la función !
        // class is equal to risk
        data.append("id,timezone,date,glucose,horario_comer,food_type,sport,trend,is_trend,stress,class\n");
        // comprobar fecha  5 días!
        for (ReadingData read: history2) {
            RealmList<GlucoseData> trend = read.getTrend();

            // Exporting only trend and not history?!
            for (GlucoseData glucose: trend) {
                data.append(glucose.getId() + "," + glucose.getTimezoneOffsetInMinutes() + "," + glucose.getDate() + ","
                        + glucose.glucose() + "," + glucose.getHorario_comer() + "," + glucose.getFood_type() + "," +
                        glucose.isSport() + "," + "" + "," + glucose.isTrendData() + "," + glucose.isStress() +
                        "," + glucose.getRisk()+ "\n");
            }
        }

        // Start writing locally (INTERNAL STORAGE??!)
        String filename = "data.csv";
        FileOutputStream out = null;

        try{
            out = openFileOutput(filename, MODE_PRIVATE);
            out.write(data.toString().getBytes());

            // Toast.makeText(this, "Saved to" + getExternalFilesDir() + "/" + filename, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Saved to" + getFilesDir() + "/" + filename, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if (out != null){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Finish writing locally

        try{
            // Saving the file into device
            //FileOutputStream
            out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((data.toString()).getBytes());
            out.close();

            // Exporting
            Context context = getApplicationContext();
            File filelocation = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "de.dorianscholz.openlibre.fileprovider", filelocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            // the next line is not ready yet
            fileIntent.putExtra(Intent.ACTION_CREATE_DOCUMENT, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));

            // Toast.makeText(this, "Path of the file saved: " + openLibreDataPath.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void importCSV(String path) throws IOException {
        SensorData sensor = null;
        ReadingData previousData = new ReadingData();

        System.out.println("path: " + path + "\n");
        // To obtain the extension of the file to import
        String extension = FilenameUtils.getExtension(path);

        // The program is going to be able to import only CSV files,
        // since it can only export data to CSV files
        /* if (!extension.equals(format) ){ // != "csv"
            // Check if it's a good option to stop the execution in this way !
            Toast.makeText(this, R.string.incorrect_extension, Toast.LENGTH_SHORT).show();
            return;
        }
        */

        // Uri uri = Uri.parse(path);
        // File file = new File(uri.getPath());

        // path  = "/sdcard/Download/data.csv";
        // System.out.println("path: " + path + "\n");
        // FileReader file_reader = new FileReader(path);
        // CSVReader reader = new CSVReader(new FileReader(path));

        CSVReader reader = null;
        path = "/storage/emulated/0/Download/data.csv";

        try {
            reader = new CSVReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        // copy data to database
        Realm realmProcessedData = Realm.getInstance(realmConfigProcessedData);
        // commit processed data into realm
        realmProcessedData.beginTransaction();

        String [] nextLine = null;
        RealmList<GlucoseData> history = new RealmList<GlucoseData>(); // = previousData.getHistory();
        RealmList<GlucoseData> trend = new RealmList<GlucoseData>(); // null;

        // Header reading
        try{
            nextLine = reader.readNext();
        } catch(NullPointerException e){
            e.printStackTrace();
        }

        // Data reading
        while ((nextLine = reader.readNext()) != null) {
            GlucoseData glucose = new GlucoseData();

            // nextLine[] is an array of values from the line
            //System.out.println(nextLine[0] + nextLine[1] + "etc...");
            previousData.setId(nextLine[0]);
            previousData.setTimezoneOffsetInMinutes(nextLine[1]);
            previousData.setDate(nextLine[2]);

            // "id,timezone,date,glucose,horario_comer,food_type,sport,trend,is_trend,stress,risk\n";
            glucose.setId(nextLine[0]);
            glucose.setTimezoneOffsetInMinutes(Integer.parseInt(nextLine[1]));
            glucose.setDate(nextLine[2]);
            glucose.setGlucoseLevelRaw(nextLine[3]);
            glucose.setHorario_comer(nextLine[4]);
            glucose.setFood_type(Integer.parseInt(nextLine[5]));
            glucose.setSport(Boolean.parseBoolean(nextLine[6]));
            glucose.setAscendent_trend(nextLine[7]);
            glucose.setTrendData(nextLine[8]);
            glucose.setStress(Boolean.parseBoolean(nextLine[9]));
            glucose.setRisk(Integer.parseInt(nextLine[10])); // revisar!

            // Its necessary to save the trend
            if (glucose.isTrendData()){
                trend.add(glucose);
            }
            else{
                history.add(glucose);
            }
            // Se necesita un ProgressBar en esta acción !
        }
        previousData.setTrend(trend);
        previousData.setHistory(history);

        ReadingData readingData = realmProcessedData.copyToRealmOrUpdate(previousData);
        realmProcessedData.commitTransaction();
        realmProcessedData.close();

        // copy data to database
        Realm realmGlucoseData = Realm.getInstance(realmConfigProcessedData);
        // commit processed data into realm
        realmGlucoseData.beginTransaction();
        List<GlucoseData> historyData = realmGlucoseData.copyToRealmOrUpdate(history);
        realmGlucoseData.commitTransaction();
        realmGlucoseData.close();

        try{
            reader.close();
            // OK Toast.makeText(this, "Archivo leído con éxito", Toast.LENGTH_SHORT).show();
        }   catch (NullPointerException e){
            e.printStackTrace();
        }

        /*
        * CSVReader reader = new CSVReader(new FileReader("emps.csv"), ',');
        */
    }

    public void onNfcReadingFinished(ReadingData readingData) {
        mLastScanTime = new Date().getTime();
        onShowScanData(readingData);
        TidepoolSynchronization.getInstance().startTriggeredSynchronization(getApplicationContext());
    }


    @Override
    public void onShowScanData(ReadingData readingData) {
        ((DataPlotFragment) mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan))
                .showScan(readingData);
        mViewPager.setCurrentItem(getResources().getInteger(R.integer.viewpager_page_show_scan));
    }


    private void startContinuousSensorReadingTimer() {
        Timer continuousSensorReadingTimer = new Timer();
        mainActivity = this;
        TimerTask continuousSensorReadingTask = new TimerTask() {
            @Override
            public void run() {
                new NfcVReaderTask(mainActivity).execute(mLastNfcTag);
            }
        };
        continuousSensorReadingTimer.schedule(continuousSensorReadingTask, 0L, TimeUnit.SECONDS.toMillis(60L));
    }

    @Override
    protected void onNewIntent(Intent data) {
        super.onNewIntent(data);
        resolveIntent(data);
    }


    private void resolveIntent(Intent data) {
        this.setIntent(data);

        if ((data.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return;
        }

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(data.getAction())) {
            mLastNfcTag = data.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            long now = new Date().getTime();

            if (mContinuousSensorReadingFlag) {
                startContinuousSensorReadingTimer();

            } else if (now - mLastScanTime > 5000) {
                DataPlotFragment dataPlotFragment = (DataPlotFragment)
                        mSectionsPagerAdapter.getRegisteredFragment(R.integer.viewpager_page_show_scan);
                if (dataPlotFragment != null) {
                    dataPlotFragment.clearScanData();
                }

                new NfcVReaderTask(this).execute(mLastNfcTag);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PENDING_INTENT_TECH_DISCOVERED:
                // Resolve the foreground dispatch intent:
                resolveIntent(data);
                break;
            case 99: // The ImportFile Action ID
                if (resultCode == RESULT_OK){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                            String path = data.getData().getPath();
                            Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                            importPathFile.setText(path);
                            try {
                                importCSV(path);
                                Toast.makeText(this, R.string.import_finished, Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } // Read external storage has not been granted
                        else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                                // ES NECESARIO COMPROBAR LOS REQUESTPERMISSIONS(..., 1); A VER SI EL 1 ES EL CORRECTO !
                                    Toast.makeText(this, "Read external storage permission is " +
                                            "needed to import the data", Toast.LENGTH_SHORT).show();

                                // Request read external storage permission
                                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        } else{
                            // You can directly ask for the permission.
                            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        }
                    }
                }
                else{
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            case 112:   // export

                break;
        }
    }

    // @Override
    public void onRequestPermissionsResults(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: // Read_external_storage as I defined
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    public boolean isLogged() {
        return logged;
    }

    // The next methods are used for writing to external storage
    private boolean writeExternalStorage(){
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            return true;
        } else
            return false;
    }


}
