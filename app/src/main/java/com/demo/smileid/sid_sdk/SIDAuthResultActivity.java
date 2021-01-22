package com.demo.smileid.sid_sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.demo.smileid.sid_sdk.geoloc.SIDGeoInfos;
import com.demo.smileid.sid_sdk.sidNet.SIDNetworkingUtils;
import com.smileidentity.libsmileid.core.RetryOnFailurePolicy;
import com.smileidentity.libsmileid.core.SIDConfig;
import com.smileidentity.libsmileid.core.SIDNetworkRequest;
import com.smileidentity.libsmileid.core.SIDResponse;
import com.smileidentity.libsmileid.core.SIDTagManager;
import com.smileidentity.libsmileid.exception.SIDException;
import com.smileidentity.libsmileid.model.GeoInfos;
import com.smileidentity.libsmileid.model.PartnerParams;
import com.smileidentity.libsmileid.model.SIDMetadata;
import com.smileidentity.libsmileid.model.SIDNetData;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.demo.smileid.sid_sdk.SIDStringExtras.EXTRA_TAG_PREFERENCES_AUTH_TAGS;


public class SIDAuthResultActivity extends AppCompatActivity implements SIDNetworkRequest.OnCompleteListener,
        SIDNetworkRequest.OnUpdateListener,
        SIDNetworkRequest.OnErrorListener,
        SIDNetworkRequest.OnAuthenticatedListener, View.OnClickListener {

    SIDNetworkRequest mSINetworkrequest;
    TextView mResultTv;
    ProgressBar mLoadingProg;
    private TextView mConfidenceValueTv;
    private Button mUploadNowBtn;
    private boolean useOfflineAuth;
    private boolean mUse258;
    private int jobType = -1;
    //Provided userid and job id
    private SharedPreferences sharedPreferences;
    private String mCurrentTag;
    private Queue<String> tagsQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sid_activity_auth_result);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mUse258 = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_USE_258, false);
        jobType = getIntent().getIntExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, -1);
        mCurrentTag = getIntent().getStringExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO);
        useOfflineAuth = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_TAG_OFFLINE_AUTH, false);
        sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        mResultTv = findViewById(R.id.result_tv);
        mLoadingProg = findViewById(R.id.loading_prog);
        mConfidenceValueTv = findViewById(R.id.confidence_val_tv);
        mUploadNowBtn = findViewById(R.id.sid_auth_upload_now_btn);
        if (useOfflineAuth) {
            mUploadNowBtn.setText("Auth using saved data");
            getOfflineAuthTags();
        }
        mUploadNowBtn.setOnClickListener(this);
    }


    private void createNetworkRequestManager() {
        mSINetworkrequest = new SIDNetworkRequest(this);
        mSINetworkrequest.setOnCompleteListener(this);
        mSINetworkrequest.setOnUpdateListener(this);
        mSINetworkrequest.setOnAuthenticatedListener(this);
        mSINetworkrequest.set0nErrorListener(this);
        mSINetworkrequest.initialize();
    }

    private void getOfflineAuthTags() {
        String tags = sharedPreferences.getString(EXTRA_TAG_PREFERENCES_AUTH_TAGS, null);
        if (tags != null) {
            String[] tagsArray = tags.split(",");
            for (int i = 0; i < tagsArray.length; i++) {
                tagsQueue.add(tagsArray[i]);
            }
        }
    }

    private void upload(SIDConfig config) {
        if (SIDNetworkingUtils.haveNetworkConnection(this)) {
            mLoadingProg.setVisibility(View.VISIBLE);
            mSINetworkrequest.submit(config);
        } else {
            //No internet connection so you can cache this job and
            // later use submitAll() to submit all offline jobs
            SIDTagManager.getInstance(this).saveConfig(config.getSubmittedTag(), config.getJobType(), config.getMode(), config.getGeoInformation(), config.getSIDMetadata(), config.isUseIdCard(), this);

            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private SIDConfig createConfig(SIDMetadata metadata) {
        SIDNetData data = new SIDNetData(this,SIDNetData.Environment.TEST);
        GeoInfos geoInfos = SIDGeoInfos.getInstance().getGeoInformation();
        //Uncomment to set user Provided partner Parameter
        //setPartnerParams();
        if ((jobType == 8) && !TextUtils.isEmpty(getSavedUserId())) {
            //USe the PartnerParams object to set the user id of the user to be reernolled.
            // Should be declared before the configuration is submitted
            setPartnerParamsForReenroll(metadata);
        }
        SIDConfig.Builder builder = new SIDConfig.Builder(this);
        builder.setRetryOnfailurePolicy(getRetryOnFailurePolicy())
                .setSmileIdNetData(data)
                .setGeoInformation(geoInfos)
                .setJobType(jobType)
                .setMode(SIDConfig.Mode.AUTHENTICATION);
        if (mUse258) {//Set the job type to 258 if user selected Auth 258 mode from the main screen
            builder.setJobType(258);
        }
        if (useOfflineAuth && tagsQueue.size() > 0) {
            return builder.build(tagsQueue.poll());
        } else {
            return builder.build(mCurrentTag);
        }
    }

    @NonNull
    private SIDConfig createConfig() {
        return createConfig(new SIDMetadata());
    }

    private RetryOnFailurePolicy getRetryOnFailurePolicy() {
        RetryOnFailurePolicy options = new RetryOnFailurePolicy();
        options.setRetryCount(15);
        options.setRetryTimeout(TimeUnit.SECONDS.toMillis(15));
        return options;
    }

    private void setPartnerParamsForReenroll(SIDMetadata metadata) {
        PartnerParams params = metadata.getPartnerParams();
        params.setUserId(getSavedUserId());
    }

    //Retreive user id to be saved
    private String getSavedUserId() {
        return sharedPreferences.getString("SHARED_PREF_USER_ID", "");
    }

    @Override
    public void onComplete() {
        mLoadingProg.setVisibility(View.GONE);
    }

    @Override
    public void onError(SIDException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        mLoadingProg.setVisibility(View.GONE);
    }

    @Override
    public void onUpdate(int progress) {
        if (Color.BLACK != mResultTv.getCurrentTextColor()) {
            mResultTv.setTextColor(Color.BLACK);
        }
        mResultTv.setText("progress " + String.valueOf(progress) + " % ");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onAuthenticated(SIDResponse response) {
        int color;
        String message;
        switch (response.getResultCodeState()) {
            case SIDResponse.SID_RESPONSE_AUTH_REJECTED:
            case SIDResponse.SID_RESPONSE_ID_REJECTED:
                //Auth was rejected
                color = Color.RED;
                message = getString(R.string.demo_auth_failed);
                break;
            case SIDResponse.SID_RESPONSE_UPDATE_PHOTO_REJECTED:
                //update photo was rejected
                color = Color.RED;
                message = getString(R.string.demo_update_image_failed);
                break;
            case SIDResponse.SID_RESPONSE_AUTH_PROVISIONAL_APPROVAL:
                //auth was provisionally approved
                message = getString(R.string.demo_provisionally_authed);
                color = Color.GRAY;
                break;
            case SIDResponse.SID_RESPONSE_UPDATE_PHOTO_PROV_APPROVAL:
                //update photo was provisionally approved
                message = getString(R.string.demo_update_photo_provisional);
                color = Color.GRAY;
                break;
            case SIDResponse.SID_RESPONSE_IMAGE_NOT_USABLE:
                //auth uploaded images were unsuable
                color = Color.RED;
                message = getString(R.string.demo_auth_image_unusable);
                break;
            case SIDResponse.SID_RESPONSE_UPDATE_PHOTO_APPROVED:
                //update photo approved
                color = Color.GREEN;
                message = getString(R.string.demo_update_image_success);
                break;
            case SIDResponse.SID_RESPONSE_AUTH_APPROVED:
                //auth approved
                color = Color.GREEN;
                message = getString(R.string.demo_auth_successfully);
                break;
            default:
                color = Color.RED;
                message = getString(R.string.demo_auth_failed);
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(response.getResultText())) {
            stringBuilder.append("Result Text : ")
                    .append(response.getResultText())
                    .append(System.getProperty("line.separator"));
        }
        if (response.getConfidenceValue() > 0) {
            stringBuilder.append(getString(R.string.demo_enrolled_confidence_value, response.getConfidenceValue()));
        }

        mResultTv.setTextColor(color);
        mResultTv.setText(message);
        mConfidenceValueTv.setVisibility(View.VISIBLE);
        mConfidenceValueTv.setText(stringBuilder);
        mUploadNowBtn.setVisibility(View.GONE);
        if (useOfflineAuth) {
            saveAuthTagsForLater();
            if (tagsQueue.size() > 0) {
                mConfidenceValueTv.setVisibility(View.INVISIBLE);
                mResultTv.setTextColor(Color.BLACK);
                createNetworkRequestManager();
                upload(createConfig());
            }
        }
    }

    private void saveAuthTagsForLater() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        String tags = null;
        if (tagsQueue.size() > 0) {
            StringBuilder sb = new StringBuilder();
            Iterator it = tagsQueue.iterator();
            while (it.hasNext()) {
                sb.append(it.next()).append(",");
            }
            tags = sb.toString();
        }
        sharedPreferences.edit().putString(EXTRA_TAG_PREFERENCES_AUTH_TAGS, tags).apply();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sid_auth_upload_now_btn:
                createNetworkRequestManager();
                upload(createConfig());
                break;
        }
    }
}
