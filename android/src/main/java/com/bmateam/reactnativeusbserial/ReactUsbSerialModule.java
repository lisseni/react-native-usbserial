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
import java.io.OutputStream;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import static android.content.ContentValues.TAG;
import java.nio.ByteBuffer;

public class ReactUsbSerialModule extends ReactContextBaseJavaModule {

  private final HashMap<String, UsbSerialDevice> usbSerialDriverDict = new HashMap<>();
  private final ExecutorService mExecutor1 = Executors.newSingleThreadExecutor();
  private final ExecutorService mExecutor2 = Executors.newSingleThreadExecutor();
  private static final String UsbEventName="Data";
  private static final String UsbDisconnectName="Disconnect";
  private ReactApplicationContext reactContext;
  private SerialInputOutputManager mSerialIoManager;
  private SerialInputOutputManager mSerialIoManager2;
  private final HashMap<String, UsbSerialPort>  mSerialPort = new HashMap<>();
  private boolean ConnectionState = false;
  private WritableArray openedDeviceArray = Arguments.createArray();
  private HashMap<String, String> monitoringDevicesDict = new HashMap<>();

  private final HashMap<String, SerialInputOutputManager.Listener> mListenerDict = new HashMap<>();
  private final SerialInputOutputManager.Listener mListener =
  new SerialInputOutputManager.Listener() {
    @Override
    public void onRunError(Exception e) {
      Log.v("BATRobot java", "Runner stopped.");
      // String param = "disconnect";
      // reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbDisconnectName, param);
    }
    @Override
    public void onNewData(final byte[] data) {
      //Log.v("BATRobot java", "onNewData");
      sendEvent(data, monitoringDevicesDict.get("1"));
    }
  };

  private final SerialInputOutputManager.Listener mListener2 =
  new SerialInputOutputManager.Listener() {
    @Override
    public void onRunError(Exception e) {
      Log.v("BATRobot java", "Runner stopped.");
      // String param = "disconnect";
      // reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbDisconnectName, param);
    }
    @Override
    public void onNewData(final byte[] data) {
      //Log.v("BATRobot java", "onNewData");
      sendEvent2(data, monitoringDevicesDict.get("2"));
    }
  };


  public ReactUsbSerialModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  private void stopIoManager(String portName) {
    if (!usbSerialDriverDict.isEmpty()) {
      Log.i("BATRobot java", "Stopping io manager .." + portName);
      if (portName.equals(monitoringDevicesDict.get("1"))){
        monitoringDevicesDict.remove("1");
        mSerialIoManager.stop();
        mSerialIoManager = null;

        Log.i("BATRobot java", "Stopping io manager .." + portName);
      }else if (portName.equals(monitoringDevicesDict.get("2"))){
        monitoringDevicesDict.remove("2");
        mSerialIoManager2.stop();
        mSerialIoManager2 = null;
        Log.i("BATRobot java", "Stopping io manager .." + portName);
      }
      // if (usbSerialDriverDict.isEmpty()){
      //   mSerialIoManager.stop();
      //   mSerialIoManager = null;
      //   Log.i("BATRobot java", "Stopping io manager mSerialIoManager" + portName);
      // }
    }
  }

  private void onDeviceStateChange(String portName) {
    //  Log.w("BATRobot java","onDeviceStateChange portName ="+portName);

    stopIoManager(portName);
    startIoManager(portName);
  }

  private void startIoManager(String portName) {
    //      Log.w("BATRobot java","startIoManager portName ="+portName);
    try{
      if (usbSerialDriverDict.isEmpty()){
        Log.w("BATRobot java","startIoManager No device opened");
        throw new Exception(String.format("No device opened"));
      }

      UsbSerialDevice usd = usbSerialDriverDict.get(portName);
      //      Log.w("BATRobot java","usd");
      if (usd == null) {
        Log.w("BATRobot java","startIoManager No device opened");
        throw new Exception(String.format("No device opened"));
      }

      if (!monitoringDevicesDict.isEmpty()){
        if (portName.equals(monitoringDevicesDict.get("1"))){
          Log.w("BATRobot java","event is monitored for port " + portName);
          return;
        }else if (portName.equals(monitoringDevicesDict.get("2"))){
          Log.w("BATRobot java","event is monitored for port " + portName);
          return;
        }
      }
      //monitoringDevicesDict.put(portName,"one");
      UsbSerialPort sPort = usd.getPort();
      Log.w("BATRobot java","startIoManager sPort" + portName);
      if (sPort != null) {
          // if (mSerialIoManager == null){
          //   mSerialIoManager = new SerialInputOutputManager(mListener);
          // }
            if (monitoringDevicesDict.get("1") == null){
              mSerialIoManager = new SerialInputOutputManager(sPort,mListener);
              monitoringDevicesDict.put("1",portName);
              Log.w("BATRobot java","startIoManager port1" + portName);
              mExecutor1.submit(mSerialIoManager);
            }else if (monitoringDevicesDict.get("2") == null){
              mSerialIoManager2 = new SerialInputOutputManager(sPort, mListener2);
              monitoringDevicesDict.put("2",portName);
              Log.w("BATRobot java","startIoManager2 port" + portName);
              mExecutor2.submit(mSerialIoManager2);
            }

      }else{
        //        Log.i("BATRobot java", "Start io manager error sPort == null");
      }
    }catch (Exception e) {
      Log.w("BATRobot java","Starting io manager Exception");
      e.printStackTrace();
    }

  }

  @Override
  public String getName() {
    return "UsbSerial";
  }

  @ReactMethod
  public void testUnbind(Promise p) {
    try {
      // String cmd2 = "su -c chmod 677 /sys";
      // runCommand(cmd2);
      // String cmd = "echo -n usb1 > /sys/bus/usb/drivers/usb/unbind";
      // runCommand(cmd);
      String cmd1 = "su -c chmod 677 /sys";
      runCommand(cmd1);
      String cmd3 = "su -c chmod 677 /sys/bus/usb/drivers/usb/unbind";
      runCommand(cmd3);
      String cmd2 = "echo -n usb1 > /sys/bus/usb/drivers/usb/unbind";
      runCommand(cmd2);
      //Process process = Runtime.getRuntime().exec("/system/xbin/su -c \"reboot\"");
      //Process process = Runtime.getRuntime().exec("/system/xbin/su -c \"echo -n \"usb1\" > /sys/bus/usb/drivers/usb/unbind\"");
    } catch (Exception e) {
      p.reject(e);
    }
  }
  
  @ReactMethod
  public void testReboot(Promise p) {
    try {     
      Process process = Runtime.getRuntime().exec("/system/xbin/su -c \"reboot\"");
      //Process process = Runtime.getRuntime().exec("/system/xbin/su -c \"echo -n \"usb1\" > /sys/bus/usb/drivers/usb/unbind\"");
    } catch (Exception e) {
      p.reject(e);
    }
  }
  
  @ReactMethod
  public void testBind(Promise p) {
    try {
      String cmd2 = "su -c chmod 677 /sys";
      runCommand(cmd2);
      String cmd1 = "su -c chmod 677 /sys/bus/usb/drivers/usb/bind";
      runCommand(cmd1);
      String cmd3 = "echo -n usb1 > /sys/bus/usb/drivers/usb/bind";
      runCommand(cmd3);
      //Process process = Runtime.getRuntime().exec("/system/xbin/su -c \"echo -n \"usb1\" > /sys/bus/usb/drivers/usb/bind\"");
    } catch (Exception e) {
      p.reject(e);
    }
  }

  /**
  * Метод выполняет скрипты shell в отдельном потоке.
  *
  * @param command shell скрипт.
  */
  private void runCommand(final String command) {
    //        Log.i("BATRobot", "BATRobot shell command: "+ command);
    // Чтобы не вис интерфейс, запускаем в другом потоке
    OutputStream out = null;
    InputStream in = null;
    try {
      // Отправляем скрипт в рантайм процесс
      String[] tempcmd = { "system/bin/sh", "-c", command };
      Process child = Runtime.getRuntime().exec(tempcmd);
      // Выходной и входной потоки
      out = child.getOutputStream();
      in = child.getInputStream();

      //Входной поток может что-нибудь вернуть
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
      String line;
      String result = "";
      while ((line = bufferedReader.readLine()) != null){
        result += line;
        Log.i("BATRobot", "BATRobot shell result: "+ line + "\n");
      }
      //Обработка того, что он вернул
      //handleBashCommandsResult(result);

    } catch (IOException e) {
      Log.i("BATRobot", "BATRobot shell result: "+ e.getMessage() + "\n");
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          Log.i("BATRobot", "BATRobot shell result: "+ e.getMessage() + "\n");
        }
      }
      if (out != null) {
        try {
          out.flush();
          out.close();
        } catch (IOException e) {
          Log.i("BATRobot", "BATRobot shell result: "+ e.getMessage() + "\n");
        }
      }
    }
  }

  @ReactMethod
  public void getDeviceListAsync(Promise p) {
    //    Log.v("BATRobot java", "getDeviceListAsync");
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
    //    Log.w("BATRobot java ","openDeviceAsync start");
    try {
      String portName = deviceObject.getString("comName");
      UsbManager manager = getUsbManager();
      UsbSerialDriver driver = getUsbSerialDriver(portName, manager);
      WritableMap map = Arguments.createMap();
      //if (manager.hasPermission(driver.getDevice())) {
      UsbSerialDevice usd = createUsbSerialDevice(manager, driver, portName);
      usbSerialDriverDict.put(portName, usd);
      Log.w("BATRobot java","port opened");
      onDeviceStateChange(portName);
      // usd.stopIoManager();
      // usd.startIoManager();
      p.resolve(usd);
    } catch (Exception e) {
      p.reject(e);
    }
  }

  @ReactMethod
  public void closeDevice(ReadableMap deviceObject, Promise p) {
    try {
      String portName = deviceObject.getString("comName");
      if (usbSerialDriverDict.isEmpty()){
        p.reject("Port wasn't opened");
      }
      UsbSerialDevice usd = usbSerialDriverDict.get(portName);
      if (usd != null){
        stopIoManager(portName);
        usd.getPort().close();
        usbSerialDriverDict.remove(portName);
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
    //   Log.w("BATRobot java getUsbPermission","start");
    try {
      String portName = deviceObject.getString("comName");
      UsbManager manager = getUsbManager();
      UsbSerialDriver driver = getUsbSerialDriver(portName, manager);
      UsbDevice device = driver.getDevice();

      if (manager.hasPermission(device)){
        p.resolve("hasPermission");
      }
      else{
        Log.w("BATRobot java getUsbPermission","device has no permission");
        requestUsbPermission(manager, device, p);
      }
    } catch (Exception e) {
      Log.w("BATRobot java getUsbPermission","Exception");
      p.reject(e);
    }
  }

  private void requestUsbPermission(UsbManager manager,
  UsbDevice device,
  Promise p) {
    //    Log.w("BATRobot java requestUsbPermission","start");
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
  public void flush(ReadableMap deviceObject, Boolean flushReadBuffers, Boolean flushWriteBuffers) {
    try {
      String portName = deviceObject.getString("comName");
        if (usbSerialDriverDict.isEmpty()){
          return;
        }
        UsbSerialDevice usd = usbSerialDriverDict.get(portName);
        if (usd != null){
          UsbSerialPort sPort = usd.getPort();
          if (sPort != null)
            sPort.purgeHwBuffers(flushReadBuffers, flushWriteBuffers);
        }
    }catch (Exception e) {
      Log.w("BATRobot java flush","Exception");
      return;
    }
  }

  @ReactMethod
  public void readDeviceAsync(ReadableMap deviceObject, Promise p) {

    int offset = 0;
    String portName = deviceObject.getString("comName");
    Log.w("BATRobot java readDeviceAsync","start "+portName);
    try {
      if (usbSerialDriverDict.isEmpty()){
        p.reject("Port is closed");
      }

      UsbSerialDevice usd = usbSerialDriverDict.get(portName);
      if (usd != null){

        UsbSerialPort sPort = usd.getPort();
        if (sPort == null){
          p.reject("Port is closed");
        }else{
          ByteBuffer mReadBuffer = ByteBuffer.allocate(4096);
          Log.w("BATRobot java readDeviceAsync","before read");
          int len = sPort.read(mReadBuffer.array(), 100);
          Log.w("BATRobot java readDeviceAsync","after read len = "+len);
          if (len>0){
            final byte[] data = new byte[len];

            mReadBuffer.get(data, 0, len);
            Log.w("BATRobot java readDeviceAsync","return resolve");
            p.resolve(data);
            //mReadBuffer.clear();
        }else{
          p.resolve(null);
        }
          //sPort.purgeHwBuffers(true, true);
                  //Log.w("BATRobot java writeInDeviceAsync","offset "+offset + portName);

        }
      }else{
        Log.w("BATRobot Port is closed",portName);
        p.reject("Port is closed");
      }
    } catch (Exception e) {
      Log.w("BATRobot java writeInDeviceAsync","Exception");
      p.reject(e);
    }
  }

  @ReactMethod
  public void writeInDeviceAsync(ReadableMap deviceObject, ReadableArray cmd, Promise p) {

    int offset = 0;
    String portName = deviceObject.getString("comName");
    //Log.w("BATRobot java writeInDeviceAsync","start "+portName);
    try {
      if (usbSerialDriverDict.isEmpty()){
        p.reject("Port is closed");
      }

      UsbSerialDevice usd = usbSerialDriverDict.get(portName);
      if (usd != null){

        byte[] data = new byte[cmd.size()];
        for (int i =0; i< cmd.size(); i++) {
          data[i] = (byte)cmd.getInt(i);
        }
        UsbSerialPort sPort = usd.getPort();
        if (sPort == null){
          p.reject("Port is closed");
        }else{
          //sPort.purgeHwBuffers(true, true);
          offset = sPort.write(data, 400);
          //Log.w("BATRobot java writeInDeviceAsync","offset "+offset + portName);
          p.resolve(0);
        }
      }else{
        Log.w("BATRobot Port is closed",portName);
        p.reject("Port is closed");
      }
    } catch (Exception e) {
      Log.w("BATRobot java writeInDeviceAsync","Exception");
      p.reject(e);
    }
  }

  private void sendEvent(byte[] data, String portName) {
    WritableArray dataArray = Arguments.createArray();
    String eventName = UsbEventName + " " + portName;
    for (int i =0; i< data.length; i++) {
      dataArray.pushInt((data[i])&0xFF);

    }
    //Log.i("BATRobot java", "sendEvent! " + eventName);
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, dataArray);
  }

  private void sendEvent2(byte[] data, String portName) {
    WritableArray dataArray = Arguments.createArray();
    String eventName = UsbEventName + " " + portName;
    for (int i =0; i< data.length; i++) {
      dataArray.pushInt((data[i])&0xFF);

    }
    //Log.i("BATRobot java", "sendEvent! " + eventName);
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, dataArray);
  }


  private UsbSerialDevice createUsbSerialDevice(UsbManager manager,
  UsbSerialDriver driver, String portName) throws IOException {

    UsbDeviceConnection connection = manager.openDevice(driver.getDevice());

    // Most have just one port (port 0).
    // int test_device_port = 0;
    // if (driver.getPorts().size() == 2){
    //   test_device_port = 1;
    // }else if (driver.getPorts().size() == 0)
    // {
    //   throw new IOException("no ports");
    // }
    // Log.i("BATRobot", "BATRobot ports num: "+ driver.getPorts().size() + " "+portName);
    // UsbSerialPort port = driver.getPorts().get(test_device_port);
    UsbSerialPort port = driver.getPorts().get(0);

    port.open(connection);
    port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

    //String id = generateId();
    UsbSerialDevice usd = new UsbSerialDevice(port, portName);
    WritableMap map = Arguments.createMap();
    WritableMap currentDev = Arguments.createMap();

    //map.putString("id", id);

    return usd;
  }



  private static final String ACTION_USB_PERMISSION  = "com.bmateam.reactnativeusbserial.USB_PERMISSION";

  private UsbManager getUsbManager() {
    ReactApplicationContext rAppContext = getReactApplicationContext();
    UsbManager usbManager = (UsbManager) rAppContext.getSystemService(rAppContext.USB_SERVICE);

    return usbManager;
  }

  private UsbSerialDriver getUsbSerialDriver(String portName, UsbManager manager) throws Exception {
    //    Log.i("BATRobot java", "portName: "+ portName);
    if (portName == null)
    throw new Error(new Error("The deviceObject is not a valid 'UsbDevice' reference"));

    List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

    // Reject if no driver is available
    if (availableDrivers.isEmpty())
    throw new Exception("No available drivers to communicate with devices");

    for (UsbSerialDriver drv : availableDrivers) {
      String deviceName = drv.getDevice().getDeviceName();
      //    Log.i("BATRobot", "deviceName: "+ deviceName);
      if (deviceName.equals(portName)){
        //      Log.i("BATRobot java getUsbSerialDriver", "return driver ok");
        return drv;
      }

    }

    // Reject if no driver exists for the current productId
    throw new Exception(String.format("No driver found for PortName '%s'", portName));
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
              p.resolve("permition ok");
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
