package com.demo.smileid.sid_sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.demo.smileid.sid_sdk.sidNet.InternetStateBroadCastReceiver;
import com.smileid.smileidui.CaptureType;
import com.smileid.smileidui.SIDCaptureManager;
import static com.demo.smileid.sid_sdk.SIDStringExtras.EXTRA_TAG_PREFERENCES_AUTH_TAGS;
import static com.demo.smileid.sid_sdk.SIDStringExtras.SHARED_PREF_USER_ID;
import static com.smileid.smileidui.IntentHelper.SMILE_REQUEST_RESULT_TAG;

public class SIDMainActivity extends BaseSIDActivity implements
        InternetStateBroadCastReceiver.OnConnectionReceivedListener {

    private static final int SMILE_ID_CARD_REQUEST_CODE = 777;
    private static final int SMILE_SELFIE_REQUEST_CODE = 778;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sid_activity_main);
    }

    private void resetJob() {
        jobType = -1;
        mUseMultipleEnroll = false;
        mUseOffLineAuth = false;
    }

    public void smileUIIDCardRegister(View view) {
        new SIDCaptureManager.Builder(this, CaptureType.SELFIE_AND_ID_CAPTURE, SMILE_ID_CARD_REQUEST_CODE).build().start();
    }


    public void smileUIRegister(View view) {
        new SIDCaptureManager.Builder(this, CaptureType.SELFIE, SMILE_SELFIE_REQUEST_CODE).build().start();
    }

    public void enroll(View view) {
        resetJob();
        startSelfieCapture(true, false);
    }

    public void enrollWithIdNo(View view) {
        resetJob();
        startSelfieCapture(true, true, false, false, true);
    }

    public void enrollWithIdCard(View view) {
        resetJob();
        startSelfieCapture(true);
    }

    public void reEnroll(View view) {
        resetJob();
        startSelfieCapture(true, false, false, true);
    }

    public void reEnrollWithId(View view) {
        resetJob();
        startSelfieCapture(true, true, false, true);
    }

    public void updateUseImage(View view) {
        resetJob();
        jobType = 8;
        startSelfieCapture(false, false, false, false);
    }

    public void multipleEnroll(View view) {
        resetJob();
        mUseMultipleEnroll = true;
        startSelfieCapture(true, false);
    }

    public void validateId(View view) {
        resetJob();
        startActivity(new Intent(this, SIDIDValidationActivity.class));
    }

    public void authenticate(View view) {
        resetJob();

        if (!hasSavedUser()) {
            enrolFirstDialog();
            return;
        }

        startSelfieCapture(false);
    }

    public void authenticateWithSavedData(View view) {
        resetJob();

        if (!hasSavedUser()) {
            enrolFirstDialog();
            return;
        }

        if (hasOfflineTags()) {
            showOfflineAuthDialog();
        } else {
            mUseOffLineAuth = true;
            startSelfieCapture(false);
        }
    }

    private void showOfflineAuthDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Would you like to use existing or add new offline jobs?");
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Use existing jobs",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(SIDMainActivity.this, SIDAuthResultActivity.class);
                        intent.putExtra(SIDStringExtras.EXTRA_TAG_OFFLINE_AUTH, true);
                        startActivity(intent);
                    }
                });

        builder1.setNegativeButton(
                "Add to existing Jobs",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mUseOffLineAuth = true;
                        startSelfieCapture(false);
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    private void enrolFirstDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("You have to enrol (register) first before you can authenticate");
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Register (Enroll)",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startSelfieCapture(true, false);
                    }
                });

        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    private boolean hasOfflineTags() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        String tags = sharedPreferences.getString(EXTRA_TAG_PREFERENCES_AUTH_TAGS, null);

        if (tags != null) {
            return tags.split(",").length > 0;
        }

        return false;
    }

    private boolean hasSavedUser() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        String tags = sharedPreferences.getString(SHARED_PREF_USER_ID, null);
        return !TextUtils.isEmpty(tags);
    }

    @Override
    public void onInternetStateChanged(boolean recovered) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SMILE_SELFIE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startActivity(
                    new Intent(this, SIDEnrollResultActivity.class) {
                        {
                            putExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, 4);
                            putExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO, data.getStringExtra(SMILE_REQUEST_RESULT_TAG));
                        }
                    }
                );
            } else {
                Toast.makeText(this, "Oops Smile ID UI Selfie did not return a success", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SMILE_ID_CARD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startActivity(
                    new Intent(this, SIDEnrollResultActivity.class) {
                        {
                            putExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, 1);
                            putExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO, data.getStringExtra(SMILE_REQUEST_RESULT_TAG));
                        }
                    }
                );
            } else {
                Toast.makeText(this, "Oops Smile ID UI Selfie and ID Card did not return a success", Toast.LENGTH_LONG).show();
            }
        }
    }
}