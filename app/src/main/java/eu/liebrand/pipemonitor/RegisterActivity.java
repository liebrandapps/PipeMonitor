package eu.liebrand.pipemonitor;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Hashtable;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String INTENT_REGISTER="eu.liebrand.pipemonitor.registeractivity";
    private MyReceiver myReceiver;
    Dialog dialogFail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button btn=(Button)findViewById(R.id.btnStart);
        btn.setOnClickListener(this);
        RadioButton rBtn1=(RadioButton)findViewById(R.id.rbtnhost1);
        RadioButton rBtn2=(RadioButton)findViewById(R.id.rbtnhost2);


        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String ip1=settings.getString("tailinstanceIP1", "");
        int port1=Integer.parseInt(settings.getString("tailinstancePort1", "0"));
        String ip2=settings.getString("tailinstanceIP2", "");
        int port2=Integer.parseInt(settings.getString("tailinstancePort2", "0"));
        if(ip1.length()==0) {
            rBtn1.setChecked(false);
            rBtn1.setEnabled(false);
            rBtn1.setVisibility(View.INVISIBLE);
        }
        else {
            rBtn1.setText(ip1 + ":" + String.valueOf(port1));
            rBtn1.setChecked(true);
        }
        if(ip2.length()==0) {
            rBtn2.setChecked(false);
            rBtn2.setEnabled(false);
            rBtn2.setVisibility(View.INVISIBLE);
        }
        else {
            rBtn2.setText(ip2 + ":" + String.valueOf(port2));
            if(!rBtn1.isChecked()) {
                rBtn2.setChecked(true);
            }
        }
        if(ip1.length()==0 && ip2.length()==0) {
            btn.setEnabled(false);
            TextView tvDesc=(TextView)findViewById(R.id.lbldescstart);
            tvDesc.setText("No hosts defined, cannot register. Please go to settings to configure hosts.");
        }
        else {
            btn.setEnabled(true);
        }



    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.btnStart) {
            performRegister(1, null);
        }
        if(v.getId()==R.id.btndlgFailok) {
            dialogFail.dismiss();
        }
    }

    public void performRegister(int step, Bundle data) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        RadioButton rBtn1=(RadioButton)findViewById(R.id.rbtnhost1);
        RadioButton rBtn2=(RadioButton)findViewById(R.id.rbtnhost2);
        String idx="";
        if(rBtn1.isChecked()) {
            idx="1";
        }
        if(rBtn2.isChecked()) {
            idx="2";
        }
        String ip=settings.getString("tailinstanceIP" + idx, "");
        int port=Integer.parseInt(settings.getString("tailinstancePort" + idx, ""));
        TextView tvResult;
        String alias=NetworkService.KEY_ALIAS_PREFIX + ip + "_" + String.valueOf(port)+idx;

        switch(step) {
            case 1:
                TextView tvHeadInfo = (TextView) findViewById(R.id.lblheadinfo);
                tvHeadInfo.setText("");
                NetworkService.startActionGetHeadParameter(this, RegisterActivity.INTENT_REGISTER, 2, ip, port);
                break;
            case 2:
                boolean success=data.getBoolean("success");
                String response=data.getString("response");
                tvResult=(TextView)findViewById(R.id.statusstep1);
                String adminStatus=success? data.getString("adminStatus") : "";
                if(success && adminStatus.equals("ok")) {
                    tvResult.setText("ok");
                    String privateKey = data.getString("privateKey");
                    String publicKey = data.getString("publicKey");
                    String headHost = data.getString("host");
                    String instanceName = data.getString("instanceName");
                    int headPort = data.getInt("port");
                    SharedPreferences.Editor edit=settings.edit();
                    edit.putString("headHost" +idx, headHost);
                    edit.putInt("headPort" +idx, headPort);
                    edit.putString("pipeName" +idx, instanceName);
                    edit.commit();
                    tvHeadInfo = (TextView) findViewById(R.id.lblheadinfo);
                    tvHeadInfo.setText("Head is at " + headHost + ":" + String.valueOf(headPort));
                    try {
                        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                        ks.load(null,null);
                        if (ks.containsAlias(alias)) {
                            ks.deleteEntry(alias);
                        }
                        byte[] pkcs8EncodedBytes = Base64.decode(privateKey, Base64.DEFAULT);
                        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        PrivateKey privKey = kf.generatePrivate(privKeySpec);

                        byte[] publicBytes = Base64.decode(publicKey, Base64.DEFAULT);
                        //X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicBytes);
                        //KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        //PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
                        //X509EncodedKeySpec x509=new X509EncodedKeySpec(publicBytes);
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(publicBytes));
                        Certificate[] arrCert = new Certificate[1];
                        arrCert[0] = cert;

                        ks.setKeyEntry(alias, privKey, null, arrCert);

                        Hashtable<String, Object> fields= new Hashtable<>();
                        fields.put("op", "challenge");

                        byte [] dataToSend=NetworkService.prepareHeadRequest(alias, fields);

                        NetworkService.startActionHeadIO(this, INTENT_REGISTER, 3, headHost, headPort, dataToSend);
                        Log.i("PipeMonitor", "Stored Private and Public Key in KeyStore");
                    } catch (KeyStoreException | UnrecoverableKeyException | InvalidKeyException | InvalidKeySpecException | CertificateException | NoSuchAlgorithmException | SignatureException | IOException e) {
                        Log.e("PipeMonitor", e.getMessage());
                        showFailureDialog("Exception", e.getMessage(), ip, port);
                    }

                }
                else {
                    tvResult.setText("fail");
                    showFailureDialog("Error registering with Tail instance [Step 1]", response, ip, port);
                }
                break;
            case 3:
                success=data.getBoolean("success");
                response=data.getString("response");
                tvResult=(TextView)findViewById(R.id.statusstep2);
                if(success) {
                    tvResult.setText("ok");
                    Hashtable<String, Object> rcvFields= null;
                    try {
                        rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(((String)(rcvFields.get("status"))).equals("ok")) {
                        String challenge = (String) rcvFields.get("challenge");
                        Hashtable<String, Object> fields = new Hashtable<>();
                        fields.put("challenge", challenge);
                        fields.put("op", "status");
                        try {
                            byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);
                            String headHost = settings.getString("headHost"+idx, "");
                            int headPort = settings.getInt("headPort"+idx, 0);

                            NetworkService.startActionHeadIO(this, INTENT_REGISTER, 4, headHost, headPort, dataToSend);
                        } catch (KeyStoreException | InvalidKeyException | IOException | SignatureException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException e) {
                            Log.e("PipeMonitor", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else {
                        tvResult.setText("fail");
                        showFailureDialog("Error registering with Tail instance [Step 2]", response, ip, port);
                    }

                }
                else {
                    tvResult.setText("fail");
                    showFailureDialog("Error registering with Tail instance [Step 2]", response, ip, port);
                }
                break;
            case 4:
                success=data.getBoolean("success");
                response=data.getString("response");
                tvResult=(TextView)findViewById(R.id.statusstep3);
                if(success) {
                    tvResult.setText("SUCCESS!");
                    Hashtable<String, Object> rcvFields= null;
                    try {
                        rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(((String)(rcvFields.get("status"))).equals("ok")) {
                        TextView tvInfo1 = (TextView)findViewById(R.id.valserverinfo1);
                        TextView tvInfo2 = (TextView)findViewById(R.id.valserverinfo2);
                        TextView tvInfo3 = (TextView)findViewById(R.id.valserverinfo3);
                        TextView tvInfo4 = (TextView)findViewById(R.id.valserverinfo4);
                        Date startTime = convertToDate((JSONObject)rcvFields.get("startTime"));
                        Date lastPing = convertToDate((JSONObject)rcvFields.get("lastPing"));
                        boolean isTailConnected= ((Boolean)rcvFields.get("tailConnected")).booleanValue();
                        String info1 = "Head is running since " + startTime.toString();
                        String info2 = String.format("Tail connected %d times", rcvFields.get("reconnects"));
                        String info3 = "Tail is currently " + (isTailConnected ? "connected" : "not connected");
                        String info4 = "Last Ping was at " + lastPing.toString();
                        tvInfo1.setText(info1);
                        tvInfo2.setText(info2);
                        tvInfo3.setText(info3);
                        tvInfo4.setText(info4);
                        SharedPreferences.Editor editor=settings.edit();
                        editor.putBoolean("isRegistered" + idx, true);
                        editor.commit();
                    }
                    else {
                        tvResult.setText("fail");
                        showFailureDialog("Error registering with Tail instance [Step 3]", response, ip, port);
                    }
                }
                else {
                    tvResult.setText("fail");
                    showFailureDialog("Error registering with Tail instance [Step 3]", response, ip, port);
                }

                break;

        }
    }

    private void showFailureDialog(String title, String detail, String ip, int port) {
        dialogFail=new Dialog(this);
        dialogFail.setContentView(R.layout.dialog_register_fail);
        Button btn=(Button)dialogFail.findViewById(R.id.btndlgFailok);
        btn.setOnClickListener(this);
        TextView tv=(TextView)dialogFail.findViewById(R.id.valip);
        tv.setText(ip);
        tv=(TextView)dialogFail.findViewById(R.id.valport);
        tv.setText(String.valueOf(port));
        tv=(TextView)dialogFail.findViewById(R.id.faildetail);
        tv.setText(detail);
        dialogFail.setTitle(title);
        dialogFail.show();
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


    @Override
    public void onResume() {
        super.onResume();

        myReceiver = new MyReceiver();
        registerReceiver(myReceiver, new IntentFilter(RegisterActivity.INTENT_REGISTER));
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(myReceiver);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (intent.getAction().equals(INTENT_REGISTER)) {
                performRegister(bundle.getInt("nextStep"), bundle);
            }
        }

    }
}

