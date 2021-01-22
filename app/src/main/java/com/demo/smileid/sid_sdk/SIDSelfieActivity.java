package com.demo.smileid.sid_sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.demo.smileid.sid_sdk.sidNet.Misc;
import com.smileidentity.libsmileid.core.CameraSourcePreview;
import com.smileidentity.libsmileid.core.SelfieCaptureConfig;
import com.smileidentity.libsmileid.core.SmartSelfieManager;
import com.smileidentity.libsmileid.core.captureCallback.FaceState;
import com.smileidentity.libsmileid.core.captureCallback.OnFaceStateChangeListener;

import java.util.ArrayList;
import java.util.Calendar;

import static com.demo.smileid.sid_sdk.SIDStringExtras.EXTRA_TAG_PREFERENCES_AUTH_TAGS;

public class SIDSelfieActivity extends AppCompatActivity implements OnFaceStateChangeListener,
        SmartSelfieManager.OnCompleteListener, View.OnClickListener {

    CameraSourcePreview mPreview;
    private TextView mPromptTv;
    private SmartSelfieManager smartSelfieManager;
    private boolean mIsEnrollMode;
    private boolean mHasId;
    private boolean mUse258;
    private boolean mHasNoIdCard;
    private boolean reenrollUser;
    private int enrollType;
    View mMultiEnrollContainer;
    int tagCount = 0;
    private boolean mMultipleEnroll = false, mUseOffLineAuth = false;
    private ArrayList<String> mTagArrayList = new ArrayList<>();
    private String currentTag;
    private View mCapturePictureBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sid_activity_selfie);
        mIsEnrollMode = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_ENROLL_MODE, true);
        reenrollUser = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_REENROLL, false);
        mHasId = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_HAS_ID, true);
        mHasNoIdCard = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_HAS_NO_ID_CARD, true);
        mUse258 = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_USE_258, false);
        enrollType = getIntent().getIntExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, -1);
        mMultipleEnroll = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_MULTIPLE_ENROLL, false);
        mUseOffLineAuth = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_TAG_OFFLINE_AUTH, false);
        mPreview = findViewById(R.id.previewCamera);
        mPromptTv = findViewById(R.id.prompt_tv);
        mMultiEnrollContainer = findViewById(R.id.multiple_selfie_container_ll);
        Button mTakeAnotherSelfieBtn = findViewById(R.id.take_another_selfie_btn);
        mCapturePictureBtn = findViewById(R.id.capture_btn);

        if (mUseOffLineAuth) {
            findViewById(R.id.continue_with_id_btn).setVisibility(View.GONE);
            Button saveForLaterBtn = findViewById(R.id.save_for_later_btn);
            saveForLaterBtn.setOnClickListener(this);
            saveForLaterBtn.setVisibility(View.VISIBLE);
        }

        Button mContinueBtn = findViewById(R.id.continue_btn);
        mContinueBtn.setOnClickListener(this);

        Button continueWithIdBtn = findViewById(R.id.continue_with_id_btn);
        continueWithIdBtn.setOnClickListener(this);

        mCapturePictureBtn.setOnClickListener(this);
        mTakeAnotherSelfieBtn.setOnClickListener(this);
        smartSelfieManager = new SmartSelfieManager(getCaptureConfig());
        smartSelfieManager.setOnCompleteListener(this);
        smartSelfieManager.setOnFaceStateChangeListener(this);
        setBrightnessToMax(this);
        smartSelfieManager.captureSelfie(getTag());
    }


    @Override
    protected void onResume() {
        super.onResume();
        smartSelfieManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        smartSelfieManager.pause();
    }

    @Override
    public void onFaceStateChange(FaceState faceState) {
        switch (faceState) {
            case DO_SMILE:
                if ((mMultipleEnroll || mUseOffLineAuth) && !mMultiEnrollContainer.isShown()) {
                    mCapturePictureBtn.setVisibility(View.VISIBLE);
                } else {
                    mPromptTv.setText(getString(R.string.default_toast_smile));
                }
                break;
            case CAPTURING:
                mPromptTv.setText("");
                break;
            case DO_SMILE_MORE:
                mPromptTv.setText(getString(R.string.default_toast_smile_more));
                break;
            case NO_FACE_FOUND:
                mPromptTv.setText(getString(R.string.default_toast_face_in_oval));
                break;
            case DO_MOVE_CLOSER:
                mPromptTv.setText(getString(R.string.default_toast_text_move_closer));
                break;
        }
    }

    private void setBrightnessToMax(Activity activity) {
        // for brightness
        WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
        layout.screenBrightness = 1F;
        activity.getWindow().setAttributes(layout);
        // keep our app screen on
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onError(Throwable e) {
        String error = "Could not initialise selfie camera";
        if (e != null && e.getMessage() != null) {
            error = e.getMessage();
        }
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        e.printStackTrace();
    }

    @Override
    protected void onStop() {
        super.onStop();
        smartSelfieManager.stop();
    }

    @Override
    public void onComplete(Bitmap fullPreviewFrame) {
        if (fullPreviewFrame != null) {
            //Process returned full preview frame
        }
        if (mIsEnrollMode) {
            if (!mMultipleEnroll)
                startEnrollMode(false);
            else {
                mTagArrayList.add(currentTag);
                showRetakeSelfieButton();
                return;
            }
        } else {
            if (!mUseOffLineAuth) {
                Intent intent = new Intent(this, SIDAuthResultActivity.class);
                intent.putExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, enrollType);
                intent.putExtra(SIDStringExtras.EXTRA_USE_258, mUse258);
                intent.putExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO, currentTag);
                startActivity(intent);
            } else {
                mTagArrayList.add(currentTag);
                showRetakeSelfieButton();
                return;
            }
        }
        smartSelfieManager.stop();
        finish();
    }

    private void showRetakeSelfieButton() {
        mMultiEnrollContainer.setVisibility(View.VISIBLE);
        mCapturePictureBtn.setVisibility(View.GONE);
    }

    private void startEnrollMode(boolean continueWithId) {
        Class clazz;
        if (mHasId && !mHasNoIdCard) {
            clazz = SIDIDCardActivity.class;
        } else {
            clazz = SIDEnrollResultActivity.class;
        }
        Intent intent = new Intent(this, clazz);
        intent.putExtra(SIDStringExtras.EXTRA_REENROLL, reenrollUser);
        intent.putExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, enrollType);
        intent.putExtra(SIDStringExtras.EXTRA_MULTIPLE_ENROLL, mMultipleEnroll);
        intent.putExtra(SIDStringExtras.EXTRA_ENROLL_TAG_LIST, mTagArrayList);
        intent.putExtra(SIDStringExtras.EXTRA_HAS_NO_ID_CARD, mHasNoIdCard);
        intent.putExtra(SIDStringExtras.EXTRA_MULTIPLE_ENROLL_ADD_ID_INFO, continueWithId);
        intent.putExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO, currentTag);

        startActivity(intent);
    }

    private void startAuthMode() {
        Intent intent = new Intent(this, SIDAuthResultActivity.class);
        intent.putExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, enrollType);
        intent.putExtra(SIDStringExtras.EXTRA_USE_258, mUse258);
        intent.putExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO, currentTag);
        intent.putExtra(SIDStringExtras.EXTRA_TAG_OFFLINE_AUTH, mUseOffLineAuth);
        startActivity(intent);
    }

    private void saveAuthTagsForLater() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        String tagsString = sharedPreferences.getString(EXTRA_TAG_PREFERENCES_AUTH_TAGS, null);
        if (mUseOffLineAuth) {
            StringBuilder sb = new StringBuilder();
            if (tagsString != null) {
                sb = new StringBuilder(tagsString);
            }
            for (int i = 0; i < mTagArrayList.size(); i++) {
                sb.append(mTagArrayList.get(i)).append(",");
            }
            sharedPreferences.edit().putString(EXTRA_TAG_PREFERENCES_AUTH_TAGS, sb.toString()).apply();
        }
    }

    private SelfieCaptureConfig getCaptureConfig() {
        return new SelfieCaptureConfig.Builder(this)
                .setCameraType(/*mMultipleEnroll ? SelfieCaptureConfig.BACK_CAMERA :*/ SelfieCaptureConfig.FRONT_CAMERA)
                .setPreview(mPreview)
                .setManualSelfieCapture((mMultipleEnroll || mUseOffLineAuth))
                .setFlashScreenOnShutter(!mMultipleEnroll && !mUseOffLineAuth)
                .build();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.continue_btn:
                if (mUseOffLineAuth) {
                    saveAuthTagsForLater();
                    startAuthMode();
                } else {
                    startEnrollMode(false);
                }
                finish();
                break;
            case R.id.continue_with_id_btn:
                startEnrollMode(true);
                finish();
                break;
            case R.id.take_another_selfie_btn:
                mMultiEnrollContainer.setVisibility(View.GONE);
                smartSelfieManager.captureSelfie(getTag());
                smartSelfieManager.resume();
                break;
            case R.id.capture_btn:
                smartSelfieManager.takePicture();
                break;
            case R.id.save_for_later_btn:
                saveAuthTagsForLater();
                finish();
                break;
        }
    }

    private String getTag() {
        return currentTag = String.format(Misc.USER_TAG, DateFormat.format("MM_dd_hh_mm_ss", Calendar.getInstance().getTime()).toString());
    }
}