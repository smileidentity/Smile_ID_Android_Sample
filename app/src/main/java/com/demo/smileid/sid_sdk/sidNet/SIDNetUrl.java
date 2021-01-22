package com.demo.smileid.sid_sdk.sidNet;


public interface SIDNetUrl {
    String AUTH_URL = "api/v2/#/auth_smile/";
    String PARTNER_PORT = "8080";
    String SID_PORT = "8443";

    String PARTNER_URL_PROD = "https://prod-smileid.herokuapp.com/";
    String PARTNER_URL_TEST = "https://test-smileid.herokuapp.com/";

    String LAMBDA_URL_PROD = "https://la7am6gdm8.execute-api.us-west-2.amazonaws.com/prod/";
    String LAMBDA_URL_TEST = "https://3eydmgh10d.execute-api.us-west-2.amazonaws.com/test/";

    String ID_VALIDATION_PROD = "https://la7am6gdm8.execute-api.us-west-2.amazonaws.com/prod/id_verification";
    String ID_VALIDATION_TEST = "https://3eydmgh10d.execute-api.us-west-2.amazonaws.com/test/id_verification";
}
