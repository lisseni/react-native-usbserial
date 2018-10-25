package com.bmateam.reactnativeusbserial;

import com.facebook.react.bridge.Promise;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import android.util.Log;
import java.io.IOException;

public class UsbSerialDevice {
    public UsbSerialPort port;
    private static final int SERIAL_TIMEOUT = 1000;

    public UsbSerialDevice(UsbSerialPort port) {
        this.port = port;
        
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
                int numBytesRead = this.port.read(buffer, 1000);
                Log.v("ReactNative", "blah");
                promise.resolve(new String(buffer, "UTF-8"));
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
}
