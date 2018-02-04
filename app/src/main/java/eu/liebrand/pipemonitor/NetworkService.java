package eu.liebrand.pipemonitor;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NetworkService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    public static final String KEY_ALIAS_PREFIX="eu.liebrand.pipemonitor.";
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_GET_PARAMS = "eu.liebrand.pipemonitor.action.get.params";
    private static final String ACTION_HEAD_IO = "eu.liebrand.pipemonitor.action.head.io";


    private static final String FIELD_PRIVATEKEY="privateKey";
    private static final String FIELD_HOST="host";
    private static final String FIELD_PORT="port";

    // TODO: Rename parameters
    private static final String EXTRA_INTENT="eu.liebrand.pipemonitor.intent";
    private static final String EXTRA_IP = "eu.liebrand.pipemonitor.extra.ip";
    private static final String EXTRA_PORT = "eu.liebrand.pipemonitor.extra.port";
    private static final String EXTRA_PAYLOAD = "eu.liebrand.pipemonitor.extra.payload";
    private static final String EXTRA_STEP = "eu.liebrand.pipemonitor.extra.step";

    public NetworkService() {
        super("NetworkService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionGetHeadParameter(Context context, String respIntent, int nextStep, String ip, int port) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.setAction(ACTION_GET_PARAMS);
        intent.putExtra(EXTRA_INTENT, respIntent);
        intent.putExtra(EXTRA_STEP, nextStep);
        intent.putExtra(EXTRA_IP, ip);
        intent.putExtra(EXTRA_PORT, port);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionHeadIO(Context context, String respIntent, int nextStep, String ip, int port, byte [] payload) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.setAction(ACTION_HEAD_IO);
        intent.putExtra(EXTRA_INTENT, respIntent);
        intent.putExtra(EXTRA_STEP, nextStep);
        intent.putExtra(EXTRA_IP, ip);
        intent.putExtra(EXTRA_PORT, port);
        intent.putExtra(EXTRA_PAYLOAD, payload);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
         if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_PARAMS.equals(action)) {
                final String ip = intent.getStringExtra(EXTRA_IP);
                final int port = intent.getIntExtra(EXTRA_PORT,0);
                final String respIntent = intent.getStringExtra(EXTRA_INTENT);
                final int nextStep = intent.getIntExtra(EXTRA_STEP,0);
                handleActionGetParams(respIntent, nextStep, ip, port);
            } else if (ACTION_HEAD_IO.equals(action)) {
                final String ip = intent.getStringExtra(EXTRA_IP);
                final int port = intent.getIntExtra(EXTRA_PORT,0);
                final String respIntent = intent.getStringExtra(EXTRA_INTENT);
                final int nextStep = intent.getIntExtra(EXTRA_STEP,0);
                final byte [] payload = intent.getByteArrayExtra(EXTRA_PAYLOAD);
                handleActionHeadIO(respIntent, nextStep, ip, port, payload);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionGetParams(String respIntent, int nextStep, String ip, int port) {
        Socket socket = null;
        String response = "";
        boolean success = false;
        Bundle bundle=new Bundle();
        try {
            socket = new Socket(ip, port);
            InputStream inputStream=socket.getInputStream();
            DataItem itemLen=StreamReader.readItem(inputStream);
            byte [] buffer = new byte[(int)itemLen.getLong()];
            int bytesRead=0;
            while(bytesRead<itemLen.getLong()) {
                bytesRead+=inputStream.read(buffer,bytesRead, (int)(itemLen.getLong())-bytesRead);
            }
            Vector<DataItem> ht=new Vector<DataItem>();
            InputStream inStream=new ByteArrayInputStream(buffer);
            try {
                while(true) {
                    DataItem item=StreamReader.readItem(inStream);
                    if(item==null) break;
                    ht.add(item);
                }
            }
            catch(IOException e) {
            }
            Iterator<DataItem> iter=ht.iterator();
            while (iter.hasNext()) {
                DataItem item=iter.next();
                if(item.getType()==DataItem.TYPE_NUMBER) {
                    bundle.putInt(item.getKey(), (int) item.getLong());
                }
                else if(item.getType()==DataItem.TYPE_BINARY) {
                    bundle.putByteArray(item.getKey(), item.getBinary());
                }
                else {
                    bundle.putString(item.getKey(), item.getString());
                }
            }
            response = String.format("Received %d bytes from host %s:%d", bytesRead, ip, port);
            success=true;
        } catch (UnknownHostException e) {
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    response = "IOException: " + e.toString();
                }
            }
        }
        bundle.putBoolean("success", success);
        bundle.putString("response", response);
        bundle.putInt("nextStep", nextStep);
        Intent intent=new Intent();
        intent.setAction(respIntent);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionHeadIO(String respIntent, int nextStep, String ip, int port, byte [] payload) {
        Socket socket=new Socket();
        boolean success=false;
        String response="nothing to report yet";
        Bundle bundle = new Bundle();
        try {
            socket = new Socket(ip, port);
            OutputStream outputStream=socket.getOutputStream();
            outputStream.write(payload);
            InputStream inputStream=socket.getInputStream();
            DataItem itemLen=StreamReader.readItem(inputStream);
            if(itemLen==null) {
                response="Unexpected response, may be the server exited";
            }
            else {
                byte[] buffer = new byte[(int) itemLen.getLong()];
                int bytesRead = 0;
                while (bytesRead < itemLen.getLong()) {
                    bytesRead += inputStream.read(buffer, bytesRead, (int) (itemLen.getLong()) - bytesRead);
                }
                bundle.putByteArray("data", buffer);
                response = String.format("Received %d bytes from host %s:%d", bytesRead, ip, port);
                success = true;
            }
        } catch (UnknownHostException e) {
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    response = "IOException: " + e.toString();
                }
            }
        }

        bundle.putBoolean("success", success);
        bundle.putString("response", response);
        bundle.putInt("nextStep", nextStep);
        Intent intent=new Intent();
        intent.setAction(respIntent);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }


    /**
     *
     * @param alias - is needed to read the private key out of the keystore
     * @param fields - list of fields to be sent, will be converted to JSON
     * @return byte array in encoded from in containes the JSON with with fields and separate the calculated signature.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws SignatureException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws CertificateException
     */
    public static byte [] prepareHeadRequest(String alias, Hashtable<String, Object> fields) throws KeyStoreException,
            NoSuchAlgorithmException, UnrecoverableKeyException, SignatureException, IOException, InvalidKeyException, CertificateException {

        // Step 1: Build JSON structure
        JSONObject jO=new JSONObject(fields);
        String payload=jO.toString();

        //Step 2: sign w/ private key
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null, null);
        PrivateKey key = (PrivateKey)ks.getKey(alias, null);

        byte [] data=payload.getBytes("UTF-8");
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(data);
        byte [] sigData=sig.sign();

        //Step 3: package for transmission
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamWriter.write(outputStream, new DataItem("payload", data));
        StreamWriter.write(outputStream, new DataItem("signature", sigData));
        byte [] buffer = outputStream.toByteArray();
        outputStream=new ByteArrayOutputStream();
        StreamWriter.write(outputStream, new DataItem(buffer.length));
        outputStream.write(buffer);

        return outputStream.toByteArray();
    }

    public static Hashtable<String, Object> processHeadResponse(byte [] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        Hashtable<String, Object> fields=new Hashtable<>();

        while(inputStream.available()>0) {
            DataItem item=StreamReader.readItem(inputStream);
            if(item.getType()==DataItem.TYPE_STRING) {
                fields.put(item.getKey(), item.getString());
            }
            else if(item.getType()==DataItem.TYPE_NUMBER) {
                fields.put(item.getKey(), new Long(item.getLong()));
            }
            else if(item.getType()==DataItem.TYPE_BINARY) {
                fields.put(item.getKey(), item.getBinary());
            }
        }
        if(fields.containsKey("result")) {
            try {
                JSONObject jO=new JSONObject((String)fields.get("result"));
                Iterator<String> iter=jO.keys();
                while(iter.hasNext()) {
                    String key=iter.next();
                    fields.put(key, jO.get(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return fields;
    }

}
