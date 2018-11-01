//@flow

import { NativeModules } from 'react-native';

import {DeviceManager} from "react-native-bci";
import type {DeviceManager, DataPacket} from "react-native-bci";

import {Observable} from "rxjs";
import type {Observable} from "rxjs";

RNLibMuse = NativeModules.RNLibMuse;

export default MuseDeviceManager extends DeviceManager
{
  static channelNames: Array<string> = RNLibMuse.getChannelNames();
  static samplingRate: number = 256; //TODO: Get this from the underlying native module

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

  connect(museID: string) : void {RNLibMuse.connect(string);}
  startListening(): void {RNLibMuse.startListening();}
  stopListening(): void {RNLibMuse.stopListening();}

  //private
  formatPacket(packet: any): DataPacket
  {
    return {
      data: MuseDeviceManager.channelNames.map(channelName => packet[channelName]),
      timestamp: new Date(packet.timestamp), //eeg_packet.timestamp is in millliseconds since epoch
      info: {
        samplingRate: LibMuse.samplingRate,
        channelNames: MuseDeviceManager.channelNames
      }
    }
  }

}
