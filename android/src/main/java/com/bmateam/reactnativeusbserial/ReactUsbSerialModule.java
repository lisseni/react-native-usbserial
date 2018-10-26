package com.bmateam.reactnativeusbserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Debug;
import android.support.annotation.Nullable;
import android.util.Log;

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
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.UnsupportedEncodingException;

import static android.content.ContentValues.TAG;

public class ReactUsbSerialModule extends ReactContextBaseJavaModule {

  //private final String TAG = ReactUsbSerialModule.class.getSimpleName();
  private final HashMap<String, UsbSerialDevice> usbSerialDriverDict = new HashMap<>();

  private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
  //private final ProbeTable customTable = new ProbeTable();
  //private final UsbSerialProber customProber;
  private static final String UsbEventName="UsbSerialEvent";
  private ReactApplicationContext reactContext;
  private SerialInputOutputManager mSerialIoManager;

  private UsbSerialPort mSerialPort = null;

  private boolean ConnectionState = false;

  private final SerialInputOutputManager.Listener mListener =
  new SerialInputOutputManager.Listener() {
    @Override
    public void onRunError(Exception e) {
      Log.v("BATROBOT", "BATROBOT java Runner stopped.");
    }

    @Override
    public void onNewData(final byte[] data) {

        Log.v("BATROBOT", "BATROBOT java data recieved");
        sendEvent(String.valueOf(data));


  };
};
  //public ReactApplicationContext REACTCONTEXT;

  public ReactUsbSerialModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  private void stopIoManager() {
     if (mSerialIoManager != null) {
         Log.i(TAG, "BATROBOT java Stopping io manager ..");
         mSerialIoManager.stop();
         mSerialIoManager = null;
     }
  }

 private void onDeviceStateChange() {
         stopIoManager();
         startIoManager();
     }

  //@ReactMethod
  private void startIoManager(String deviceId) {
    try{
      //UsbSerialDevice usd = usbSerialDriverDict.get(deviceId);

      if (mSerialPort == null) {
        throw new Exception(String.format("BATROBOT java No device opened for the id '%s'", deviceId));
      }

      //UsbSerialPort sPort = usd.getPort();
      if (mSerialPort != null) {
        Log.v("BATROBOT", " BATROBOT java Starting io manager ..");
        mSerialIoManager = new SerialInputOutputManager(mSerialPort, mListener);
        mExecutor.submit(mSerialIoManager);
      }

    }catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public String getName() {
    return "UsbSerial";
  }

  @ReactMethod
  public void getDeviceListAsync(Promise p) {

    try {
      UsbManager usbManager = getUsbManager();

      HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
      WritableArray deviceArray = Arguments.createArray();

      for (String key: usbDevices.keySet()) {
        UsbDevice device = usbDevices.get(key);
        WritableMap map = Arguments.createMap();

        map.putString("comName", device.getDeviceName());
        map.putInt("deviceId", device.getDeviceId());
        map.putInt("productId", device.getProductId());
        map.putInt("vendorId", device.getVendorId());

        deviceArray.pushMap(map);
      }
      Log.v("BATROBOT", " BATROBOT java getDeviceListAsync");
      p.resolve(deviceArray);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void list(Promise p) {

    try {
      UsbManager usbManager = getUsbManager();

      HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
      WritableArray deviceArray = Arguments.createArray();

      for (String key: usbDevices.keySet()) {
        UsbDevice device = usbDevices.get(key);
        WritableMap map = Arguments.createMap();

        map.putString("comName", device.getDeviceName());
        map.putInt("deviceId", device.getDeviceId());
        map.putInt("productId", device.getProductId());
        map.putInt("vendorId", device.getVendorId());

        deviceArray.pushMap(map);
      }

      p.resolve(deviceArray);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void test(Promise p){
    p.resolve("eeee");
  }

  @ReactMethod
  public void openDeviceAsync(ReadableMap deviceObject, Promise p) {

    try {
      int prodId = deviceObject.getInt("productId");
      UsbManager manager = getUsbManager();
      UsbSerialDriver driver = getUsbSerialDriver(prodId, manager);

      if (manager.hasPermission(driver.getDevice())) {
        WritableMap usd = createUsbSerialDevice(manager, driver);
        ConnectionState = true;
        Log.v("BATROBOT", "BATROBOT java opened");
        onDeviceStateChange();
        p.resolve(usd);
      } else {
        Log.v("BATROBOT", "BATROBOT need Permission java opened");
        requestUsbPermission(manager, driver.getDevice(), p);

        //p.reject("need Permission");
      }

    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void writeInDeviceAsync(ReadableArray cmd, Promise p) {
    int offset = 0;
    try {
      if (ConnectionState){
        // WritableArray cmd = Arguments.createArray();
        // cmd.pushInt(0xC0);
        // cmd.pushInt(0x46);
        // cmd.pushInt(0x0C);
        // cmd.pushInt(0x02);
        // cmd.pushInt(0x18);
        // cmd.pushInt(0x00);
        // cmd.pushInt(0xd2);
        //byte[] data = {(byte)0x80, (byte)0x27,(byte)0x05,(byte)0x52};
        byte[] data = new byte[cmd.size()];
        for (int i =0; i< cmd.size(); i++) {
          data[i] = (byte)cmd.getInt(i);
        }

        offset = mSerialPort.write(data, 400);
        sendEvent(String.valueOf(offset));
        //sendEvent(REACTCONTEXT, "test", offset);
        p.resolve(offset);
      }else{
        p.reject("Port is closed");
      }

    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void readDeviceAsync(String deviceId, Promise p) {

    try {
      //UsbSerialDevice usd = usbSerialDriverDict.get(deviceId);

      if (mSerialPort == null) {
        throw new Exception(String.format("No device opened for the id '%s'", deviceId));
      }
      readAsync(p);
      // mSerialIoManager = new SerialInputOutputManager(usd.port, mListener);
      // mExecutor.submit(mSerialIoManager);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  private void readAsync(Promise promise) {

    if (mSerialPort != null) {
      try {
        byte buffer[] = new byte[16];
        int numBytesRead = mSerialPort.read(buffer, 1000);
        Log.v("ReactNative", "blah");
        promise.resolve(buffer);
      } catch (IOException e) {
        promise.reject(e);
      }
    } else {
      promise.resolve("no Port");
    }
  }

  // private void sendEvent(ReactContext reactContext,
  //                        String eventName,
  //                        @Nullable WritableMap params) {
  //     reactContext
  //             .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
  //             .emit(eventName, params);
  // }
  private void sendEvent(String data) {
    WritableMap params = Arguments.createMap();
    params.putString("data", data);
    Log.v("BATROBOT","BATROBOT java emit event");
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbEventName, params);
  }
  // public void emitNewData(byte[] data) {
  //     if (REACTCONTEXT != null) {
  //         WritableMap params = Arguments.createMap();
  //         Formatter formatter = new Formatter();
  //         for (byte b : data) {
  //             formatter.format("%02x", b);
  //         }
  //         String hex = formatter.toString();
  //         params.putString("data", hex);
  //         sendEvent(REACTCONTEXT, "newData", params);
  //     }
  // }

  private WritableMap createUsbSerialDevice(UsbManager manager,
  UsbSerialDriver driver) throws IOException {

    UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

    // Most have just one port (port 0).
    mSerialPort = driver.getPorts().get(0);


    mSerialPort.open(connection);
    mSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

    String id = generateId();
    UsbSerialDevice usd = new UsbSerialDevice(mSerialPort);
    WritableMap map = Arguments.createMap();


    // Add UsbSerialDevice to the usbSerialDriverDict map
    usbSerialDriverDict.put(id, usd);

    map.putString("id", id);

    return map;
  }

  private void requestUsbPermission(UsbManager manager,
  UsbDevice device,
  Promise p) {

    try {
      ReactApplicationContext rAppContext = getReactApplicationContext();
      PendingIntent permIntent = PendingIntent.getBroadcast(rAppContext, 0, new Intent(ACTION_USB_PERMISSION), 0);

      registerBroadcastReceiver(p);

      manager.requestPermission(device, permIntent);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  private static final String ACTION_USB_PERMISSION  = "com.bmateam.reactnativeusbserial.USB_PERMISSION";

  private UsbManager getUsbManager() {
    ReactApplicationContext rAppContext = getReactApplicationContext();
    UsbManager usbManager = (UsbManager) rAppContext.getSystemService(rAppContext.USB_SERVICE);

    return usbManager;
  }

  private UsbSerialDriver getUsbSerialDriver(int prodId, UsbManager manager) throws Exception {

    if (prodId == 0)
    throw new Error(new Error("The deviceObject is not a valid 'UsbDevice' reference"));

    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

    // Reject if no driver is available
    if (availableDrivers.isEmpty())
    throw new Exception("No available drivers to communicate with devices");

    for (UsbSerialDriver drv : availableDrivers) {

      if (drv.getDevice().getProductId() == prodId)
      return drv;
    }

    // Reject if no driver exists for the current productId
    throw new Exception(String.format("No driver found for productId '%s'", prodId));
  }

  private void registerBroadcastReceiver(final Promise p) {
    IntentFilter intFilter = new IntentFilter(ACTION_USB_PERMISSION);
    final BroadcastReceiver receiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {

        if (ACTION_USB_PERMISSION.equals(intent.getAction())) {

          synchronized (this) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
              UsbManager manager = getUsbManager();

              try {
                WritableMap usd = createUsbSerialDevice(manager,
                getUsbSerialDriver(device.getProductId(), manager));

                p.resolve(usd);
              } catch (Exception e) {
                p.reject(e);
              }

            } else {
              p.resolve(new Exception(String.format("Permission denied by user for device %s", device
              .getDeviceName())));
            }
          }
        }

        unregisterReceiver(this);
      }
    };

    getReactApplicationContext().registerReceiver(receiver, intFilter);

  }

  private void unregisterReceiver(BroadcastReceiver receiver) {
    getReactApplicationContext().unregisterReceiver(receiver);
  }

  private String generateId() {
    return UUID.randomUUID().toString();
  }
}
