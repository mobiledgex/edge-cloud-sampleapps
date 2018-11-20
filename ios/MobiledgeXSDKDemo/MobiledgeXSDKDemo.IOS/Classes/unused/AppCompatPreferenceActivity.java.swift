//package com.mobiledgex.sdkdemo;
//
//import android.content.res.Configuration;
//import android.os.Bundle;
//import android.preference.PreferenceActivity;
//import android.support.annotation.LayoutRes;
//import android.support.annotation.Nullable;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatDelegate;
//import android.support.v7.widget.Toolbar;
//import android.view.MenuInflater;
//import android.view.View;
//import android.view.ViewGroup;

import UIKit
/**
 * A {@link PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public  class AppCompatPreferenceActivity
        // JT 18.10.23 todo
//  extends PreferenceActivity
// abstract
{
    init () // JT 18.10.23  settings screen todo
    {
    }
    
    private var mDelegate:AppCompatDelegate

    
     func onCreate(_ savedInstanceState: Bundle) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    
     func onPostCreate(_ savedInstanceState: Bundle) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public func getSupportActionBar() ->ActionBar{
        return getDelegate().getSupportActionBar();
    }

    public func setSupportActionBar(_  toolbar: Toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    
    public func getMenuInflater() ->MenuInflater
{
        return getDelegate().getMenuInflater();
    }

    
    public func setContentView(  _ layoutResID: Int) {
        getDelegate().setContentView(layoutResID);
    }

    
    public func setContentView(_ view: UIView) {
        getDelegate().setContentView(view);
    }

    
    public func setContentView(_ view: UIView, _ params: ViewGroup.LayoutParams) {
        getDelegate().setContentView(view, params);
    }

    
    public func addContentView(_ view: UIView, _ params:ViewGroup.LayoutParams ) {
        getDelegate().addContentView(view, params);
    }

    
     func onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    
    func onTitleChanged( _ title: CharSequence, _ color: Int) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    
    public func onConfigurationChanged(_ newConfig: Configuration) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    
     func onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    
     func onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public func invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private func getDelegate() ->AppCompatDelegate {
        if (mDelegate == nil) {
            mDelegate = AppCompatDelegate.create(self, nil);
        }
        return mDelegate;
    }
}
