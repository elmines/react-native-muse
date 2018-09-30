
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.choosemuse.libmuse.MuseManagerAndroid;

import android.util.Log;

public class RNLibMuseModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  private final MuseManagerAndroid manager;

  public RNLibMuseModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    this.manager = MuseManagerAndroid.getInstance();
    Log.i("ReactNative","Yo, check out my instance of MuseManagerAndroid: "+this.manager);
  }

  @Override
  public String getName() {
    return "RNLibMuse";
  }

  @ReactMethod
  public void startListening() {this.manager.startListening(); Log.i("ReactNative", "Started listening. . .");}
}
