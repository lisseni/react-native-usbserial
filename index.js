'use strict';

import {
  Platform,
  NativeModules
} from 'react-native';
import UsbSerialDevice from './UsbSerialDevice';
import { DeviceEventEmitter } from 'react-native';

const UsbSerialModule = NativeModules.UsbSerial;

export class UsbSerial {
  constructor() {
    if (Platform.OS != 'android') {
      throw 'Unfortunately only android is supported';
    }
  }

  getDeviceListAsync() {
    console.log('BATrobot getDeviceListAsyn')
    return UsbSerialModule.getDeviceListAsync();
  }

  openDeviceAsync(deviceObject = {}) {
    return UsbSerialModule.openDeviceAsync(deviceObject).then((usbSerialDevNativeObject) => {
      const usd = new UsbSerialDevice(UsbSerialModule, usbSerialDevNativeObject);
      if(this.eventListener) {
        this.eventListener.remove();
      }
      let temp = {rrrr:"dfsf"};
      this.eventListener = DeviceEventEmitter.addListener('UsbSerialEvent',function(e: Event) {
        this.emit('newData', e);
        //console.warn('SerialEvent test' + JSON.stringify(temp));
      });
      return resolve(usd);
    })
    .catch((err)=>{
      return reject(err);
    });
  }

  write(cmd){
    return UsbSerialModule.writeInDeviceAsync(cmd).then((res)=>{
      return new Promise((resolve, reject)=>{
        //this.emit('newData');
        return resolve(res);
      })
    })
    .catch((res)=>{
      return new Promise((resolve, reject)=>{
        return reject(res);
      })
    });
  }
  readOn(deviceId){

  }
}
