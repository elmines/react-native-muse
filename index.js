//@flow
import { NativeModules, DeviceEventEmitter } from 'react-native';
import type {DeviceManager} from "react-native-bci";
import type {DataPacket} from "react-native-bci";
import {Observable, Observer} from "rxjs";
import Singleton from "flow-singleton";

const RNLibMuse = NativeModules.RNLibMuse;

export class MuseDeviceManager implements DeviceManager
{
  static channelNames: Array<string> = RNLibMuse.getChannelNames();
  static samplingRate: number = 256; //TODO: Get this from the underlying native module

  static devicesInitialized: boolean = false;
  static devicesObservable: Observable<Array<string>>;

  //static initialized: boolean = false;
  //static instance: MuseDeviceManager;
  static instance: Singleton<MuseDeviceManager> = new Singleton((): MuseDeviceManager =>
  {
	const manager: MuseDeviceManager = new MuseDeviceManager();
	return manager;
  });


  static getInstance(): MuseDeviceManager{return this.instance.getInstance();}

  constructor()
  {
    if (MuseDeviceManager.instance.isInitialized())
      throw "Error: There can only be one MuseDeviceManager";
    RNLibMuse.Init();
    RNLibMuse.setBufferSize(64);
  }


  getChannelNames(): Array<string>{return MuseDeviceManager.channelNames;}

  data(): Observable<DataPacket>
  {
    const packetStream: Observable = Observable.create(function(observer){
      DeviceEventEmitter.addListener("MUSE_EEG", (buffer) => {
        buffer.forEach(packet => {observer.next(packet);});
      });
    });
    return packetStream;
  }

  devices(): Observable<Array<string>>
  {
	if (!MuseDeviceManager.devicesInitialized)
	{
		MuseDeviceManager.devicesObservable =
			Observable.create((observer: Observer): void =>
			{
				DeviceEventEmitter.addListener("OnMuseListChanged", (muses: Array<string>): void =>
				{
        				//if (muses.length > 0) museManager.connect(muses[0]);
					observer.next(muses);
				});
			});
	}

	return MuseDeviceManager.devicesObservable;
  }

  connect(museID: string) : void {RNLibMuse.connect(museID);}
  startListening(): void {RNLibMuse.startListening();}
  stopListening(): void {RNLibMuse.stopListening();}

  //Not part of DeviceManager, but convenient
  search(): void {this.stopListening(); this.startListening();}

  //private
  formatPacket(packet: any): DataPacket
  {
    return {
      data: MuseDeviceManager.channelNames.map(channelName => packet[channelName]),
      timestamp: new Date(packet.timestamp), //eeg_packet.timestamp is in millliseconds since epoch
      info: {
        samplingRate: RNLibMuse.samplingRate,
        channelNames: MuseDeviceManager.channelNames
      }
    }
  }

}
