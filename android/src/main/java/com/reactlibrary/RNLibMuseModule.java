
package com.reactlibrary;


import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.choosemuse.libmuse.MuseManagerAndroid;

import android.util.Log;

//For obtaining permissions
import android.R;
import android.support.v4.content.PermissionChecker;
//import android.support.v4.content.ContextCompat;
//import android.support.v4.app.ActivityCompat;
//import android.content.pm.PackageManager;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Activity;

public class RNLibMuseModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  public static Activity mainActivity;
  private MuseManagerAndroid manager = null; //Not initialized in constructor, so it can't be final

  public RNLibMuseModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

  }

  @Override
  public String getName() {
    return "RNLibMuse";
  }

  @ReactMethod
  public void Init()
  {
    if (this.mainActivity == null)
      throw new RuntimeException("You must set RNLibMuseModule.mainActivity in the constructor of your app's MainActivity class");
    this.manager = MuseManagerAndroid.getInstance();
    this.manager.setContext(this.mainActivity);
    this.VerifyPermissions();
  }

  @ReactMethod
  public void startListening() {this.manager.startListening();}

  private void VerifyPermissions()
  {
    if (PermissionChecker.checkSelfPermission(this.mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED)
      return;

    DialogInterface.OnClickListener buttonListener =
      new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface dialog, int which)
        {
          RNLibMuseModule.mainActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
      };
      //Those com.reactlibrary. qualifiers are necessary, as
      //  the main module may/will have its own resources
      // TODO: Append a namespace to com.reactlibrary. to avoid conflicts with other native modules
      AlertDialog introDialog = new AlertDialog.Builder(this.mainActivity)
        .setTitle(com.reactlibrary.R.string.permission_dialog_title) //Do they see my changes?
        .setMessage(com.reactlibrary.R.string.permission_dialog_description)
        .setPositiveButton(com.reactlibrary.R.string.permission_dialog_understand, buttonListener)
        .create();
      introDialog.show();
  }
}
