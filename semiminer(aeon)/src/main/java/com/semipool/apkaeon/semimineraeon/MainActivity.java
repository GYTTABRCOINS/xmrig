/*
 *  ETN/AEON Miner App bobbieltd
 *  Monero Miner App (c) 2018 Uwe Post
 *  based on the XMRig Monero Miner https://github.com/xmrig/xmrig
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 * /
 */

package com.semipool.apkaeon.semimineraeon;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class MainActivity extends Activity {

    public static final String PREFS_NAME = "MyPrefsFile";
    private final static String[] SUPPORTED_ARCHITECTURES = {"aarch64", "arm64-v8a"};
    private final static String edMaxCpu = "99";

    private ScheduledExecutorService svc;
    private TextView tvLog;
    private EditText edPool,edUser;
    private EditText  edThreads;
    private TextView tvSpeed,tvAccepted,edPass;
    private CheckBox cbUseWorkerId;
    private boolean validArchitecture = true;
    private Random sprandom = new Random();
    private MiningService.MiningServiceBinder binder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableButtons(false);
        // settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        // wire views
        tvLog = findViewById(R.id.output);
        tvSpeed = findViewById(R.id.speed);
        tvAccepted = findViewById(R.id.accepted);
        edPool = findViewById(R.id.pool);
        edUser = findViewById(R.id.username);
        edPass = findViewById(R.id.pass);
        edThreads = findViewById(R.id.threads);
//        edMaxCpu = findViewById(R.id.maxcpu);
        cbUseWorkerId = findViewById(R.id.use_worker_id);

        // setting
        if (settings.contains("edPool")) {
            String sPoolURL = settings.getString("edPool", "");
            edPool.getText().clear();
            edPool.getText().append(sPoolURL);
        }
        if (settings.contains("edUser")) {
            String sEdUser = settings.getString("edUser", "");
            edUser.getText().clear();
            edUser.getText().append(sEdUser);
        }

        // check architecture
        if (!Arrays.asList(SUPPORTED_ARCHITECTURES).contains(Build.CPU_ABI.toLowerCase())) {
            Toast.makeText(this, "Sorry, this app currently only supports 64 bit architectures, but yours is " + Build.CPU_ABI, Toast.LENGTH_LONG).show();
            // this flag will keep the start button disabled
            validArchitecture = false;
        }

        // run the service
        Intent intent = new Intent(this, MiningService.class);
        bindService(intent, serverConnection, BIND_AUTO_CREATE);
        startService(intent);


    }

    private void startMining(View view) {
        if (binder == null) return;
        MiningService.MiningConfig cfg = binder.getService().newConfig(edUser.getText().toString(), edPool.getText().toString(),
                Integer.parseInt(edThreads.getText().toString()), Integer.parseInt(edMaxCpu), cbUseWorkerId.isChecked());
        binder.getService().startMining(cfg);
    }

    private void stopMining(View view) {
        binder.getService().stopMining();
    }

    private void saveSettings(View view) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        // save
        editor.putString("edUser", edUser.getText().toString());
        editor.putString("edPass", edPass.getText().toString());
        editor.putString("edPool", edPool.getText().toString());
        editor.putString("edThreads", edThreads.getText().toString());
        editor.putString("edMaxCpu", edMaxCpu);
        editor.putBoolean("cbUseWorkerId", cbUseWorkerId.isChecked());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // the executor which will load and display the service status regularly
        svc = Executors.newSingleThreadScheduledExecutor();
        svc.scheduleWithFixedDelay(this::updateLog, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onPause() {
        svc.shutdown();
        super.onPause();
    }

    private ServiceConnection serverConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (MiningService.MiningServiceBinder) iBinder;
            if (validArchitecture) {
                enableButtons(true);
                findViewById(R.id.start).setOnClickListener(MainActivity.this::startMining);
                findViewById(R.id.stop).setOnClickListener(MainActivity.this::stopMining);
                findViewById(R.id.save).setOnClickListener(MainActivity.this::saveSettings);
                int cores = binder.getService().getAvailableCores();
                // write suggested cores usage into editText
                int suggested = cores / 2;
                if (suggested == 0) suggested = 1;
                edThreads.getText().clear();
                edThreads.getText().append(Integer.toString(suggested));
                ((TextView) findViewById(R.id.cpus)).setText(String.format(" (%d %s)", cores, getString(R.string.cpus)));
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
            enableButtons(false);
        }
    };

    private void enableButtons(boolean enabled) {
        findViewById(R.id.start).setEnabled(enabled);
        findViewById(R.id.stop).setEnabled(enabled);
        findViewById(R.id.save).setEnabled(enabled);
    }


    private void updateLog() {
        runOnUiThread(()->{
            if (binder != null) {
                tvLog.setText(binder.getService().getOutput());
                tvAccepted.setText(Integer.toString(binder.getService().getAccepted()));
                tvSpeed.setText(binder.getService().getSpeed());
            }
        });
    }





}
