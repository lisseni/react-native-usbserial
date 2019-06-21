package com.bmateam.reactnativeusbserial;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import android.util.Log;
import java.io.IOException;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.UnsupportedEncodingException;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;


public class UsbSerialDevice {
    public UsbSerialPort port;
    public String portName;
    private ReactApplicationContext reactContext;
    private static final int SERIAL_TIMEOUT = 1000;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final SerialInputOutputManager.Listener mListener =
    new SerialInputOutputManager.Listener() {
      @Override
      public void onRunError(Exception e) {
        Log.v("BATRobot java", "Runner stopped.");
        disconnect();
      }
      @Override
      public void onNewData(final byte[] data) {
        //Log.v("BATRobot java", "onNewData");
        sendEvent(data);
      }
    };

    private void disconnect() {
      String UsbDisconnectName = "disconnect_"+this.portName;
      stopIoManager();
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbDisconnectName, this.portName);
    }


    public UsbSerialDevice(UsbSerialPort port,String portName) {
        this.port = port;
        this.portName = portName;

    }

    public void stopIoManager() {
    if (mSerialIoManager != null) {
      Log.i("BATRobot java", "Stopping io manager .."+this.portName);
      mSerialIoManager.stop();
      mSerialIoManager = null;
    }
  }

  public void startIoManager() {
      try{
        if (this.port == null) {
          throw new Exception(String.format("No device opened"));
        }

        if (this.port != null) {
          Log.v("BATRobot java", "Starting io manager .."+this.portName);
          mSerialIoManager = new SerialInputOutputManager(this.port, mListener);
          mExecutor.submit(mSerialIoManager);
        }

      }catch (Exception e) {
        e.printStackTrace();
      }

    }


    public void writeAsync(String value, Promise promise) {

        if (port != null) {

            try {
                port.write(value.getBytes(), SERIAL_TIMEOUT);

                promise.resolve(null);
            } catch (IOException e) {
                promise.reject(e);
            }

        } else {
            promise.reject(getNoPortErrorMessage());
        }
    }

    public void readAsync(Promise promise) {

        if (port != null) {
            try {
               byte buffer[] = new byte[16];
                int numBytesRead = this.port.read(buffer, 20);
                Log.v("BATRobot java", "read" + new String(buffer, "UTF-8"));
                promise.resolve(buffer);
            } catch (IOException e) {
                promise.reject(e);
            }
        } else {
            promise.resolve(getNoPortErrorMessage());
        }
    }

    public UsbSerialPort getPort(){
      return this.port;
  }

    private Exception getNoPortErrorMessage() {
        return new Exception("No port present for the UsbSerialDevice instance");
    }

    private void sendEvent(byte[] data) {
      WritableArray dataArray = Arguments.createArray();
      String eventName = "Data" + " " + this.portName;
      for (int i =0; i< data.length; i++) {
        dataArray.pushInt((data[i])&0xFF);

      }
      Log.i("BATRobot java", "sendEvent! " + eventName);
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, dataArray);
    }
}
