package com.applozic.mobicomkit.sample;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.applozic.mobicommons.people.ALContactProcessor;
import com.crashlytics.android.Crashlytics;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import io.fabric.sdk.android.Fabric;

/**
 * Created by sunil on 21/3/16.
 */
public class ApplozicSampleApplication extends MultiDexApplication implements ALContactProcessor {

    @Override
    public void onCreate() {
        super.onCreate();
       Fabric.with(this, new Crashlytics());
    }


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
