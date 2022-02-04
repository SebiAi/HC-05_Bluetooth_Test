package com.sebiai.hc_05bluetoothtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class TestActivity extends AppCompatActivity {
    TestActivity.mReceiver receiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setResult(Activity.RESULT_OK);

        if (receiver == null) {
            receiver = new TestActivity.mReceiver(this);
            IntentFilter filter = new IntentFilter(Constants.INTENT_BT_MESSAGE);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        }
    }

    @Override
    protected void onStop() {
        if (receiver != null)
            unregisterReceiver(receiver);
        super.onStop();
    }

    private static class mReceiver extends BroadcastReceiver {
        private final WeakReference<TestActivity> mActivity;

        public mReceiver(TestActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // No need to check intent action - only one intent will trigger this
            final TestActivity activity = mActivity.get();

            Bundle extras = intent.getExtras();
            switch (extras.getInt(Constants.KEY_WHAT)) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if (extras.getInt(Constants.KEY_STATE) == Constants.STATE_ERROR) {
                        activity.setResult(Constants.STATE_ERROR);
                        activity.finish();
                    }
                    break;
                case Constants.MESSAGE_READ:
                    String message = extras.getString(Constants.KEY_MESSAGE);
                    Toast.makeText(activity, "R: " + message, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}