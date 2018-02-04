package eu.liebrand.pipemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Created by mark on 07.03.17.
 */

public class ProviderImpl extends SAAgent {
    public static final String TAG = "SAP Service Agent";
    private static final String INTENT_STATUS = "eu.liebrand.pipemonitor.providerimpl";
    private static int channelId=104;
    private SA mAccessory;
    ProviderServiceConnection connHandler;
    String currentPeerId;
    //HashMap<Integer, ProviderConnection> mConnectionsMap = null;
    MyReceiver myReceiver;
    int rcvRefCounter;
    String ip1Head, ip2Head, ip1Tail, ip2Tail, pipe1Name, pipe2Name;
    int port1Head, port2Head, port1Tail, port2Tail;
    boolean isRegistered1, isRegistered2;

    public class LocalBinder extends Binder {
        public ProviderImpl getService() {
            return ProviderImpl.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rcvRefCounter=0;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        ip1Tail = settings.getString("tailinstanceIP1", "");
        port1Tail = Integer.parseInt(settings.getString("tailinstancePort1", "0"));
        ip2Tail = settings.getString("tailinstanceIP2", "");
        port2Tail = Integer.parseInt(settings.getString("tailinstancePort2", "0"));
        ip1Head = settings.getString("headHost1", "");
        port1Head = settings.getInt("headPort1", 0);
        ip2Head = settings.getString("headHost2", "");
        port2Head = settings.getInt("headPort2", 0);
        pipe1Name=settings.getString("pipe1Name", "");
        pipe2Name=settings.getString("pipe2Name", "");
        isRegistered1 = settings.getBoolean("isRegistered1", false);
        isRegistered2 = settings.getBoolean("isRegistered2", false);


        mAccessory = new SA();
        try {
            mAccessory.initialize(this);
            myReceiver = new MyReceiver();
            IntentFilter intentFilter = new IntentFilter();
            //intentFilter.addAction(INTENT_ACTION);
            registerReceiver(myReceiver, intentFilter);
        }
        catch(SsdkUnsupportedException e) {
            if (processUnsupportedException(e) == true) {
                return;
            }
        }
        catch (Exception e1) {
            Log.e(TAG, "Cannot initialize SAccessory package.");
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. You
             * application should work smoothly without using this SDK, or you
             * may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        }
    }


    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            Log.e(TAG, "This device does not support SAccessory.");
            /*
             * Your application can not use Samsung Accessory SDK. You
             * application should work smoothly without using this SDK, or you
             * may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install SAccessory package"
                    + " to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update SAccessory package"
                    + " to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG,
                    "We recommend that you update your"
                            + " Samsung Accessory software before using this application.");
            return false;
        }
        return true;
    }

    @Override
    public void onLowMemory() {
        Log.e(TAG, "onLowMemory  has been hit better to do  graceful  exit now");
        closeConnection();
        super.onLowMemory();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if(myReceiver!=null) {
            unregisterReceiver(myReceiver);
        }
        Log.i(TAG, "Service Stopped.");
    }

    public ProviderImpl() {
        super(TAG, ProviderServiceConnection.class);
    }

    public boolean closeConnection() {
        connHandler.close();
        connHandler=null;
        /* if (mConnectionsMap != null) {
            final List<Integer> listConnections = new ArrayList<Integer>(
                    mConnectionsMap.keySet());
            for (final Integer s : listConnections) {
                Log.i(TAG, "KEYS found are" + s);
                mConnectionsMap.get(s).close();
                mConnectionsMap.remove(s);
            }
        } */
        return true;
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent saPeerAgent, SASocket saSocket, int result) {
        if (result == SAAgent.CONNECTION_SUCCESS)
        {
            if (saSocket != null)
            {
                connHandler = (ProviderServiceConnection) saSocket;
                Log.d(TAG, "Gear connection is successful.");
            }
        }
        else if (result == SAAgent.CONNECTION_ALREADY_EXIST)
        {
            Log.e(TAG, "Gear connection is already exist.");
        }
    }

   /* @Override
    protected void onServiceConnectionResponse(SASocket uThisConnection,
                                               int result) {
        if (result == CONNECTION_SUCCESS) {
            if (uThisConnection != null) {
                final SAProviderConnection myConnection = (SAProviderConnection) uThisConnection;
                if (mConnectionsMap == null) {
                    mConnectionsMap = new HashMap<Integer, SAProviderConnection>();
                }
                myConnection.mConnectionId = (int) (System.currentTimeMillis() & 255);
                Log.d(TAG, "onServiceConnection connectionID = "
                        + myConnection.mConnectionId);
                mConnectionsMap.put(myConnection.mConnectionId, myConnection);
                //              Toast.makeText(getBaseContext(),
                //                  R.string.ConnectionEstablishedMsg, Toast.LENGTH_LONG)
//                        .show();
            } else {
                Log.e(TAG, "SASocket object is null");
            }
        } else {
            Log.e(TAG, "onServiceConnectionResponse result error =" + result);
        }
    } */

    /**
     * The code silently assumes that only one device (one connected PeerId) is there...
     *
     * @param connectedPeerId
     * @param channelId
     * @param data - transferred data from device
     */
    private void onDataAvailableonChannel(String connectedPeerId,
                                          long channelId, String data) {
        Log.i(TAG, "incoming data on channel = " + channelId + ": from peer ="
                + connectedPeerId);
        currentPeerId=connectedPeerId;
        // Step #1: parse the incoming JSON
        Hashtable<String, Object> fieldsIn= new Hashtable<>();
        try {
            JSONObject jO=new JSONObject(data);
            Iterator<String> iter=jO.keys();
            while(iter.hasNext()) {
                String key=iter.next();
                fieldsIn.put(key, jO.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(fieldsIn.containsKey("op") && "status".equals(fieldsIn.get("op"))) {
            boolean success=false;
            int reasonCode=0;
            String reasonText="";
            if(!isRegistered1 && !isRegistered2) {
                reasonCode=1;
                reasonText="No Pipes configured on phpne";
            }
            try {
                myReceiver = new MyReceiver();
                registerReceiver(myReceiver, new IntentFilter(ProviderImpl.INTENT_STATUS));
                Hashtable<String, Object> fields = new Hashtable<>();
                fields.put("op", "challenge");
                if(isRegistered1) {
                    rcvRefCounter++;
                    int op = 1;
                    String ip = ip1Head;
                    int port = port1Head;
                    String alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                    byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);
                    NetworkService.startActionHeadIO(this, INTENT_STATUS, op, ip, port, dataToSend);
                    Hashtable<String, Object> outFields = new Hashtable<>();
                    outFields.put("status", "update");
                    outFields.put("idx", 1);
                    outFields.put("name", pipe1Name);
                    outFields.put("progress", "req chlg");
                    JSONObject jO=new JSONObject(outFields);
                    String payload=jO.toString();
                    sendResponse(currentPeerId, payload);
                }
                if(isRegistered2) {
                    rcvRefCounter++;
                    int op = 2;
                    String ip = ip2Head;
                    int port = port2Head;
                    String alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                    byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);
                    NetworkService.startActionHeadIO(this, INTENT_STATUS, op, ip, port, dataToSend);
                    Hashtable<String, Object> outFields = new Hashtable<>();
                    outFields.put("status", "update");
                    outFields.put("idx", 2);
                    outFields.put("name", pipe2Name);
                    outFields.put("progress", "req chlg");
                    JSONObject jO=new JSONObject(outFields);
                    String payload=jO.toString();
                    sendResponse(currentPeerId, payload);
                }
                success=true;
            }
            catch (KeyStoreException | UnrecoverableKeyException | InvalidKeyException | CertificateException | NoSuchAlgorithmException | SignatureException | IOException e) {
                Log.e("PipeMonitor", e.getMessage());
                reasonCode=2;
                reasonText="Caught a exception on the phone";
            }
            if(!success) {
                Hashtable<String, Object> fields = new Hashtable<>();
                fields.put("status", "fail");
                fields.put("reasonCode", reasonCode);
                fields.put("reasonText", reasonText);
                JSONObject jO=new JSONObject(fields);
                String payload=jO.toString();
                sendResponse(connectedPeerId, payload);
            }
        }
    }

    /**
     *
     * @param step
     * @param data
     */
    private void doStep(int step, Bundle data) {
        boolean success;
        String response, ip;
        int port;
        String alias;
        Hashtable<String, Object> rcvFields = null;
        Hashtable<String, Object> sndfields = new Hashtable<>();
        sndfields.put("idx", (step%2==1)? 1 : 2);
        sndfields.put("name", (step%2==1)? pipe1Name : pipe2Name);

        switch (step) {
            case 1:
            case 2:
                if(step==1) {
                    alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                }
                else {
                    alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                }
                if(!sendRequest(data, (step==1) ? ip1Head : ip2Head, (step==1)? port1Head : port2Head, alias, step+2, "status")) {
                    sndfields.put("status", "fail");
                    sndfields.put("reasonCode", 3);
                    sndfields.put("reasonText", "Error querying Status [Step Challenge]");
                    rcvRefCounter--;
                    if(rcvRefCounter==0) {
                        unregisterReceiver(myReceiver);
                    }
                }
                else {
                    sndfields.put("status", "update");
                    sndfields.put("progress", "req status");
                }
                break;

            case 3:
            case 4:
                success = data.getBoolean("success");
                if (success) {
                    try {
                        rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (((String) (rcvFields.get("status"))).equals("ok")) {
                        sndfields.put("status", "ok");
                        sndfields.put("tailConnected", (Boolean) rcvFields.get("tailConnected"));
                        sndfields.put("connStatus", (String) rcvFields.get("connStatus"));

                        //Date startTime = convertToDate((JSONObject)rcvFields.get("startTime"));
                        //Date lastPing = convertToDate((JSONObject) rcvFields.get("lastPing"));
                        //long seconds = (new Date().getTime() - lastPing.getTime()) / 1000;
                        //boolean isOk = isTailConnected && (seconds < 180);
                        //String statusStrg = (String) rcvFields.get("connStatus");

                    } else {
                        sndfields.put("status", "fail");
                        sndfields.put("reasonCode", 3);
                        sndfields.put("reasonText", "Error querying Status [Step Status]");
                    }
                } else {
                    sndfields.put("status", "fail");
                    sndfields.put("reasonCode", 3);
                    sndfields.put("reasonText", "Error querying Status [Step Status]");
                }
                rcvRefCounter--;
                if(rcvRefCounter==0) {
                    unregisterReceiver(myReceiver);
                }
                break;
        }
        JSONObject jO=new JSONObject(sndfields);
        String payload=jO.toString();
        sendResponse(currentPeerId, payload);
    }

    private Date convertToDate(JSONObject jsonObject){
        try {
            Long lng = jsonObject.getLong("seconds");
            Date date = new Date(lng*1000);
            return date;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * any interaction with the admin server starts with obtaining a challenge. The challenge is needed to make each
     * request different and prevent replay attacks.
     *
     * @param data bundle with fields to send, specifically "data" is relevant
     * @param ip host to send to
     * @param port port on host
     * @param alias key in AndroidKeyStore to use for signature
     * @param nextStep next step to execute in code here after the broadcast is recevied upon response
     * @param opKey actuel operation to be performed
     * @return true - success, false failure.
     */
    private boolean sendRequest(Bundle data, String ip, int port, String alias, int nextStep, String opKey) {
        boolean isSuccess=true;
        boolean success = data.getBoolean("success");
        String response = data.getString("response");
        if (success) {
            Hashtable<String, Object> rcvFields = null;
            try {
                rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (((String) (rcvFields.get("status"))).equals("ok")) {
                String challenge = (String) rcvFields.get("challenge");
                Hashtable<String, Object> fields = new Hashtable<>();
                fields.put("challenge", challenge);
                fields.put("op", opKey);
                try {
                    byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);

                    NetworkService.startActionHeadIO(this, INTENT_STATUS, nextStep, ip, port, dataToSend);
                } catch (KeyStoreException | InvalidKeyException | IOException | SignatureException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException e) {
                    Log.e("PipeMonitor", e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            } else {
                isSuccess=false;
            }
        } else {
            isSuccess=false;
        }
        return isSuccess;
    }

    private void sendResponse(String connectedPeerId, String data) {
        Log.d(TAG, "sendTbListMsg : Enter");

        if (connHandler != null) {
            //final SAProviderConnection uHandler = mConnectionsMap.get(Integer.parseInt(connectedPeerId));
            try {
                connHandler.send(channelId, data.getBytes());
            } catch (final IOException e) {
                Log.e(TAG, "I/O Error occured while send");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPeerAgentUpdated(SAPeerAgent peerAgent, int result) {
        Log.i(TAG, "Peer Updated with status : " + result);
    }


    public class ProviderServiceConnection extends SASocket {
        public static final String TAG = "ProviderServiceConnect";
        private int connectionId;

        public ProviderServiceConnection() {
            super(ProviderServiceConnection.class.getName());
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            Log.i(TAG, "onReceive ENTER channel = " + channelId);
            final String strToUpdateUI = new String(data);
            onDataAvailableonChannel(String.valueOf(connectionId), channelId, // getRemotePeerId()
                    strToUpdateUI);
        }

        @Override
        public void onError(int channelId, String errorString, int error) {
            Log.e(TAG, "ERROR: " + errorString + "  " + error);
        }

        @Override
        public void onServiceConnectionLost(int errorCode) {
            Log.e(TAG, "onServiceConnectionLost  for peer = " + connectionId
                    + "error code =" + errorCode);
            connHandler=null;
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (intent.getAction().equals(INTENT_STATUS)) {
                doStep(bundle.getInt("nextStep"), bundle);
            }
        }

    }

}
