//package com.mobiledgex.sdkdemo;
//
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import GoogleSignIn // JT 18.10.24

    // JT 18.10.23
 class Account
 {
    var  ourInstance:Account
    var mGoogleSignInAccount: GIDGoogleUser? //GoogleSignInAccount?

    init () // JT 18.10.23
    {
        ourInstance =  Account()
        
    }
    
    
    
    func  getSingleton() -> Account
    {
        return ourInstance;
    }

    func setGoogleSignInAccount(_ account: GIDGoogleUser)
    {
        mGoogleSignInAccount = account
    }

    func  getGoogleSignInAccount() ->GIDGoogleUser
    {
        return mGoogleSignInAccount!;
        
    }

    func  isSignedIn() -> Bool
    {
        return mGoogleSignInAccount != nil;
    }

}
