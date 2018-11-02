package com.reactlibrary;

//BRIDGING
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

//EVENTS
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

//LIBMUSE
//Connecting
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
//Data
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseDataPacketType.*; //Need the unqualified enumeration constants
import com.choosemuse.libmuse.ConnectionState.*; //Need the unqualified enumeration constants
import com.choosemuse.libmuse.Eeg;

//UTILITIES
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

//ANDROID PERMISSIONS
import android.R;
import android.support.v4.content.PermissionChecker;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Activity;


//DEBUGGING
import android.util.Log;

public class RNLibMuseModule extends ReactContextBaseJavaModule {

  public static Activity mainActivity;
  public static final Object EEG_NAN = null; //Use the null reference to represent NaN

  @ReactMethod
  public static void setBufferSize(int bufferSize){RNLibMuseModule.bufferSize = bufferSize;}

  @Override
  public Map<String, Object> getConstants()
  {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("EEG_NAN", RNLibMuseModule.EEG_NAN);
    return constants;
  }

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
    this.dataListener       = createMuseDataListener();

    this.manager = MuseManagerAndroid.getInstance();
    this.manager.setContext(this.mainActivity);
    this.manager.setMuseListener(new MuseListener(){
      @Override
      public void museListChanged(){RNLibMuseModule.this.museListChanged();}
    });
    this.muses = this.manager.getMuses(); //FIXME: Just initialize to null?
    this.eegBuffer = RNLibMuseModule.createEegBuffer();
  }

  @ReactMethod
  public WritableArray getChannelNames()
  {
  	//private static final Eeg[] eegChannels = {Eeg.EEG1, Eeg.EEG2, Eeg.EEG3, Eeg.EEG4};
	WritableArray channelNames = Arguments.createArray();
	for (Eeg channel : eegChannels) channelNames.pushString(channel.toString());
	return channelNames;
  }

  @ReactMethod public void startListening() {this.manager.startListening();}
  @ReactMethod public void stopListening() {this.manager.stopListening();}

  @ReactMethod
  public void search()
  {
    this.stopListening();
    this.startListening();
  }


  @ReactMethod
  public void connect(String headbandName)
  {
    this.stopListening();
    for (Muse candidate : this.manager.getMuses())
    {
      if (candidate.getName().equals(headbandName))
      {
        this.muse = candidate;
        this.connectHelper();
        return;
      }
    }
  }

  private static final Eeg[] eegChannels = {Eeg.EEG1, Eeg.EEG2, Eeg.EEG3, Eeg.EEG4};
  private static       int   bufferSize  = 32; //Note this can be changed with setBufferSize()

  private final ReactApplicationContext reactContext;
  private MuseManagerAndroid                 manager;
  private MuseListener                  museListener;
  private MuseConnectionListener  connectionListener;
  private MuseDataListener              dataListener;
  private List<Muse>                           muses;
  private Muse                                  muse;

  /**
   * A WritableArray of WritableMap's,
   * where each WritableMap is a packet of EEG data from one timestamp
   */
  private List<WritableMap>             eegBuffer;

  private static List<WritableMap> createEegBuffer(){return new LinkedList<WritableMap>();}

  private static String connectionStateText(ConnectionState state)
  {
    switch (state)
    {
      case CONNECTED:     return "Connected";
      case CONNECTING:    return "Connecting";
      case DISCONNECTED:  return "Disconnected";
      case DISCONNECTING: return "Disconnecting";
      default:            return "Unknown";
    }
  }

  private void emitEvent(String name, Object args)
  {
    this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(name, args);
  }

  private void connectHelper()
  {
    this.muse.unregisterAllListeners();
    this.muse.registerConnectionListener(this.connectionListener);
    this.muse.registerDataListener(this.dataListener, MuseDataPacketType.EEG);
    this.muse.runAsynchronously();
  }

  /**
   * Emits OnMuseListChanged event to ReactNative
  */
  private void museListChanged()
  {
    this.muses = this.manager.getMuses();
    WritableArray eventArgs = Arguments.createArray();
    for (Muse element : this.muses) eventArgs.pushString(element.getName());
    this.emitEvent("OnMuseListChanged", eventArgs);
  }

  private void receiveMuseConnectionPacket(MuseConnectionPacket packet, Muse muse)
  {
     WritableArray arguments = Arguments.createArray();
     arguments.pushString(muse.getName());
     arguments.pushString(RNLibMuseModule.connectionStateText(
       packet.getCurrentConnectionState()));

     RNLibMuseModule.this.emitEvent("ChangeMuseConnectionState", arguments);
  }


  private MuseDataListener createMuseDataListener()
  {
    return new MuseDataListener()
    {
      @Override
      public void receiveMuseDataPacket(MuseDataPacket packet, Muse muse)
      {
        MuseDataPacketType type = packet.packetType();
        WritableMap data = null;

        switch(type)
        {
          case EEG:
            data = RNLibMuseModule.getEegChannelValues(packet);
            data.putDouble("timestamp", ((double) packet.timestamp() / 1000)); //Timestamp in milliseconds
            break;
          case ACCELEROMETER:
            break;
          case GYRO:
            break;
          default:
            break;
        }
        if (data != null)
        {
          RNLibMuseModule.this.eegBuffer.add(data);
          if (RNLibMuseModule.this.eegBuffer.size() >= RNLibMuseModule.bufferSize)
          {
            WritableNativeArray eventContent = Arguments.makeNativeArray(RNLibMuseModule.this.eegBuffer);
            RNLibMuseModule.this.emitEvent("MUSE_"+type.name(), eventContent);
            RNLibMuseModule.this.eegBuffer = RNLibMuseModule.createEegBuffer();
          }
        }
      }

      @Override
      public void receiveMuseArtifactPacket(MuseArtifactPacket packet, Muse muse)
      {
        //TODO: Implement this method
      }
    };
  }

  private static WritableNativeMap getEegChannelValues(MuseDataPacket packet)
  {
    //FIXME: That cast assumes knowledge of createMaps()'s implementation
    WritableNativeMap data = (WritableNativeMap) Arguments.createMap();
    for (Eeg channel : RNLibMuseModule.eegChannels)
    {
      data.putDouble(channel.name(), packet.getEegChannelValue(channel));
    }

    return data;
  }

  private MuseConnectionListener createMuseConnectionListener()
  {
    return new MuseConnectionListener(){
      @Override
      public void receiveMuseConnectionPacket(MuseConnectionPacket packet, Muse muse)
      {
        RNLibMuseModule.this.receiveMuseConnectionPacket(packet, muse);
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
