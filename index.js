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

    list() {
        return UsbSerialModule.list();
    }

    // testhandler(param){
    //   if (param){
    //             console.warn(`BATrobot UsbSerialEvent param != undefined`);
    //   }
    //   else {
    //             console.warn(`BATrobot UsbSerialEvent param = undefined`);
    //   }
    // }

    openDeviceAsync(deviceObject = {}) {
        return UsbSerialModule.openDeviceAsync(deviceObject).then((usbSerialDevNativeObject) => {
          // DeviceEventEmitter.addListener('test', function(e: Event) {
          //     console.warn('BATROBOT test');
          // });
          if(this.eventListener) this.eventListener.remove();
          let temp = {rrrr:"dfsf"};
            this.eventListener = DeviceEventEmitter.addListener('UsbSerialEvent',function(e: Event) {
                console.warn('BATROBOT UsbSerialEvent test' + JSON.stringify(temp));

          });
          //this.eventListener = DeviceEventEmitter.addListener('UsbSerialEvent', testhandler);
            return new UsbSerialDevice(UsbSerialModule, usbSerialDevNativeObject);
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
      //UsbSerialModule.startIoManager();
      //UsbSerialModule.readDeviceAsync(deviceId);

    }
    // monitorDevice(handler) {
    //        if(this.eventListener) this.eventListener.remove();
    //        this.eventListener = DeviceEventEmitter.addListener('UsbSerialEvent', handler);
    // }

}
