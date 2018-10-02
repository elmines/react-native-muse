package com.reactlibrary;

//Bridge modules
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

//Emitting events
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

//Libmuse
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseConnectionListener;

//Java utilities
import java.util.List;
import java.util.LinkedList;

//For obtaining permissions
import android.R;
import android.support.v4.content.PermissionChecker;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Activity;

//Debugging
import android.util.Log;

public class RNLibMuseModule extends ReactContextBaseJavaModule {

  public static Activity mainActivity;

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
    this.VerifyPermissions();

    this.connectionListener = createMuseConnectionListener();

    this.manager = MuseManagerAndroid.getInstance();
    this.manager.setContext(this.mainActivity);
    this.manager.setMuseListener(new MuseListener(){
      @Override
      public void museListChanged(){RNLibMuseModule.this.museListChanged();}
    });
    //this.manager.setMuseConnection
    this.muses = this.manager.getMuses(); //FIXME: Just initialize to null?

  }

  @ReactMethod
  public void search()
  {
    this.stopListening();
    this.startListening();
  }

  /**
  *
  */
  @ReactMethod
  public void connect(String headbandName)
  {
    this.stopListening();
    for (Muse candidate : this.muses)
    {
      if (candidate.getName() == headbandName)
      {
        this.muse = candidate;
        break;
      }
    }
    this.muse.unregisterAllListeners();
    this.muse.registerConnectionListener(this.connectionListener);
    this.muse.runAsynchronously();
  }

  private final ReactApplicationContext reactContext;
  private MuseManagerAndroid manager; //Not initialized in constructor, so it can't be final
  private MuseListener museListener;
  private MuseConnectionListener connectionListener;
  private List<Muse> muses;
  private Muse muse;
  private ConnectionState connectionState;

  private void startListening() {this.manager.startListening(); Log.i("ReactNative", "Started listening");}
  private void stopListening() {this.manager.stopListening();}

  private void emitEvent(String name, WritableArray args)
  {
    this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name, args);
  }

  /**
   * Emits OnMuseListChanged event to ReactNative
  */
  private void museListChanged()
  {
    Log.i("ReactNative", "Called museListChanged()");
    this.muses = this.manager.getMuses();
    WritableArray eventArgs = Arguments.createArray();
    for (Muse element : this.muses) eventArgs.pushString(element.getName());
    this.emitEvent("OnMuseListChanged", eventArgs);
  }

  private void receiveMuseConnectionPacket(MuseConnectionPacket packet, Muse muse)
  {
     final ConnectionState currState = packet.getCurrentConnectionState();
     Log.i("ReactNative", packet.getPreviousConnectionState() + "->" + currState);
  }

  private MuseConnectionListener createMuseConnectionListener()
  {
    return new MuseConnectionListener(){
      @Override
      public void receiveMuseConnectionPacket(MuseConnectionPacket packet, Muse muse)
      {
        this.receiveMuseConnectionPacket(packet, muse);
      }
    };
  }

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
