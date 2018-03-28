package com.applozic.mobicomkit.sample;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.applozic.mobicommons.people.ALContactProcessor;
import com.crashlytics.android.Crashlytics;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import io.fabric.sdk.android.Fabric;

/**
 * Application Class for loading data prior to launcher activity and global app level implementations
 */

public class FRApplication extends Application implements ALContactProcessor {

    private static FRApplication fr_applicationInstance;

    public static FRApplication getInstance() {
//        initializeFarmRiseApplicationInstance();
        return fr_applicationInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();


        initializeFarmRiseApplicationInstance();

        //Initiate Crashlytics
        initializeFabric();


    }

    private void initializeFabric() {
        Fabric.with(this, new Crashlytics());
        Crashlytics.log("Users Device Id " + getDeviceId());
    }

    @SuppressLint("HardwareIds")
    public String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private void initializeFarmRiseApplicationInstance() {
        if (fr_applicationInstance == null) {
            fr_applicationInstance = this;
        }
    }

    /**
     * Returns the current version name of the App
     *
     * @return appVersion
     */
    public String getAppVersion() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            //Update version name on every release to PlayStore
            //Even if the package info is null, this will work
            return "2.0.0";
        }

    }


    // To avoid Multidex error
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public String processContact(String number, String defaultCountryCode) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }

        Phonenumber.PhoneNumber phoneNumber;
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        if (TextUtils.isEmpty(number)) {
            return "";
        }

        long contactNumber = 0;
        int countryCode = 0;

        try {
            phoneNumber = phoneUtil.parse(number, defaultCountryCode);
            if (phoneNumber.hasCountryCode()) {
                countryCode = phoneNumber.getCountryCode();
            }

            contactNumber = phoneNumber.getNationalNumber();
        } catch (Exception ex) {
            try {
                contactNumber = Long.parseLong(number);
            } catch (Exception e) {
                return number;
            }
        }
        return "+" + countryCode + contactNumber;
    }
}
