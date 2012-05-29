package org.kevoree.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import eu.powet.android.rfcomm.IRfcomm;
import eu.powet.android.rfcomm.Rfcomm;
import eu.powet.android.rfcomm.listener.BluetoothEvent;
import eu.powet.android.rfcomm.listener.BluetoothEventListener;
import org.kevoree.android.framework.helper.UIServiceHandler;
import org.kevoree.android.framework.service.KevoreeAndroidService;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractChannelFragment;
import org.kevoree.framework.ChannelFragmentSender;
import org.kevoree.framework.KevoreeChannelFragment;
import org.kevoree.framework.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;


/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 23/03/12
 * Time: 09:03
 */

@Library(name = "Android")
@ChannelTypeFragment
@DictionaryType({
		@DictionaryAttribute(name = "secureUUID", defaultValue = BluetoothChannel.DEFAULT_SECURE_UUID, optional = false, fragmentDependant = false),
		@DictionaryAttribute(name = "unsecureUUID", defaultValue = BluetoothChannel.DEFAULT_UNSECURE_UUID, optional = false, fragmentDependant = false),
		@DictionaryAttribute(name = "replay", defaultValue = "true", optional = false, vals = {"true", "false"}),
		@DictionaryAttribute(name = "timeoutAccept", defaultValue = "300000", optional = false, fragmentDependant = false),
		@DictionaryAttribute(name = "timeoutConnect", defaultValue = "300000", optional = false, fragmentDependant = false),
		@DictionaryAttribute(name = "discoverableTime", defaultValue = "300", optional = false, fragmentDependant = false)
}
)
public class BluetoothChannel extends AbstractChannelFragment implements BluetoothEventListener {

	// Debugging
	private static final String TAG = "BluetoothChannel";
	private static final boolean D = true;

	// Default constants for dictionary values
	static final String DEFAULT_SECURE_UUID = "eae75780-a40f-11e1-b3dd-0800200c9a66";
	static final String DEFAULT_UNSECURE_UUID = "8a4bc930-9f53-11e1-a8b0-0800200c9a66";
	private static final long DEFAULT_TIMEOUT_ACCEPT = 300000;
	private static final long DEFAULT_TIMEOUT_CONNECT = 300000;
	private static final int DEFAULT_DISCOVERABLE_TIME = 300;

	private Logger logger = LoggerFactory.getLogger(BluetoothChannel.class);
	private KevoreeAndroidService uiservice;
	private IRfcomm rfComm;
	private String connectedDeviceName;
	private MessageManager manager;
	private long timeoutAccept, timeoutConnect;
	private int discoverableTime;
	private boolean replay;
	private ChannelClassResolver resolver = new ChannelClassResolver(this);

	@Start
	public void startChannel () {
		uiservice = UIServiceHandler.getUIService();

		try {
			replay = (getDictionary().get("replay").equals("true"));
		} catch (Exception e) {
			replay = true;
			Log.e(TAG, "Error for dictionary value \"replay\", using default value \"" + replay + "\"");
		}

		final UUID secureUUID, unsecureUUID;
		UUID secureUUIDtmp, unsecureUUIDtmp;

		try {
			secureUUIDtmp = UUID.fromString(getDictionary().get("secureUUID").toString());
			unsecureUUIDtmp = UUID.fromString(getDictionary().get("unsecureUUID").toString());

		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Given UUID are not well formed, using default values..", e);
			secureUUIDtmp = UUID.fromString(DEFAULT_SECURE_UUID);
			unsecureUUIDtmp = UUID.fromString(DEFAULT_UNSECURE_UUID);
		}
		secureUUID = secureUUIDtmp;
		unsecureUUID = secureUUIDtmp;

		try {
			timeoutAccept = Long.parseLong(getDictionary().get("timeoutAccept").toString());
		} catch (Exception e) {
			Log.e(TAG, "Error in dictionary value \"timeoutAccept\", using default value " + DEFAULT_TIMEOUT_ACCEPT);
			timeoutAccept = DEFAULT_TIMEOUT_ACCEPT;
		}

		try {
			timeoutConnect = Long.parseLong(getDictionary().get("timeoutConnect").toString());
		} catch (Exception e) {
			Log.e(TAG, "Error in dictionary value \"timeoutConnect\", using default value " + DEFAULT_TIMEOUT_CONNECT);
			timeoutConnect = DEFAULT_TIMEOUT_CONNECT;
		}

		try {
			discoverableTime = Integer.parseInt(getDictionary().get("discoverableTime").toString());
		} catch (Exception e) {
			Log.e(TAG, "Error in dictionary value \"discoverableTime\", using default value " + DEFAULT_DISCOVERABLE_TIME);
			discoverableTime = DEFAULT_DISCOVERABLE_TIME;
		}

		uiservice.getRootActivity().runOnUiThread(new Runnable() {
			@Override
			public void run () {
				//final Handler mHandler =

				rfComm = new Rfcomm(uiservice.getRootActivity(), secureUUID, unsecureUUID, new Handler() {
					@Override
					public void handleMessage (android.os.Message msg) {
						switch (msg.what) {
							case Rfcomm.MESSAGE_STATE_CHANGE:
								switch (msg.arg1) {
									case Rfcomm.STATE_CONNECTED:
										if (D) {
											Toast.makeText(uiservice.getRootActivity(), "STATE_CONNECTED",
													Toast.LENGTH_SHORT).show();
										}
										break;
									case Rfcomm.STATE_CONNECTING:
										if (D) {
											Toast.makeText(uiservice.getRootActivity(), "STATE_CONNECTING",
													Toast.LENGTH_SHORT).show();
										}
										break;
									case Rfcomm.STATE_LISTEN:
										if (D) {
											Toast.makeText(uiservice.getRootActivity(), "STATE_LISTEN",
													Toast.LENGTH_SHORT).show();
										}
										break;
									case Rfcomm.STATE_NONE:
										if (D) {
											Toast.makeText(uiservice.getRootActivity(), "STATE_NONE",
													Toast.LENGTH_SHORT).show();
										}
										break;
								}
								break;

							case Rfcomm.MESSAGE_READ:
								byte[] readBuf = (byte[]) msg.obj;
								// construct a string from the valid bytes in the buffer
								try {
									ByteArrayInputStream bis = new ByteArrayInputStream(readBuf);
									Log.i(TAG, "DRE DRE DRE DRE ?");
									ObjectInputStream ois = new ObjectInputStream(bis) {
										@Override
										protected Class<?> resolveClass (ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
											Class c = null;
											try {
												if (c == null) {
													c = resolver.resolve(objectStreamClass.getName());
												}
											} catch (Exception e) {
											}
											try {
												if (c == null) {
													c = super.resolveClass(objectStreamClass);
												}
											} catch (Exception e) {
											}
											try {
												if (c == null) {
													c = Class.forName(objectStreamClass.getName());
												}
											} catch (Exception e) {
											}

											return c;
										}
									};

									Message mess = (Message) ois.readObject();
									bis.close();
									ois.close();
									if (D) Log.i(TAG, "MESSAGE_READ: " + mess.getContent().toString());
									remoteDispatch(mess);
								} catch (IOException e) {
									Log.e(TAG, "Failed to deserialize the message");

								} catch (ClassNotFoundException e) {
									Log.e(TAG, "Unable to cast the deserialized object");
								}
								break;

							case Rfcomm.MESSAGE_DEVICE_NAME:
								// save the connected device's name
								connectedDeviceName = msg.getData().getString(Rfcomm.DEVICE_NAME);
								if (D) {
									Toast.makeText(uiservice.getRootActivity(), "Connected to "
											+ connectedDeviceName, Toast.LENGTH_SHORT).show();
								}
								break;

							case Rfcomm.MESSAGE_TOAST:
								if (D) {
									Toast.makeText(uiservice.getRootActivity(), msg.getData().getString(Rfcomm.TOAST),
											Toast.LENGTH_SHORT).show();
								}
								break;
						}
					}
				}, timeoutAccept);
				manager = new MessageManager(rfComm);

				rfComm.addEventListener(BluetoothChannel.this);
				rfComm.setName(getNodeName());
				if (discoverableTime > 0) rfComm.setDiscoverable(discoverableTime);
				rfComm.start();
				rfComm.discovering();
				if (D) Log.i(TAG, "Bluetooth channel started");
			}
		});
	}

	@Stop
	public void stopChannel () {
		rfComm.stop();
		if (D) Log.i(TAG, "Server channel closed");
	}

	@Update
	public void updateChannel () {
		if (D) Log.i(TAG, "Server channel updated !!!!!!!!!!!!!!!!!!!!!!!!!!");
		stopChannel();
		startChannel();
	}

	@Override
	public Object dispatch (Message msg) {
		for (org.kevoree.framework.KevoreePort p : getBindedPorts()) {
			// do not send back our own message
			// TODO change "default" stuff :/ make it bug-proof
			if (!msg.getDestNodeName().equals("default")) {
				forward(p, msg);
			}
		}
		for (KevoreeChannelFragment cf : getOtherFragments()) {
			if (msg.getPassedNodes().isEmpty()) {
				forward(cf, msg);
			}
		}
		return msg;
	}

	@Override
	public ChannelFragmentSender createSender (final String remoteNodeName, String remoteChannelName) {
		return new ChannelFragmentSender() {
			@Override
			public Object sendMessageToRemote (Message message) {
				try {
					// save the actual node in the message
					message.getPassedNodes().add(getNodeName());

					// serialize the message into a byte array
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutput out = new ObjectOutputStream(bos);
					out.writeObject(message);
					byte[] byteMess = bos.toByteArray();
					out.close();
					bos.close();

					// check if there's already a connection with the remote node
					if (rfComm.isDeviceConnected(remoteNodeName)) {
						if (D) Log.i(TAG, "[Connected] Sending message to " + remoteNodeName);
						rfComm.writeFromName(remoteNodeName, byteMess);

					} else {
						// we are not connected to the remote device
						// trying to find the node in the discovered devices
						BluetoothDevice remoteDevice = rfComm.getDeviceByName(remoteNodeName);
						if (remoteDevice != null) {
							// destination node has already been discovered by bluetooth
							// trying to connect to this remote device
							if (D) Log.i(TAG, "[Need connection] no connection with " + remoteDevice + ", trying to connect now...");
							rfComm.connect(remoteDevice, true, timeoutConnect);

							// if replay is set to true, add the message to the MessageManager
							if (replay) {
								manager.sendMessage(remoteNodeName, byteMess);
								if (D) Log.i(TAG, "Message from " + getNodeName() + " to " + remoteNodeName + " added in queue manager");
							}

						} else {
							// remote node is unreachable...not discovered or I don't know :/
							if (D) Log.i(TAG, "[OutOfRange] Remote device " + remoteNodeName + " is unreachable on discovery...maybe too far ?");
							// starting a new discovery process, just in case
							rfComm.discovering();
						}
					}

				} catch (IOException e) {
					Log.e(TAG, "Message serialization failed");
				}
				return message;
			}
		};

	}

	@Override
	public void discoveryFinished (BluetoothEvent bluetoothEvent) {
		if (rfComm != null && manager.hasMessageToDeliver()) {
			rfComm.discovering();
		}
	}

	@Override
	public void disconnected (BluetoothDevice bluetoothDevice) {
		// check if replay is set to true and if we still have message
		// to deliver to the disconnected device
		if (replay && manager.hasMessageToDeliver(bluetoothDevice)) {
			rfComm.connect(bluetoothDevice, true, timeoutConnect);
		}
	}

	@Override
	public void newDeviceFound (BluetoothDevice bluetoothDevice) {
		if (replay && manager.hasMessageToDeliver(bluetoothDevice)) {
			rfComm.connect(bluetoothDevice, true, timeoutConnect);
		}
	}

	@Override
	public void discoverable () {
	}

	@Override
	public void connected (BluetoothDevice bluetoothDevice) {
		Log.i(TAG, "Bluetooth: connected device " + bluetoothDevice);
	}
}
