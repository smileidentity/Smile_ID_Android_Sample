package com.demo.smileid.sid_sdk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smileidentity.libsmileid.utils.Version;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GoogleApiAvailability;

public class SIDMainActivity extends AppCompatActivity {

    private View mTakeSelfie;
    private TextView mVersion;
    private static final int RC_HANDLE_GMS = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sid_activity_main);
        mTakeSelfie = findViewById(R.id.take_selfie_btn);
        mVersion = findViewById(R.id.version_tv);
        mVersion.setText("lib:" + Version.name() + " - app:" + BuildConfig.VERSION_NAME);
        mTakeSelfie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!playServiceAvailable()) {
                    Toast.makeText(SIDMainActivity.this, "Install or update Play Service", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(SIDMainActivity.this, SIDSmileIDActivity.class);
                startActivity(intent);
            }
        });

    }

    private boolean playServiceAvailable() {
        /*int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SERVICE_MISSING ||
                resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                resultCode == ConnectionResult.SERVICE_DISABLED) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, RC_HANDLE_GMS);
            dlg.show();
        }

        return resultCode == ConnectionResult.SUCCESS;*/
        return true;
    }
}
