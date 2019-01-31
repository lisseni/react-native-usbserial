'use strict';

import {
  Platform,
  NativeModules,
  DeviceEventEmitter
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

  openDeviceAsync(deviceObject = {}) {
    return UsbSerialModule.openDeviceAsync(deviceObject).then((usbSerialDevNativeObject) => {
      return new Promise((resolve)=>{
        const usd = new UsbSerialDevice(UsbSerialModule, usbSerialDevNativeObject);
        return resolve(usd);
      })
      .catch((err)=>{
        return new Promise((reject)=>{
          return reject(err);
        })
      })
    });
  }

  getUsbPermission(deviceObject = {}){
    return UsbSerialModule.getUsbPermission(deviceObject).then((res)=>{
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

  monitor(handler){
    if(this.eventListener) {
      this.eventListener.remove();
    }
    this.eventListener = DeviceEventEmitter.addListener('Data',function(e: Event) {
      handler(e);
    });
  }

  write(cmd){
    return UsbSerialModule.writeInDeviceAsync(cmd).then((res)=>{
      return new Promise((resolve)=>{
        return resolve(res);
      })
    })
    .catch((err)=>{
      return new Promise((reject)=>{
        return reject(err);
      })
    });
  }

  close(deviceObject = {}){
    return UsbSerialModule.closeDevice(deviceObject).then(()=>{
      return new Promise((resolve, reject)=>{
        return resolve();

      })
    })
    .catch((err)=>{
      return new Promise((resolve, reject)=>{
        return reject(err);
      })
    });
  }

  testUnbind() {
    return UsbSerialModule.testUnbind();
  }
  testBind() {
    return UsbSerialModule.testBind();
  }

  // flush(flushReadBuffers, flushWriteBuffers){
  //   return UsbSerialModule.flushBuffers(flushReadBuffers, flushWriteBuffers);
  // }

}
