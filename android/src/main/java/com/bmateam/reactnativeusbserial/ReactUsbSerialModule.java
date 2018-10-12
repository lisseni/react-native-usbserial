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
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;

public class ReactUsbSerialModule extends ReactContextBaseJavaModule {

    private final HashMap<String, UsbSerialDevice> usbSerialDriverDict = new HashMap<>();

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private UsbSerialPort mSerialPort;

    private boolean ConnectionState = false;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    ReactUsbSerialModule.this.emitNewData(data);
                }
            };

    public ReactApplicationContext REACTCONTEXT;

    public ReactUsbSerialModule(ReactApplicationContext reactContext) {
        super(reactContext);
        REACTCONTEXT = reactContext;
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

                map.putString("name", device.getDeviceName());
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
                byte[] data = {(byte)0x80, (byte)0x27,(byte)0x05,(byte)0x52};
                mSerialPort.write(data, 400);
                p.resolve(usd);
            } else {
                requestUsbPermission(manager, driver.getDevice(), p);
            }

        } catch (Exception e) {
            p.reject(e);
        }
    }

    @ReactMethod
    public void writeInDeviceAsync(Promise p) {
        int offset;
        try {
            if (ConnectionState){
              byte[] data = {(byte)0x80, (byte)0x27,(byte)0x05,(byte)0x52};
              offset = mSerialPort.write(data, 400);

            }
            p.resolve(null);
        } catch (Exception e) {
            p.reject(e);
        }
    }

    @ReactMethod
    public void readDeviceAsync(String deviceId) {

        try {
            UsbSerialDevice usd = usbSerialDriverDict.get(deviceId);

            if (usd == null) {
                throw new Exception(String.format("No device opened for the id '%s'", deviceId));
            }

            mSerialIoManager = new SerialInputOutputManager(usd.port, mListener);
            mExecutor.submit(mSerialIoManager);
        } catch (Exception e) {
        }
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }



    public void emitNewData(byte[] data) {
        if (REACTCONTEXT != null) {
            WritableMap params = Arguments.createMap();
            params.putString("data", new String(data));
            sendEvent(REACTCONTEXT, "newData", params);
        }
    }

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
