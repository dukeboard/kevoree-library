package org.kevoree.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import eu.powet.android.rfcomm.IRfcomm;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 23/05/12
 * Time: 11:40
 */
public class MessageManager implements QueueMessageThreadListener {

    // Debugging
    private static final String TAG = "MessageManager";
    private static final boolean D = true;

    private IRfcomm rfComm;
    private HashMap<String, ArrayDeque<byte[]>> recipients;

    public MessageManager(IRfcomm rfcomm) {
        this.rfComm = rfcomm;
        recipients = new HashMap<String, ArrayDeque<byte[]>>();
    }

    /**
     * Will send the message to the specified destNode when possible
     * @param destNode msg recipient
     * @param msg serialized message
     */
    public synchronized void sendMessage(String destNode, byte[] msg) {
        if (D) Log.i(TAG, "BEGIN sendMessage");
        if (recipients.containsKey(destNode)) {
            // we already have a message queue for this node
            recipients.get(destNode).add(msg);
            if (D) Log.i(TAG, "mess added in queue for "+destNode);
            String queueStr = "";
            for (byte[] buffer : recipients.get(destNode)) {
                queueStr += new String(buffer);
            }
            Log.i(TAG, "queue="+queueStr);

        } else {
            // we have to create a new entry for this node
            ArrayDeque<byte[]> queue = new ArrayDeque<byte[]>();
            queue.add(msg);
            recipients.put(destNode, queue);
            if (D) Log.i(TAG, "new mess queue added for "+destNode);
            SendQueuedMessageThread t = new SendQueuedMessageThread(rfComm, destNode, queue);
            t.addListener(this);
            t.start();
        }
    }

    public boolean hasMessageToDeliver() {
        return !recipients.isEmpty();
    }

    public boolean hasMessageToDeliver(BluetoothDevice device) {
        return recipients.containsKey(device.getName());
    }

    @Override
    public void onThreadEnding(String destNode) {
        recipients.remove(destNode);
    }
}
