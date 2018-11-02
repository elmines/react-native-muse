//@flow
import { NativeModules, DeviceEventEmitter } from 'react-native';
import type {DeviceManager, DataPacket, ConnectionPacket, ConnectStatus} from "react-native-bci";
import {Observable, Observer} from "rxjs";
import Singleton from "flow-singleton";

const RNLibMuse = NativeModules.RNLibMuse;

export class MuseDeviceManager implements DeviceManager
{
  //PUBLIC, STATIC
  static getInstance(): MuseDeviceManager{return this.instance.getInstance();}

  //PUBLIC, INSTANCE

  //DeviceManager implementation
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
  devices(): Observable<Array<string>> {return this.devicesObservable.getInstance();}
  connections(): Observable<ConnectionPacket> {return this.connectionsObservable.getInstance();}
  connect(museID: string) : void {RNLibMuse.connect(museID);}
  startListening(): void {RNLibMuse.startListening();}
  stopListening(): void {RNLibMuse.stopListening();}

  //Not part of DeviceManager, but convenient
  search(): void {this.stopListening(); this.startListening();}


  //PRIVATE, STATIC
  static channelNames: Array<string> = RNLibMuse.getChannelNames();
  static samplingRate: number = 256; //TODO: Get this from the underlying native module



  static instance: Singleton<MuseDeviceManager> = new Singleton((): MuseDeviceManager =>
  {
    return new MuseDeviceManager();
  });

  static formatPacket(packet: any): DataPacket
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

  //PRIVATE, INSTANCE
  devicesObservable: Singleton<Observable<Array<string>>>;
  connectionsObservable: Singleton<Observable<ConnectionPacket>>;
  constructor()
  {
    if (MuseDeviceManager.instance.isInitialized())
      throw "Error: There can only be one MuseDeviceManager";
    RNLibMuse.Init();
    RNLibMuse.setBufferSize(64);
    this.devicesObservable = new Singleton((): Observable<Array<string>> => {
      return Observable.create((observer: Observer): void => {
        DeviceEventEmitter.addListener("OnMuseListChanged", (muses: Array<string>): void => {
          observer.next(muses);
        });
  	  });
    });
    this.connectionsObservable = new Singleton((): Observable<ConnectionPacket> => {
        return Observable.create((observer: Observer): void => {
          DeviceEventEmitter.addListener("ChangeMuseConnectionState",
            ([id: string, status: ConnectStatus]): void => {observer.next({id, status});}
          );
        });
    });
  }//End constructor

}//End MuseDeviceManager
