'use strict';

import {
  Platform,
  NativeModules,
  DeviceEventEmitter
} from 'react-native';
//import UsbSerialDevice from './UsbSerialDevice';

const UsbSerialModule = NativeModules.UsbSerial;

export class UsbSerial {
  constructor() {

    if (Platform.OS != 'android') {
      throw 'Unfortunately only android is supported';
    }
  }

  getDeviceListAsync() {
    console.log("BATRobot index.js getDeviceListAsync");
    return UsbSerialModule.getDeviceListAsync();
  }

  openDeviceAsync(deviceObject = {}) {
    console.log("BATRobot index.js openDeviceAsync");
    return UsbSerialModule.openDeviceAsync(deviceObject).then((usbSerialDevNativeObject) => {
      //return new Promise((resolve)=>{
        //const usd = new UsbSerialDevice(UsbSerialModule, usbSerialDevNativeObject);
        console.log("BATRobot index.js resolve" + JSON.stringify(usbSerialDevNativeObject));
        return resolve(usbSerialDevNativeObject);
      })
      .catch((err)=>{
        return new Promise((reject)=>{
          return reject(err);
        })
      })
  //  });
  }

  getUsbPermission(deviceObject = {}){
    console.log("BATRobot index.js getUsbPermission");
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
      this.eventListener = undefined;
      //DeviceEventEmitter.removeListener('Data', handler)
    }
    console.log("BATRobot index.js monitor");
    this.eventListener = DeviceEventEmitter.addListener('Data',function(e: Event) {
      handler(e);
    });
  }

  write(deviceObject = {},cmd){
    console.log("BATRobot index.js write" + JSON.stringify(deviceObject));
    return UsbSerialModule.writeInDeviceAsync(deviceObject,cmd).then((res)=>{
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
    if(this.eventListener) {
      this.eventListener = undefined;
      //DeviceEventEmitter.removeListener('Data', handler)
    }
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
