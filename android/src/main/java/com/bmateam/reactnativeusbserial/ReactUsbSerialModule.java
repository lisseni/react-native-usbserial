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

  private final HashMap<String, UsbSerialDevice> usbSerialDriverDict = new HashMap<>();
  private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
  private static final String UsbEventName="Data";
  private ReactApplicationContext reactContext;
  private SerialInputOutputManager mSerialIoManager;
  private UsbSerialPort mSerialPort = null;
  private boolean ConnectionState = false;

  private final SerialInputOutputManager.Listener mListener =
  new SerialInputOutputManager.Listener() {
    @Override
    public void onRunError(Exception e) {
      Log.v("USBSerialModule", "Runner stopped.");
    }
    @Override
    public void onNewData(final byte[] data) {
      sendEvent(data);
    }
  };

  public ReactUsbSerialModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  private void stopIoManager() {
    if (mSerialIoManager != null) {
      Log.i("USBSerialModule", "Stopping io manager ..");
      mSerialIoManager.stop();
      mSerialIoManager = null;
    }
  }

  private void onDeviceStateChange() {
    stopIoManager();
    startIoManager();
  }

  private void startIoManager() {
    try{
      if (mSerialPort == null) {
        throw new Exception(String.format("No device opened"));
      }

      if (mSerialPort != null) {
        Log.v("USBSerialModule", "Starting io manager ..");
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
  public void test(Promise p) {
    try {
      Process process = Runtime.getRuntime().exec("/system/xbin/su -c \"echo -n \"usb1\" > /sys/bus/usb/drivers/usb/unbind\"");
    } catch (Exception e) {
      p.reject(e);
    }
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
      p.resolve(deviceArray);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void openDeviceAsync(ReadableMap deviceObject, Promise p) {

    try {
      int prodId = deviceObject.getInt("productId");
      UsbManager manager = getUsbManager();
      UsbSerialDriver driver = getUsbSerialDriver(prodId, manager);

      //if (manager.hasPermission(driver.getDevice())) {
        WritableMap usd = createUsbSerialDevice(manager, driver);
        ConnectionState = true;
        onDeviceStateChange();
        p.resolve(usd);
      // } else {
      //   requestUsbPermission(manager, driver.getDevice(), p);
      // }
    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void closeDevice(ReadableMap deviceObject, Promise p) {

    try {
      if (ConnectionState){
        stopIoManager();
        mSerialPort.close();
        mSerialPort = null;
        ConnectionState = false;
        //usbSerialDriverDict
        p.resolve("Port closed");
      }else{
        p.reject("Port wasn't opened");
      }
    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void getUsbPermission(ReadableMap deviceObject, Promise p) {

    try {
      int prodId = deviceObject.getInt("productId");
      UsbManager manager = getUsbManager();
      UsbSerialDriver driver = getUsbSerialDriver(prodId, manager);

      UsbDevice device = driver.getDevice();
      if (manager.hasPermission(device)){
        p.resolve("hasPermission");
      }
      else{
        Log.w("USBSerialModule","device has no permission");
        requestUsbPermission(manager, device, p);
      }
    } catch (Exception e) {
      p.reject(e);
    }
  }

  private void requestUsbPermission(UsbManager manager,
  UsbDevice device,
  Promise p) {

    try {
      ReactApplicationContext rAppContext = getReactApplicationContext();
      PendingIntent permIntent = PendingIntent.getBroadcast(rAppContext, 0, new Intent(ACTION_USB_PERMISSION), 0);

      registerBroadcastReceiver(p);
      if (manager.hasPermission(device)){
        p.resolve("hasPermission");
      }else{
        manager.requestPermission(device, permIntent);
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
        byte[] data = new byte[cmd.size()];
        for (int i =0; i< cmd.size(); i++) {
          data[i] = (byte)cmd.getInt(i);
        }
        offset = mSerialPort.write(data, 400);

        p.resolve(offset);
      }else{
        p.reject("Port is closed");
      }

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

  private void sendEvent(byte[] data) {
    WritableArray dataArray = Arguments.createArray();
     for (int i =0; i< data.length; i++) {
       dataArray.pushInt((data[i])&0xFF);

     }
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbEventName, dataArray);
  }


  private WritableMap createUsbSerialDevice(UsbManager manager,
  UsbSerialDriver driver) throws IOException {

    UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

    // Most have just one port (port 0).
    int test_device_port = 0;
    if (driver.getPorts().size() == 2){
      test_device_port = 1;
    }else if (driver.getPorts().size() == 0)
    {
      throw new IOException("no ports");
    }
    Log.i("BATRobot", "BATRobot ports num: "+ driver.getPorts().size());

    mSerialPort = driver.getPorts().get(test_device_port);

    //mSerialPort = driver.getPorts().get(0);


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
              p.resolve(new Exception(String.format("Permission denied by user for device '%s'", device
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
