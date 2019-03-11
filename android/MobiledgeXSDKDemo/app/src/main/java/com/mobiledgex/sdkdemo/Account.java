package com.mobiledgex.sdkdemo;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class Account {
    private GoogleSignInAccount mGoogleSignInAccount = null;
    private static final Account ourInstance = new Account();
    public static Account getSingleton() {
        return ourInstance;
    }

    public void setGoogleSignInAccount(GoogleSignInAccount account) {
        mGoogleSignInAccount = account;
    }

    public GoogleSignInAccount getGoogleSignInAccount() { return mGoogleSignInAccount; }

    public boolean isSignedIn() {
        return mGoogleSignInAccount != null;
    }

}
