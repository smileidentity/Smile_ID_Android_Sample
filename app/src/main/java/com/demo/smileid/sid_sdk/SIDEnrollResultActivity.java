package com.demo.smileid.sid_sdk;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.smileid.sid_sdk.geoloc.SIDGeoInfos;
import com.demo.smileid.sid_sdk.sidNet.IdTypeUtil;
import com.demo.smileid.sid_sdk.sidNet.InternetStateBroadCastReceiver;
import com.demo.smileid.sid_sdk.sidNet.SIDNetworkingUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.hbb20.CountryCodePicker;
import com.smileidentity.libsmileid.core.RetryOnFailurePolicy;
import com.smileidentity.libsmileid.core.SIDConfig;
import com.smileidentity.libsmileid.core.SIDNetworkRequest;
import com.smileidentity.libsmileid.core.SIDResponse;
import com.smileidentity.libsmileid.core.SIDTagManager;
import com.smileidentity.libsmileid.core.idcard.IdCard;
import com.smileidentity.libsmileid.exception.SIDException;
import com.smileidentity.libsmileid.model.GeoInfos;
import com.smileidentity.libsmileid.model.PartnerParams;
import com.smileidentity.libsmileid.model.SIDMetadata;
import com.smileidentity.libsmileid.model.SIDNetData;
import com.smileidentity.libsmileid.model.SIDUserIdInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.demo.smileid.sid_sdk.SIDStringExtras.SHARED_PREF_JOB_ID;
import static com.demo.smileid.sid_sdk.SIDStringExtras.SHARED_PREF_USER_ID;

public class SIDEnrollResultActivity extends BaseSIDActivity implements SIDNetworkRequest.OnCompleteListener,
        SIDNetworkRequest.OnErrorListener,
        SIDNetworkRequest.OnUpdateListener,
        SIDNetworkRequest.OnEnrolledListener,
        InternetStateBroadCastReceiver.OnConnectionReceivedListener,
        View.OnClickListener {

    SIDNetworkRequest mSINetworkrequest;
    TextView mResultTv;
    Switch mAutoUpload;
    ProgressBar mLoadingProg;
    private TextView mConfidenceValueTv;
    private Button mUploadNowBtn, skipIdInfoBtn, setIdInfoBtn;
    private boolean mHasId = false, mHasNoIdCard;
    private boolean reenrollUser = false;
    private boolean mMultipleEnroll, mContinueWithIdInfo;
    private int enrollType;
    private SharedPreferences sharedPreferences;
    private InternetStateBroadCastReceiver mInternetStateBR;
    private SIDConfig mConfig;
    private CountryCodePicker ccp;
    private LinearLayout mLayoutNoCard, mMultipleEnrolActions;
    private Spinner sIdType;
    private TextInputLayout tiIdNumber, tiFirstName, tiLastName, tiDOB;
    private String mSelectedCountryName = "", mSelectedIdCard;
    private String mCurrentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sid_activity_enroll_result);
        mHasId = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_HAS_ID, false);
        mHasNoIdCard = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_HAS_NO_ID_CARD, false);
        reenrollUser = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_REENROLL, false);
        enrollType = getIntent().getIntExtra(SIDStringExtras.EXTRA_ENROLL_TYPE, -1);
        mMultipleEnroll = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_MULTIPLE_ENROLL, false);
        mContinueWithIdInfo = getIntent().getBooleanExtra(SIDStringExtras.EXTRA_MULTIPLE_ENROLL_ADD_ID_INFO, false);
        mCurrentTag = getIntent().getStringExtra(SIDStringExtras.EXTRA_TAG_FOR_ADD_ID_INFO);

        mResultTv = findViewById(R.id.result_tv);
        mLoadingProg = findViewById(R.id.loading_prog);
        mConfidenceValueTv = findViewById(R.id.confidence_val_tv);
        mUploadNowBtn = findViewById(R.id.sid_enroll_upload_now_btn);

        mAutoUpload = findViewById(R.id.auto_upload_chk);

        //SIDUserIDInfo testing
        ccp = findViewById(R.id.dialog_country_code_picker);
        ccp.setOnCountryChangeListener(new CountryCodePicker.OnCountryChangeListener() {
            @Override
            public void onCountrySelected() {
                getSelectedCountryName();
                populateIdCard();
            }
        });

        mLayoutNoCard = findViewById(R.id.layout_no_card);
        mLayoutNoCard.setVisibility((mHasNoIdCard || mContinueWithIdInfo) ? View.VISIBLE : View.GONE);

        if (mContinueWithIdInfo) {
            mAutoUpload.setVisibility(View.GONE);
            mUploadNowBtn.setVisibility(View.GONE);
        }

        mMultipleEnrolActions = findViewById(R.id.multiple_enrol_actions);
        mMultipleEnrolActions.setVisibility((mMultipleEnroll) ? View.VISIBLE : View.GONE);

        skipIdInfoBtn = findViewById(R.id.sid_skip_id_info);
        setIdInfoBtn = findViewById(R.id.set_id_info_btn);
        skipIdInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUseMultipleEnroll = true;
                startSelfieCapture(true, false);
                finish();
            }
        });
        setIdInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isIdInfoValid()) {
                    mUseMultipleEnroll = true;
                    saveUserIdInfo();
                    startSelfieCapture(true, false);
                    finish();
                }
            }
        });


        sIdType = findViewById(R.id.id_type_spin);
        sIdType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedIdCard = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        tiIdNumber = findViewById(R.id.ti_id_number);
        tiFirstName = findViewById(R.id.ti_first_name);
        tiLastName = findViewById(R.id.ti_last_name);
        tiDOB = findViewById(R.id.ti_d_o_b);
        tiDOB.setOnClickListener(this);
        tiDOB.getEditText().setOnClickListener(this);

        getSelectedCountryName();
        populateIdCard();


        mAutoUpload.setVisibility(mMultipleEnroll && !mContinueWithIdInfo ? View.VISIBLE : View.GONE);
        sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        mInternetStateBR = new InternetStateBroadCastReceiver();
        mInternetStateBR.setOnConnectionReceivedListener(this);
        mUploadNowBtn.setOnClickListener(this);
        mSINetworkrequest = new SIDNetworkRequest(this);
        mSINetworkrequest.setOnCompleteListener(this);
        mSINetworkrequest.set0nErrorListener(this);
        mSINetworkrequest.setOnUpdateListener(this);
        mSINetworkrequest.setOnEnrolledListener(this);
        mSINetworkrequest.initialize();
    }

    private String getSelectedCountryName() {
        return mSelectedCountryName = ccp.getSelectedCountryName();
    }

    private void saveUserIdInfo() {
        SIDTagManager sidTagManager = SIDTagManager.getInstance(this);
        SIDMetadata metadata = new SIDMetadata();
        setUserIdInfo(metadata);
        sidTagManager.saveConfig(mCurrentTag, mContinueWithIdInfo ? 1 : 4, SIDConfig.Mode.ENROLL, null, metadata, false, this);
    }

    private boolean isIdInfoValid() {
        List<String> errors = new ArrayList<>();
//        if (TextUtils.isEmpty(mSelectedCountryName)) {
//            errors.add("Country name is required");
//        }
//
//        if (TextUtils.isEmpty(mSelectedIdCard)) {
//            errors.add("ID type is required");
//        }
//        if (TextUtils.isEmpty(tiIdNumber.getEditText().getText())) {
//            errors.add("ID number is required");
//        }

        if (errors.size() > 0) {
            for (int i = 0; i < errors.size(); i++) {
                Toast.makeText(this, errors.get(i), Toast.LENGTH_SHORT).show();
            }
        }
        return (errors.size() == 0);
    }

    private SIDMetadata setUserIdInfo(SIDMetadata metadata) {

        SIDUserIdInfo userIdInfo = metadata.getSidUserIdInfo();
        userIdInfo.setCountry(mSelectedCountryName);
        userIdInfo.setFirstName(tiFirstName.getEditText().getText().toString());
        userIdInfo.setLastName(tiLastName.getEditText().getText().toString());
        userIdInfo.setIdNumber(tiIdNumber.getEditText().getText().toString());
        userIdInfo.setIdType(mSelectedIdCard.replace(" ", "_"));
        userIdInfo.additionalValue("dob", tiDOB.getEditText().getText().toString());
        return metadata;
    }


    private void upload(String tag) {
        SIDMetadata metadata = new SIDMetadata();

        if (mHasNoIdCard) {
            if (!isIdInfoValid()) {
                return;
            }
            setUserIdInfo(metadata);
            mLayoutNoCard.setVisibility(View.GONE);

        }
        SIDConfig sidConfig = createConfig(tag, metadata);


        if (SIDNetworkingUtils.haveNetworkConnection(this)) {
            mLoadingProg.setVisibility(View.VISIBLE);
            if (mMultipleEnroll) {
                mSINetworkrequest.submitAll(sidConfig);
            } else {
                mSINetworkrequest.submit(sidConfig);
            }
        } else {

            //No internet connection so you can cache this job and
            // later use submitAll() to submit all offline jobs

            SIDTagManager.getInstance(this).saveConfig(tag, sidConfig.getJobType(), sidConfig.getMode(), sidConfig.getGeoInformation(), sidConfig.getSIDMetadata(), sidConfig.isUseIdCard(), this);


            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }


    private SIDConfig createConfig(String tag) {
        return createConfig(tag, null);
    }

    private SIDConfig createConfig(String tag, SIDMetadata metadata) {
        SIDNetData data = new SIDNetData(this, SIDNetData.Environment.TEST);
        if (reenrollUser && !TextUtils.isEmpty(getSavedUserId())) {
            //USe the PartnerParams object to set the user id of the user to be reernolled.
            // Should be declared before the configuration is submitted
            setPartnerParamsForReenroll(metadata);
        }
        GeoInfos infos = SIDGeoInfos.getInstance().getGeoInformation();

        int jobType = mHasNoIdCard ? 1 : enrollType;
        boolean useIdCard = !mHasNoIdCard && mHasId;
        SIDConfig.Builder builder;
        if (metadata != null) {
            builder = new SIDConfig.Builder(this)
                    .setRetryOnfailurePolicy(getRetryOnFailurePolicy())
                    .setMode(SIDConfig.Mode.ENROLL)
                    .setSmileIdNetData(data)
                    .setGeoInformation(infos)
                    .setSIDMetadata(metadata)
                    .setJobType(jobType)
                    .useIdCard(useIdCard);

        } else {
            builder = new SIDConfig.Builder(this)
                    .setRetryOnfailurePolicy(getRetryOnFailurePolicy())
                    .setMode(SIDConfig.Mode.ENROLL)
                    .setSmileIdNetData(data)
                    .setGeoInformation(infos)
                    .setJobType(jobType)
                    .useIdCard(useIdCard);
        }
        mConfig = builder.build(mCurrentTag);
        return mConfig;
    }

    private RetryOnFailurePolicy getRetryOnFailurePolicy() {
        RetryOnFailurePolicy retryOnFailurePolicy = new RetryOnFailurePolicy();
        retryOnFailurePolicy.setRetryCount(10);
        retryOnFailurePolicy.setRetryTimeout(TimeUnit.SECONDS.toMillis(15));
        return retryOnFailurePolicy;
    }

    @Override
    public void onComplete() {
        Toast.makeText(this, "Job Complete", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(SIDException e) {
        mLoadingProg.setVisibility(View.GONE);
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

        if (mMultipleEnroll) {
            mResultTv.setTextColor(Color.RED);
            mResultTv.setText("TAG: " + e.getFailedTag() + "FAILED - remaining tags:" + mConfig.getIdleTags(this).size());
        }
        e.printStackTrace();
    }

    @Override
    public void onUpdate(int progress) {
        if (Color.BLACK != mResultTv.getCurrentTextColor()) {
            mResultTv.setTextColor(Color.BLACK);
        }
        mResultTv.setText("progress " + String.valueOf(progress) + " % ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mInternetStateBR.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mInternetStateBR.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSINetworkrequest.registerListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSINetworkrequest.unregisterListeners();
    }

    private void setPartnerParamsForReenroll(SIDMetadata metadata) {
        PartnerParams params = metadata.getPartnerParams();
        params.setUserId(getSavedUserId());
    }

    @Override
    public void onEnrolled(SIDResponse response) {
        //Get smile job id
        //response.getStatusResponse().getResult().getSmileJobID();
        saveUserId(response.getPartnerParams().getUserId(), response.getPartnerParams().getJobId());
        mLoadingProg.setVisibility(View.GONE);
        int color;
        String message;
        switch (response.getResultCodeState()) {
            case SIDResponse.SID_RESPONSE_ENROL_REJECTED:
            case SIDResponse.SID_RESPONSE_ID_REJECTED:
            case SIDResponse.SID_RESPONSE_ENROL_WID_REJECTED:
                //Enroll/Register was rejected
                color = Color.RED;
                message = getString(R.string.demo_enrolled_failed);
                break;
            case SIDResponse.SID_RESPONSE_ENROL_WID_PROV_APPROVED:
            case SIDResponse.SID_RESPONSE_ENROL_PROVISIONAL_APPROVAL:
                //Enroll/Register was provisionally approved
                message = getString(R.string.demo_provisionally_enrolled);
                color = Color.GRAY;
                break;
            case SIDResponse.SID_RESPONSE_IMAGE_NOT_USABLE:
                //Enroll/Register uploaded images were unsuable
                color = Color.RED;
                message = getString(R.string.demo_enrolled_image_unusable);
                break;
            case SIDResponse.SID_RESPONSE_ENROL_APPROVED:
            case SIDResponse.SID_RESPONSE_ENROL_WID_APPROVED:
                color = Color.GREEN;
                message = getString(R.string.demo_enrolled_successfully);
                break;
            default:
                color = Color.RED;
                message = getString(R.string.demo_enrolled_failed);
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
        mConfidenceValueTv.setVisibility(TextUtils.isEmpty(stringBuilder) ? View.GONE : View.VISIBLE);
        mConfidenceValueTv.setText(stringBuilder);
        mUploadNowBtn.setVisibility(View.GONE);
    }

    //Emulates partner data source.
    //The user id of the enrollee is to be saved for later use
    private void saveUserId(String userId, String jobId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SHARED_PREF_USER_ID, userId);
        editor.putString(SHARED_PREF_JOB_ID, jobId);
        editor.apply();
    }

    //Retreive user id to be saved
    private String getSavedUserId() {
        return sharedPreferences.getString(SHARED_PREF_USER_ID, "");
    }

    private void showDateDialog() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, monthOfYear, dayOfMonth);

                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                tiDOB.getEditText().setText(fmt.format(selectedDate.getTime()));
            }

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sid_enroll_upload_now_btn:
                upload(mCurrentTag);
                break;
            case R.id.ti_d_o_b:
            case R.id.ti_et_d_o_b:
                showDateDialog();
                break;
        }
    }

    @Override
    public void onInternetStateChanged(boolean recovered) {
        if (recovered && mAutoUpload.isChecked()) {
            upload(mCurrentTag);
        }
    }

    private void initSpinner(List<String> idTypes) {
        ArrayAdapter dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, idTypes);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sIdType.setAdapter(dataAdapter);
    }

    private void populateIdCard() {
        IdCard idCard = IdTypeUtil.idCards(mSelectedCountryName);
        initSpinner(idCard.getIdCards());
    }
}
