package org.kevoree.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import eu.powet.android.rfcomm.IRfcomm;

import java.util.ArrayDeque;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 23/05/12
 * Time: 16:32
 * To change this template use File | Settings | File Templates.
 */
public class SendQueuedMessageThread extends Thread {

    // Debugging
    private static final String TAG = "SendQueueMessageThread";
    private static final boolean D = true;

    private IRfcomm rfcomm;
    private String deviceName;
    private ArrayDeque<byte[]> queue;
    private QueueMessageThreadListener listener;
    private boolean test = true;

    public SendQueuedMessageThread(IRfcomm rfcomm, String deviceName, ArrayDeque<byte[]> queue) {
        this.rfcomm = rfcomm;
        this.deviceName = deviceName;
        this.queue = queue;
    }

    @Override
    public void run() {
        if (D) Log.i(TAG, "BEGIN SendQueueMessageThread for " + deviceName);
        while (!queue.isEmpty()) {
            if (rfcomm.isDeviceConnected(deviceName)) {
                if (test) {
                    try {
                        if (D) Log.i(TAG, "Gonna sleep for a second to ensure connection is really ok");
                        Thread.sleep(1000);
                        test = false;
                    } catch (InterruptedException e) {}
                }
                rfcomm.writeFromName(deviceName, queue.pollFirst());
                if (D) Log.i(TAG, "mess written for " + deviceName);
            }
        }
        if (listener != null) listener.onThreadEnding(deviceName);
        if (D) Log.i(TAG, "END SendQueueMessageThread for " + deviceName);
    }

    public void addListener(QueueMessageThreadListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        this.listener = null;
    }
}
