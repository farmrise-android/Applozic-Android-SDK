package com.applozic.mobicomkit.sample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.PushNotificationTask;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.account.user.UserLoginTask;
import com.applozic.mobicomkit.api.conversation.ApplozicMqttIntentService;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.people.UserIntentService;
import com.applozic.mobicomkit.broadcast.BroadcastService;
import com.applozic.mobicomkit.contact.database.ContactDatabase;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.MessageCommunicator;
import com.applozic.mobicomkit.uiwidgets.conversation.MobiComKitBroadcastReceiver;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.MobiComKitActivityInterface;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.MobiComQuickConversationFragment;
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

    private HashMap<String, Stack<Fragment>> mStacks;
    public static final String TAB_HOME = "tab_home";
    public static final String TAB_CHAT = "tab_chat";
    private BottomNavigationView bottomNavigationView;
    private String currentTab;
    private static int retry;
    ConversationUIService conversationUIService;
    MobiComQuickConversationFragment mobiComQuickConversationFragment;
    MobiComKitBroadcastReceiver mobiComKitBroadcastReceiver;
    protected ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initialize();

        //Manually displaying the first fragment - one time only
        bottomNavigationView.setSelectedItemId(R.id.action_home);
        mobiComQuickConversationFragment = new MobiComQuickConversationFragment();
        conversationUIService = new ConversationUIService(this, mobiComQuickConversationFragment);
        mobiComKitBroadcastReceiver = new MobiComKitBroadcastReceiver(this, conversationUIService);

        Intent lastSeenStatusIntent = new Intent(this, UserIntentService.class);
        lastSeenStatusIntent.putExtra(UserIntentService.USER_LAST_SEEN_AT_STATUS, true);
        startService(lastSeenStatusIntent);
        mActionBar = getSupportActionBar();
       // addFragment(this, mobiComQuickConversationFragment, ConversationUIService.QUICK_CONVERSATION_FRAGMENT); //here we are adding fragment
        //Used to select an item programmatically
        //bottomNavigationView.getMenu().getItem(2).setChecked(true);
    }

    private void goToFragment(Fragment selectedFragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, selectedFragment);
        fragmentTransaction.commit();
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
        Applozic.init(this, "orphan12e5101382f0cc751fa8c224");


    }

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
                    pushFragments(tabId, new MobiComQuickConversationFragment(), true);
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
        if (shouldAdd)
            mStacks.get(tag).push(fragment);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//        ft.addToBackStack(null);
        ft.commit();
    }

    public void popFragments() {
  /*
   *    Select the second last fragment in current tab's stack..
   *    which will be shown after the fragment transaction given below
   */
        Fragment fragment = mStacks.get(currentTab).elementAt(mStacks.get(currentTab).size() - 2);

  /*pop current fragment from stack.. */
        mStacks.get(currentTab).pop();

  /* We have the target fragment in hand.. Just show it.. Show a standard navigation animation*/
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        ft.replace(R.id.container, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
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

    private int id = 0;

    public void setId(int newsId) {
        this.id = newsId;
    }

    public int getId() {
        return id;
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
        super.onStop();
        final String deviceKeyString = MobiComUserPreference.getInstance(this).getDeviceKeyString();
        final String userKeyString = MobiComUserPreference.getInstance(this).getSuUserKeyString();
        Intent intent = new Intent(this, ApplozicMqttIntentService.class);
        intent.putExtra(ApplozicMqttIntentService.USER_KEY_STRING, userKeyString);
        intent.putExtra(ApplozicMqttIntentService.DEVICE_KEY_STRING, deviceKeyString);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mobiComKitBroadcastReceiver, BroadcastService.getIntentFilter());
        Intent subscribeIntent = new Intent(this, ApplozicMqttIntentService.class);
        subscribeIntent.putExtra(ApplozicMqttIntentService.SUBSCRIBE, true);
        startService(subscribeIntent);

        if (!Utils.isInternetAvailable(this)) {
            String errorMessage = getResources().getString(R.string.internet_connection_not_available);
            showErrorMessageView(errorMessage);
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mobiComKitBroadcastReceiver);
        super.onPause();
    }
    private void login() {
        UserLoginTask.TaskListener listener = new UserLoginTask.TaskListener() {

            @Override
            public void onSuccess(RegistrationResponse registrationResponse, final Context context) {
                //Basic settings...

                ApplozicClient.getInstance(context).setContextBasedChat(true).setHandleDial(true);

                Map<ApplozicSetting.RequestCode, String> activityCallbacks = new HashMap<ApplozicSetting.RequestCode, String>();
                activityCallbacks.put(ApplozicSetting.RequestCode.USER_LOOUT, HomeActivity.class.getName());
                ApplozicSetting.getInstance(context).setActivityCallbacks(activityCallbacks);

                if(MobiComUserPreference.getInstance(context).isRegistered()) {


                    //Set activity callbacks
                    /*Map<ApplozicSetting.RequestCode, String> activityCallbacks = new HashMap<ApplozicSetting.RequestCode, String>();
                    activityCallbacks.put(ApplozicSetting.RequestCode.MESSAGE_TAP, MainActivity.class.getName());
                    ApplozicSetting.getInstance(context).setActivityCallbacks(activityCallbacks);*/

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
                            Applozic.getInstance(context).getDeviceRegistrationId(), pushNotificationTaskListener, context);
                    pushNotificationTask.execute((Void) null);
                }

                ApplozicClient.getInstance(context).hideChatListOnNotification();
                pushFragments(TAB_CHAT, new MobiComQuickConversationFragment(), true);
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
        user.setUserId("skrold");
        user.setContactNumber("9959841814");

        new UserLoginTask(user, listener, this).execute((Void) null);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if(requestCode == 1011) {
                Intent intent = new Intent(this, ConversationActivity.class);
                intent.putExtra(ConversationUIService.CONVERSATION_ID, "");
                intent.putExtra(ConversationUIService.SEARCH_STRING, "");
                intent.putExtra(ConversationUIService.TAKE_ORDER, true);
                String userId = data.getStringExtra(ConversationUIService.USER_ID);
                Contact contact = new ContactDatabase(this).getContactById(userId);

                if (contact != null) {
                    intent.putExtra(ConversationUIService.USER_ID, contact.getUserId());
                    intent.putExtra(ConversationUIService.DISPLAY_NAME, contact.getDisplayName());
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

}
