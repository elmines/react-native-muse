//@flow
import { NativeModules, DeviceEventEmitter } from 'react-native';
import type {DeviceManager} from "react-native-bci";
import type {DataPacket} from "react-native-bci";
import {Observable} from "rxjs";
const RNLibMuse = NativeModules.RNLibMuse;

export class MuseDeviceManager implements DeviceManager
{
  static channelNames: Array<string> = RNLibMuse.getChannelNames();
  static samplingRate: number = 256; //TODO: Get this from the underlying native module

  static initialized = false;
  static instance: MuseDeviceManager;

  static getInstance(): MuseDeviceManager
  {
    if (!MuseDeviceManager.initialized) MuseDeviceManager.instance = new MuseDeviceManager();
    MuseDeviceManager.initialized = true;
    return MuseDeviceManager.instance;
  }

  constructor()
  {
    if (MuseDeviceManager.instance) throw "Error: There can only be one MuseDeviceManager";
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

  connect(museID: string) : void {RNLibMuse.connect(museID);}
  search(): void {this.stopListening(); this.startListening();}
  startListening(): void {RNLibMuse.startListening();}
  stopListening(): void {RNLibMuse.stopListening();}

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
