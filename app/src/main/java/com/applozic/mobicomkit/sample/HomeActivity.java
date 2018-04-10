package com.applozic.mobicomkit.sample;

import android.*;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.PushNotificationTask;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.account.user.UserLoginTask;
import com.applozic.mobicomkit.api.conversation.ApplozicMqttIntentService;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.people.UserIntentService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.database.ContactDatabase;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.MessageCommunicator;
import com.applozic.mobicomkit.uiwidgets.conversation.MobiComKitBroadcastReceiver;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComKitActivityInterface;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.MobiComQuickConversationFragment;
import com.applozic.mobicomkit.uiwidgets.people.contact.DeviceContactSyncService;
import com.applozic.mobicomkit.uiwidgets.people.fragment.ProfileFragment;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Home Activity For Application
 */

public class HomeActivity extends AppCompatActivity implements MessageCommunicator, MobiComKitActivityInterface {

    public static final String TAB_HOME = "tab_home";
    public static final String TAB_CHAT = "tab_chat";
    private static int retry;
    protected ActionBar mActionBar;
    ConversationUIService conversationUIService;
    MobiComQuickConversationFragment mobiComQuickConversationFragment;
    MobiComKitBroadcastReceiver mobiComKitBroadcastReceiver;

    private HashMap<String, Stack<Fragment>> mStacks;
    private BottomNavigationView bottomNavigationView;
    private String currentTab;
    private boolean isStopCalled = false;
    // Request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {

                case R.id.action_home:
                    mStacks.get(TAB_HOME).clear();
                    selectedTab(TAB_HOME);
                    break;
                case R.id.action_chat:
                    mStacks.get(TAB_CHAT).clear();
                    selectedTab(TAB_CHAT);
                    break;

            }

            return true;

        }
    };
    private int id = 0;

    public static void addFragment(FragmentActivity fragmentActivity, Fragment fragmentToAdd, String fragmentTag) {
        FragmentManager supportFragmentManager = fragmentActivity.getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.layout_child_activity, fragmentToAdd,
                fragmentTag);

        if (supportFragmentManager.getBackStackEntryCount() > 1) {
            supportFragmentManager.popBackStackImmediate();
        }
        fragmentTransaction.addToBackStack(fragmentTag);
        fragmentTransaction.commitAllowingStateLoss();
        supportFragmentManager.executePendingTransactions();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initialize();
        showContacts();

        //Put Support Contact Data
        //buildSupportContactData();

        //Manually displaying the first fragment - one time only
        bottomNavigationView.setSelectedItemId(R.id.action_home);
        mobiComQuickConversationFragment = new MobiComQuickConversationFragment();
        conversationUIService = new ConversationUIService(this, mobiComQuickConversationFragment);
        mobiComKitBroadcastReceiver = new MobiComKitBroadcastReceiver(this, mobiComQuickConversationFragment);

        Intent lastSeenStatusIntent = new Intent(this, UserIntentService.class);
        lastSeenStatusIntent.putExtra(UserIntentService.USER_LAST_SEEN_AT_STATUS, true);
        startService(lastSeenStatusIntent);

        mActionBar = getSupportActionBar();
        //Used to select an item programmatically
        //bottomNavigationView.getMenu().getItem(2).setChecked(true);
        try {
            if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().getBoolean(ConversationUIService.FROM_GROUP_DELETE)) {
                bottomNavigationView.setSelectedItemId(R.id.action_chat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*  private void goToFragment(Fragment selectedFragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.layout_child_activity, selectedFragment);
        fragmentTransaction.commit();
    }*/

    /**
     * Show the contacts in the ListView.
     */
    private void showContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&

                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) !=
                        PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) override method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            Intent intent = new Intent(this, DeviceContactSyncService.class);
            DeviceContactSyncService.enqueueWork(this, intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we cannot display the names",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initialize() {

        //Set Up BottomNavigationView
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Set Up Tab Stacks
        mStacks = new HashMap<>();
        mStacks.put(TAB_HOME, new Stack<Fragment>());
        mStacks.put(TAB_CHAT, new Stack<Fragment>());


    }

    private void selectedTab(String tabId) {
        currentTab = tabId;

//        if (mStacks.get(tabId).size() == 0) {
      /*
       *    First time this tab is selected. So add first fragment of that tab.
       *    Don't need animation, so that argument is false.
       *    We are adding a new fragment which is not present in stack. So add to stack is true.
       */
        switch (tabId) {
            case TAB_HOME:
                pushFragments(tabId, new HomeFragment(), true);
                break;
            case TAB_CHAT:

                if (MobiComUserPreference.getInstance(this).isLoggedIn()) {
                   /* //pushFragments(tabId, new MobiComQuickConversationFragment(), true);
                    Intent intent = new Intent(this, ConversationActivity.class);
                    if (ApplozicClient.getInstance(this).isContextBasedChat()) {
                        intent.putExtra(ConversationUIService.CONTEXT_BASED_CHAT, true);
                    }
                    startActivity(intent);*/

                 addFragment(this, mobiComQuickConversationFragment, ConversationUIService.QUICK_CONVERSATION_FRAGMENT); //here we are adding fragment
                    pushFragments(tabId, mobiComQuickConversationFragment, true);

                } else {
                    login();
                }

                break;
        }
        /*} else {
      *//*
       *    We are switching tabs, and target tab is already has atleast one fragment.
       *    No need of animation, no need of stack pushing. Just show the target fragment
       *//*
            pushFragments(tabId, mStacks.get(tabId).lastElement(), false);
        }*/
    }

    public void pushFragments(String tag, Fragment fragment, boolean shouldAdd) {

        if (!isStopCalled) {

            if (!isFinishing() && !isDestroyed()) {
                FragmentManager manager = getSupportFragmentManager();
                if (fragment != null && getSupportFragmentManager() != null &&
                        !getSupportFragmentManager().isDestroyed()) {

                    if (shouldAdd)
                        mStacks.get(tag).push(fragment);
                    //FragmentManager manager = getSupportFragmentManager();
                    FragmentTransaction ft = manager.beginTransaction();
                    ft.replace(R.id.layout_child_activity, fragment);
                    //ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    //ft.addToBackStack(null);
                    ft.commit();
                }
            }
        }
    }

    private void popFragments() {

        Fragment fragment;

        /*pop current fragment from stack.. */
        if (mStacks.get(currentTab).get(mStacks.get(currentTab).size() - 1).getClass().getSimpleName().equals("ErrorFragment") ||
                mStacks.get(currentTab).get(mStacks.get(currentTab).size() - 1).getClass().getSimpleName().equals("NoInternetFragment")) {

            /*
             *  Select the second last fragment in current tab's stack..
             *  which will be shown after the fragment transaction given below
             */
            fragment = mStacks.get(currentTab).elementAt(mStacks.get(currentTab).size() - 3);

            mStacks.get(currentTab).pop();
            mStacks.get(currentTab).pop();
        } else {

            fragment = mStacks.get(currentTab).elementAt(mStacks.get(currentTab).size() - 2);
            mStacks.get(currentTab).pop();
        }

        /* We have the target fragment in hand.. Just show it.. Show a standard navigation animation*/
        if (!isStopCalled) {
            if (!isFinishing() && !isDestroyed()) {

                FragmentManager manager = getSupportFragmentManager();

                if (fragment != null && getSupportFragmentManager() != null && !getSupportFragmentManager().isDestroyed()) {

                    FragmentTransaction ft = manager.beginTransaction();
                    ft.replace(R.id.layout_child_activity, fragment);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                    ft.commit();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (mStacks.get(currentTab).size() == 1) {

            if (currentTab.equals(TAB_HOME)) {
                // We are already showing first fragment of current tab, so when back pressed, we will finish this activity..
                finish();
                return;
            } else {
                bottomNavigationView.setSelectedItemId(R.id.action_home);
                return;
            }
        }

    /* Goto previous fragment in navigation stack of this tab */
        popFragments();
    }

    public int getId() {
        return id;
    }

    public void setId(int newsId) {
        this.id = newsId;
    }

    @Override
    public void onQuickConversationFragmentItemClick(View view, Contact contact, Channel channel, Integer conversationId, String searchString) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationUIService.CONVERSATION_ID, conversationId);
        intent.putExtra(ConversationUIService.SEARCH_STRING, searchString);
        intent.putExtra(ConversationUIService.TAKE_ORDER, true);
        if (contact != null) {
            intent.putExtra(ConversationUIService.USER_ID, contact.getUserId());
            intent.putExtra(ConversationUIService.DISPLAY_NAME, contact.getDisplayName());
            startActivity(intent);
        } else if (channel != null) {
            intent.putExtra(ConversationUIService.GROUP_ID, channel.getKey());
            intent.putExtra(ConversationUIService.GROUP_NAME, channel.getName());
            startActivity(intent);

        }
    }

    @Override
    public void startContactActivityForResult() {
        conversationUIService.startContactActivityForResult();
    }

    @Override
    public void addFragment(ConversationFragment conversationFragment) {

    }

    @Override
    public void updateLatestMessage(Message message, String formattedContactNumber) {
        conversationUIService.updateLatestMessage(message, formattedContactNumber);
    }

    @Override
    public void removeConversation(Message message, String formattedContactNumber) {
        conversationUIService.removeConversation(message, formattedContactNumber);
    }

    @Override
    public void showErrorMessageView(String errorMessage) {

    }

    @Override
    public void retry() {
        retry++;
    }

    @Override
    public int getRetryCount() {
        return retry;
    }

    /*@Override
    public void onBackPressed() {

        int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            finishAffinity();

            //additional code
        } else {
            getFragmentManager().popBackStack();
        }

    }*/
    @Override
    protected void onStop() {
        isStopCalled = true;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStopCalled = false;
        LocalBroadcastManager.getInstance(this).registerReceiver(mobiComKitBroadcastReceiver, BroadcastService.getIntentFilter());
        Intent subscribeIntent = new Intent(this, ApplozicMqttIntentService.class);
        subscribeIntent.putExtra(ApplozicMqttIntentService.SUBSCRIBE, true);
        ApplozicMqttIntentService.enqueueWork(HomeActivity.this, subscribeIntent);

        if (!Utils.isInternetAvailable(this)) {
            String errorMessage = getResources().getString(R.string.internet_connection_not_available);
            showErrorMessageView(errorMessage);
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mobiComKitBroadcastReceiver);
        final String deviceKeyString = MobiComUserPreference.getInstance(this).getDeviceKeyString();
        final String userKeyString = MobiComUserPreference.getInstance(this).getSuUserKeyString();
        Intent intent = new Intent(this, ApplozicMqttIntentService.class);
        intent.putExtra(ApplozicMqttIntentService.USER_KEY_STRING, userKeyString);
        intent.putExtra(ApplozicMqttIntentService.DEVICE_KEY_STRING, deviceKeyString);
        ApplozicMqttIntentService.enqueueWork(HomeActivity.this, intent);
        super.onPause();
    }

    private void login() {

        UserLoginTask.TaskListener listener = new UserLoginTask.TaskListener() {

            @Override
            public void onSuccess(RegistrationResponse registrationResponse, final Context context) {
                //Basic settings...

                ApplozicClient.getInstance(context).setContextBasedChat(true).setHandleDial(true);

                ApplozicClient.getInstance(context).enableDeviceContactSync(true);


                /*Map<ApplozicSetting.RequestCode, String> activityCallbacks = new HashMap<ApplozicSetting.RequestCode, String>();
                activityCallbacks.put(ApplozicSetting.RequestCode.USER_LOOUT, HomeActivity.class.getName());
                ApplozicSetting.getInstance(context).setActivityCallbacks(activityCallbacks);*/

                if (MobiComUserPreference.getInstance(context).isRegistered()) {

                    //Set activity callbacks
                    Map<ApplozicSetting.RequestCode, String> activityCallbacks = new HashMap<ApplozicSetting.RequestCode, String>();
                    activityCallbacks.put(ApplozicSetting.RequestCode.MESSAGE_TAP, ConversationActivity.class.getName());
                    ApplozicSetting.getInstance(context).setActivityCallbacks(activityCallbacks);

                    //Start GCM registration....
                    PushNotificationTask pushNotificationTask = null;
                    PushNotificationTask.TaskListener pushNotificationTaskListener = new PushNotificationTask.TaskListener() {
                        @Override
                        public void onSuccess(RegistrationResponse registrationResponse) {

                        }

                        @Override
                        public void onFailure(RegistrationResponse registrationResponse, Exception exception) {

                        }
                    };
                    pushNotificationTask = new PushNotificationTask(
                            Applozic.getInstance(context).getDeviceRegistrationId(),
                            pushNotificationTaskListener, context);
                    pushNotificationTask.execute((Void) null);
                }

                ApplozicClient.getInstance(context).hideChatListOnNotification();

                /*Intent intent = new Intent(HomeActivity.this, ConversationActivity.class);
                if (ApplozicClient.getInstance(HomeActivity.this).isContextBasedChat()) {
                    intent.putExtra(ConversationUIService.CONTEXT_BASED_CHAT, true);
                }
               //startActivity(intent);*/

                addFragment(HomeActivity.this, mobiComQuickConversationFragment, ConversationUIService.QUICK_CONVERSATION_FRAGMENT); //here we are adding fragment
                pushFragments(TAB_CHAT, mobiComQuickConversationFragment, true);

            }

            @Override
            public void onFailure(RegistrationResponse registrationResponse, Exception exception) {
                AlertDialog alertDialog = new AlertDialog.Builder(getApplicationContext()).create();
                alertDialog.setTitle("FarmRise");
                alertDialog.setMessage(exception.toString());
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(com.applozic.mobicomkit.uiwidgets.R.string.ok_alert),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                if (!isFinishing()) {
                    alertDialog.show();
                }
            }
        };

        User user = new User();
        user.setUserId("userDevice");
        //user.setEmail("pskreddy"@gmail.com);
        user.setPassword("123");
        user.setDisplayName("LG"); //display name and user name are similar //Sai //In all contacts, conatct shows with display name and number
        //user.setAuthenticationTypeId(authenticationType.getValue());


        new UserLoginTask(user, listener, this).execute((Void) null);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == 1011) {
                Intent intent = new Intent(this, ConversationActivity.class);
                intent.putExtra(ConversationUIService.CONVERSATION_ID, "");
                intent.putExtra(ConversationUIService.SEARCH_STRING, "");
                intent.putExtra(ConversationUIService.TAKE_ORDER, true);
                String userId = data.getStringExtra(ConversationUIService.USER_ID);
                Integer groupId = data.getIntExtra(ConversationUIService.GROUP_ID, 0);
                Contact contact = new ContactDatabase(this).getContactById(userId);

                if (contact != null) {
                    intent.putExtra(ConversationUIService.USER_ID, contact.getUserId());
                    intent.putExtra(ConversationUIService.DISPLAY_NAME, contact.getDisplayName());
                    startActivity(intent);
                } else if (groupId != 0) {
                    intent.putExtra(ConversationUIService.GROUP_ID, groupId);
                    startActivity(intent);
                }
            }
            //  conversationUIService.onActivityResult(requestCode, resultCode, data);
            // handleOnActivityResult(requestCode, data);
           /* if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        return;
                    }
                    if (imageUri != null) {
                        imageUri = result.getUri();
                        if (imageUri != null && profilefragment != null) {
                            profilefragment.handleProfileimageUpload(true, imageUri, profilePhotoFile);
                        }
                    } else {
                        imageUri = result.getUri();
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_" + ".jpeg";
                        profilePhotoFile = FileClientService.getFilePath(imageFileName, this, "image/jpeg");
                        if (imageUri != null && profilefragment != null) {
                            profilefragment.handleProfileimageUpload(true, imageUri, profilePhotoFile);
                        }
                    }
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Utils.printLog(this, ConversationActivity.class.getName(), "Cropping failed:" + result.getError());
                }
            }
            if (requestCode == LOCATION_SERVICE_ENABLE) {
                if (((LocationManager) getSystemService(Context.LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    googleApiClient.connect();
                } else {
                    Toast.makeText(this, com.applozic.mobicomkit.uiwidgets.R.string.unable_to_fetch_location, Toast.LENGTH_LONG).show();
                }
                return;
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void handleOnActivityResult(int requestCode, Intent intent) {

        switch (requestCode) {

            case ProfileFragment.REQUEST_CODE_ATTACH_PHOTO:
                Uri selectedFileUri = (intent == null ? null : intent.getData());
               /* imageUri = null;
                beginCrop(selectedFileUri);*/
                break;

            case ProfileFragment.REQUEST_CODE_TAKE_PHOTO:
                // beginCrop(imageUri);
                break;

        }
    }

    private void buildSupportContactData() {
        Context context = getApplicationContext();
        AppContactService appContactService = new AppContactService(context);
        // avoid each time update ....
        if (!appContactService.isContactExists(getString(R.string.support_contact_userId))) {
            Contact contact = new Contact();
            contact.setUserId(getString(R.string.support_contact_userId));
            contact.setFullName(getString(R.string.support_contact_display_name));
            contact.setContactNumber(getString(R.string.support_contact_number));
            contact.setImageURL(getString(R.string.support_contact_image_url));
            contact.setEmailId(getString(R.string.support_contact_emailId));
            appContactService.add(contact);
        }
    }

}
