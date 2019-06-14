/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.util;

import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class which services a {@link UsbSerialPort} in its {@link #run()}
 * method.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialInputOutputManager implements Runnable {

    private static final String TAG = SerialInputOutputManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int READ_WAIT_MILLIS = 200;
    private static final int BUFSIZ = 4096;

    private UsbSerialPort mDriver = null;
    private UsbSerialPort mDriver2 = null;
    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);
    private final ByteBuffer mReadBuffer2 = ByteBuffer.allocate(BUFSIZ);
    // Synchronized by 'mWriteBuffer'
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);
    private final ByteBuffer mWriteBuffer2 = ByteBuffer.allocate(BUFSIZ);
    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    // Synchronized by 'this'
    private State mState = State.STOPPED;

    // Synchronized by 'this'
    private Listener mListener;
    private Listener mListener2;

    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        public void onNewData(byte[] data);

        public void onNewData2(byte[] data);

        /**
         * Called when {@link SerialInputOutputManager#run()} aborts due to an
         * error.
         */
        public void onRunError(Exception e);
    }

    /**
     * Creates a new instance with the provided listener.
     */
    public SerialInputOutputManager(Listener listener) {
        mListener = listener;
    }

    public synchronized void setListener(Listener listener) {
        mListener = listener;
    }

    public synchronized Listener getListener() {
        return mListener;
    }

    public synchronized void setListener2(Listener listener) {
        mListener2 = listener;
    }

    public synchronized Listener getListener2() {
        return mListener2;
    }

    public synchronized void setDriver2(UsbSerialPort driver) {
        mDriver2 = driver;
    }

    public synchronized UsbSerialPort getDriver2() {
        return mDriver2;
      }
    public synchronized void setDriver(UsbSerialPort driver) {
            mDriver = driver;
        }

    public synchronized UsbSerialPort getDriver() {
            return mDriver;
          }
    public void writeAsync(byte[] data) {
        synchronized (mWriteBuffer) {
            mWriteBuffer.put(data);
        }
    }

    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            Log.i(TAG, "Stop requested");
            mState = State.STOPPING;
        }
    }

    private synchronized State getState() {
        return mState;
    }

    /**
     * Continuously services the read and write buffers until {@link #stop()} is
     * called, or until a driver exception is raised.
     *
     * NOTE(mikey): Uses inefficient read/write-with-timeout.
     */
    @Override
    public void run() {
        synchronized (this) {
            if (getState() != State.STOPPED) {
                throw new IllegalStateException("Already running.");
            }
            mState = State.RUNNING;
        }

        Log.i(TAG, "Running ..");
        try {
            while (true) {
                if (getState() != State.RUNNING) {
                    Log.i(TAG, "Stopping mState=" + getState());
                    break;
                }
                step();
            }
        } catch (Exception e) {
            Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
            final Listener listener = getListener();
            if (listener != null) {
              listener.onRunError(e);
            }
        } finally {
            synchronized (this) {
                mState = State.STOPPED;
                Log.i(TAG, "Stopped.");
            }
        }
    }

    private void step() throws IOException {
        // Handle incoming data.
        if (mDriver != null){
          int len = mDriver.read(mReadBuffer.array(), READ_WAIT_MILLIS);
          if (len > 0) {
  //            if (DEBUG) Log.d("BATRobot", "Read data len=" + len);
              final Listener listener = getListener();
              if (listener != null) {
                  final byte[] data = new byte[len];
                  mReadBuffer.get(data, 0, len);
                  listener.onNewData(data);
              }
              mReadBuffer.clear();
              //mDriver.purgeHwBuffers(true, false);
          }

          // Handle outgoing data.
          byte[] outBuff = null;
          synchronized (mWriteBuffer) {
              len = mWriteBuffer.position();
              if (len > 0) {
                  outBuff = new byte[len];
                  mWriteBuffer.rewind();
                  mWriteBuffer.get(outBuff, 0, len);
                  mWriteBuffer.clear();
              }
          }
          if (outBuff != null) {
              if (DEBUG) {
                  Log.d(TAG, "Writing data len=" + len);
              }
              mDriver.write(outBuff, READ_WAIT_MILLIS);
          }
        }

        if (mDriver2 != null){
          int len2 = mDriver2.read(mReadBuffer2.array(), READ_WAIT_MILLIS);
          if (len2 > 0) {
//              if (DEBUG) Log.d("BATRobot", "Read data 2 len=" + len2);
              final Listener listener = getListener();
              if (listener != null) {
                  final byte[] data = new byte[len2];
                  mReadBuffer2.get(data, 0, len2);
                  listener.onNewData2(data);
                  //mDriver2.purgeHwBuffers(true, false);
              }
              mReadBuffer2.clear();
          }

          // Handle outgoing data.
          byte[] outBuff2 = null;
          synchronized (mWriteBuffer2) {
              len2 = mWriteBuffer2.position();
              if (len2 > 0) {
                  outBuff2 = new byte[len2];
                  mWriteBuffer2.rewind();
                  mWriteBuffer2.get(outBuff2, 0, len2);
                  mWriteBuffer2.clear();
              }
          }
          if (outBuff2 != null) {
              if (DEBUG) {
                  Log.d(TAG, "Writing data len=" + len2);
              }
              mDriver2.write(outBuff2, READ_WAIT_MILLIS);
          }
        }
    }

}
