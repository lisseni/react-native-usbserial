'use strict';

import {
  Platform,
  NativeModules
} from 'react-native';
import UsbSerialDevice from './UsbSerialDevice';

const UsbSerialModule = NativeModules.UsbSerial;

export class UsbSerial {
    constructor() {
        if (Platform.OS != 'android') {
            throw 'Unfortunately only android is supported';
        }
    }

    getDeviceListAsync() {
        return UsbSerialModule.getDeviceListAsync();
    }

    list() {
        return UsbSerialModule.list();
    }

    openDeviceAsync(deviceObject = {}) {
        return UsbSerialModule.openDeviceAsync(deviceObject).then((usbSerialDevNativeObject) => {
            return new UsbSerialDevice(UsbSerialModule, usbSerialDevNativeObject);
        });
    }

    write(cmd){
        return UsbSerialModule.writeInDeviceAsync(cmd).then((res)=>{
          return new Promise((resolve, reject)=>{
            return resolve(res);
          })
        })
        .catch((res)=>{
          return new Promise((resolve, reject)=>{
            return reject(res);
          })
        });
    }
}
