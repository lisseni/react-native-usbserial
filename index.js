'use strict';

import {
  Platform,
  NativeModules,
  DeviceEventEmitter
} from 'react-native';
//import UsbSerialDevice from './UsbSerialDevice';
//const EventEmitter = require('events')

const UsbSerialModule = NativeModules.UsbSerial;

export class UsbSerial {
  constructor() {

    if (Platform.OS != 'android') {
      throw 'Unfortunately only android is supported';
    }
    //let emitter = new EventEmitter;
  }

  getDeviceListAsync() {
    //console.log("BATRobot index.js getDeviceListAsync");
    return UsbSerialModule.getDeviceListAsync();
  }

  openDeviceAsync(deviceObject = {}) {
    //console.log("BATRobot index.js openDeviceAsync");
    return UsbSerialModule.openDeviceAsync(deviceObject).then((usbSerialDevNativeObject) => {
      //return new Promise((resolve)=>{
        //const usd = new UsbSerialDevice(UsbSerialModule, usbSerialDevNativeObject);
        //console.log("BATRobot index.js resolve" + JSON.stringify(usbSerialDevNativeObject));
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
    //console.log("BATRobot index.js getUsbPermission");
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

  monitor(deviceObject = {},handler){
    if(this.eventListener) {
      //console.log("BATRobot index.js monitor eventListener exists");
      this.eventListener = undefined;
      //DeviceEventEmitter.removeListener('Data', handler)
    }
    let eventName = 'Data ' + deviceObject.comName;
    //console.log("BATRobot index.js monitor " + eventName);
    this.eventListener = DeviceEventEmitter.addListener(eventName,function(e: Event) {
      //emitter.emit('java_event_test', eventName)
      handler(e);
    });
  }

  disconnectMonitor(handler){
    if(this.eventDiscListener) {
      //console.log("BATRobot index.js monitor eventListener exists");
      this.eventDiscListener = undefined;
      //DeviceEventEmitter.removeListener('Data', handler)
    }
    let eventName = 'Disconnect'; //+portName;
    //consoleeventDiscListener.log("BATRobot index.js monitor " + eventName);
    this.eventDiscListener = DeviceEventEmitter.addListener(eventName,function(e: Event) {
      console.log("BATRobot index.js DISCONNECT");
      handler(e);
    });
  }

  write(deviceObject = {},cmd){
    //console.log("BATRobot index.js write" + JSON.stringify(deviceObject));
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

  read(deviceObject = {}){
    console.log("BATRobot index.js read" + JSON.stringify(deviceObject));
    return resolve(0, 0);
    // return UsbSerialModule.readDeviceAsync(deviceObject).then((data, len)=>{
    // console.log("BATRobot index.js read" + data.toString('hex') + "len "+len);
    //   return new Promise((resolve)=>{
    //     return resolve(data, len);
    //   })
    // })
    // .catch((err)=>{
    //   return new Promise((reject)=>{
    //     return reject(err);
    //   })
    // });
  }

  close(deviceObject = {}){
    if(this.eventListener) {
      this.eventListener = undefined;
      //DeviceEventEmitter.removeListener('Data', handler)
    }
    if(this.eventDiscListener) {
      this.eventDiscListener = undefined;
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

  flush(deviceObject = {},flushReadBuffers, flushWriteBuffers){
    return UsbSerialModule.flushBuffers(deviceObject,flushReadBuffers, flushWriteBuffers);
  }

}
