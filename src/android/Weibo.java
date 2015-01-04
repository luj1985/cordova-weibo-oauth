package com.ihavekey.weibo.oauth;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

public class Weibo extends CordovaPlugin {
    
    private SsoHandler mSsoHandler = null;
    private static final int WEIBO_NOT_INSTALLED = 1;
    private static final int AUTHORIZE_CANCELED = 2;
    private static final int UNKNOWN_ERROR = 99;

    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        if (action.equals("login")) {
            AuthInfo authInfo = this.createAuthInfo();
            this.login(authInfo, callbackContext);
            return true;
        }
        return false;
    }
    
    private AuthInfo createAuthInfo() {
        Activity activity = this.cordova.getActivity();
        String ns = activity.getPackageName();
        Resources resources = activity.getResources();

        int resKey = resources.getIdentifier("wb_app_key", "string", ns);
        int resUrl = resources.getIdentifier("wb_redirect_url", "string", ns);
        int resScope = resources.getIdentifier("wb_scope", "string", ns);

        String key = activity.getString(resKey);
        String url = activity.getString(resUrl);
        // scope is optional
        String scope = resScope == 0 ? "" : activity.getString(resScope);

        AuthInfo authInfo = new AuthInfo(activity, key, resUrl, scope);
        return authInfo;
    }

    private void login(AuthInfo authInfo, final CallbackContext context) {
        Activity activity = this.cordova.getActivity();
        mSsoHandler = new SsoHandler(activity, authInfo);
        if (mSsoHandler.isWeiboAppInstalled()) {
            this.cordova.setActivityResultCallback(this);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSsoHandler.authorizeClientSso(new AuthListener(context));
                }
            });
        } else {
            context.error(new ErrorMessage(WEIBO_NOT_INSTALLED, "Weibo not installed").toJSON());
        }
    }
    

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }
    
    class ErrorMessage {
        private int code;
        private String message;
        public ErrorMessage(int code, String message) {
            this.code = code;
            this.message = message;
        }
        JSONObject toJSON() {
            JSONObject json = new JSONObject();
            try {
                json.put("code", this.code);
                json.put("message", this.message);
            } catch(JSONException e) {
                throw new RuntimeException(e);
            }
            return json;
        }
    }

    class AuthListener implements WeiboAuthListener {
        private CallbackContext context;

        public AuthListener(CallbackContext context) {
            this.context = context;
        }

        @Override
        public void onComplete(Bundle values) {
            Oauth2AccessToken accessToken = Oauth2AccessToken
                    .parseAccessToken(values);
            if (accessToken != null && accessToken.isSessionValid()) {
                String uid = accessToken.getUid();
                String token = accessToken.getToken();

                JSONObject res = new JSONObject();
                try {
                    res.put("uid", uid);
                    res.put("token", token);
                    context.success(res);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            String message = e.getMessage();
            ErrorMessage m = new ErrorMessage(UNKNOWN_ERROR, message);
            context.error(m.toJSON());
            Log.e("Cordova-Weibo", message, e);
        }

        @Override
        public void onCancel() {
            String message = "authorize cancelled";
            context.error(new ErrorMessage(AUTHORIZE_CANCELED, message).toJSON());
            Log.i("Cordova-Weibo", message);
        }
    }
}
