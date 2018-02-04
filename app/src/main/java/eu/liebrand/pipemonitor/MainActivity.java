package eu.liebrand.pipemonitor;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
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
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String INTENT_STATUS = "eu.liebrand.pipemonitor.mainactivity";
    private MyReceiver myReceiver;
    Dialog dialogFail;

    String ip1Head, ip2Head, ip1Tail, ip2Tail;
    int port1Head, port2Head, port1Tail, port2Tail;
    boolean isRegistered1, isRegistered2;
    private enum SHOW {  Nothing, Buttons, Details, HourStat, DayStat };
    SHOW show1, show2;
    //boolean isShowingDetails1, isShowingDetails2, isShowingStatHour1, isShowingStatHour2, isShowingStatDay1, isShowingStatDay2;
    boolean isRunning1, isRunning2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        prepareUI();

        updateDetails(1, SHOW.Nothing);
        updateDetails(2, SHOW.Nothing);
    }

    void prepareUI() {
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.btnRefresh);
        btn.setOnClickListener(this);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        ip1Tail = settings.getString("tailinstanceIP1", "");
        port1Tail = Integer.parseInt(settings.getString("tailinstancePort1", "0"));
        ip2Tail = settings.getString("tailinstanceIP2", "");
        port2Tail = Integer.parseInt(settings.getString("tailinstancePort2", "0"));
        ip1Head = settings.getString("headHost1", "");
        port1Head = settings.getInt("headPort1", 0);
        ip2Head = settings.getString("headHost2", "");
        port2Head = settings.getInt("headPort2", 0);
        String pipe1Name=settings.getString("pipeName1", "");
        String pipe2Name=settings.getString("pipeName2", "");
        isRegistered1 = settings.getBoolean("isRegistered1", false);
        isRegistered2 = settings.getBoolean("isRegistered2", false);
        TextView tvInstructions = (TextView) findViewById(R.id.lblinstructions);
        TextView tvPipe1Name = (TextView) findViewById(R.id.valpipe1);
        TextView tvPipe2Name = (TextView) findViewById(R.id.valpipe2);
        tvPipe1Name.setText(pipe1Name);
        tvPipe2Name.setText(pipe2Name);

        if (ip1Tail.length() == 0 && ip2Tail.length() == 0) {
            tvInstructions.setText("Please configure your tail hosts in 'Settings'");
        } else if (!isRegistered1 && !isRegistered2) {
            tvInstructions.setText("Please register your pipe(s) with 'Register'");
        } else {
            tvInstructions.setVisibility(View.GONE);
        }
        if (!isRegistered1) {
            int ids[] = {R.id.lblpipe1, R.id.lblfrom1, R.id.lblto1, R.id.lblstatus1, R.id.valstatus1, R.id.btndetail1, R.id.btnstatday1, R.id.btnstathour1, R.id.anline1};
            for (int id : ids) {
                View v = findViewById(id);
                v.setVisibility(View.GONE);
            }
        }
        else {
            TextView tvf=(TextView)findViewById(R.id.lblfrom1);
            tvf.setText(ip1Tail);
            TextView tvt=(TextView)findViewById(R.id.lblto1);
            tvt.setText(ip1Head);
        }
        if (!isRegistered2) {
            int ids[] = {R.id.lblpipe2, R.id.lblfrom2, R.id.lblto2, R.id.lblstatus2, R.id.valstatus2, R.id.btndetail2, R.id.btnstatday2, R.id.btnstathour2, R.id.anline2};
            for (int id : ids) {
                View v = findViewById(id);
                v.setVisibility(View.GONE);
            }
        }
        else {
            TextView tvf=(TextView)findViewById(R.id.lblfrom2);
            tvf.setText(ip2Tail);
            TextView tvt=(TextView)findViewById(R.id.lblto2);
            tvt.setText(ip2Head);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0); //fix the color to white
            item.setTitle(spanString);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                intent = new Intent(this, MyPreferencesActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_register:
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("show1", show1);
        outState.putSerializable("show2", show2);
        int [] ids = { R.id.valstatus1, R.id.valstatus2, R.id.lblfrom1, R.id.lblto1, R.id.lblfrom2, R.id.lblto2,
                        R.id.valserverinfo1a, R.id.valserverinfo1b, R.id.valserverinfo1c, R.id.valserverinfo1d,
                    R.id.valserverinfo2a, R.id.valserverinfo2b, R.id.valserverinfo2c, R.id.valserverinfo2d,
        };
        for(int id : ids) {
            TextView tv = (TextView) findViewById(id);
            outState.putString(String.valueOf(id), tv.getText().toString());
        }
        outState.putIntArray("ids", ids);
        BarChartView b=(BarChartView)findViewById(R.id.graph1);
        b.storeAllSeries(outState, R.id.graph1);
        b=(BarChartView)findViewById(R.id.graph2);
        b.storeAllSeries(outState, R.id.graph2);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int [] ids = savedInstanceState.getIntArray("ids");
        for(int id : ids) {
            TextView tv = (TextView) findViewById(id);
            tv.setText(savedInstanceState.getString(String.valueOf(id)));
        }
        updateDetails(1, (SHOW)savedInstanceState.getSerializable("show1"));
        updateDetails(2, (SHOW)savedInstanceState.getSerializable("show2"));
        BarChartView b=(BarChartView)findViewById(R.id.graph1);
        b.restoreAllSeries(savedInstanceState, R.id.graph1);
        b=(BarChartView)findViewById(R.id.graph2);
        b.restoreAllSeries(savedInstanceState, R.id.graph2);
    }


    @Override
    public void onClick(View v) {
        Hashtable<String, Object> fields= new Hashtable<>();
        fields.put("op", "challenge");
        String ip="";
        int port=0;
        int op=0;
        String alias="";
        boolean reqChallenge=false;
        int whichPipe=0;

        if(v.getId()==R.id.btndlgFailok) {
            dialogFail.dismiss();
            return;
        }

        try {
            if (v.getId() == R.id.btnRefresh) {
                prepareUI();
                if(isRegistered1) {
                    op=1;
                    ip = ip1Head;
                    port = port1Head;
                    alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                    byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);
                    NetworkService.startActionHeadIO(this, INTENT_STATUS, op, ip, port, dataToSend);
                    updateDetails(1, SHOW.Nothing);
                    TextView tv = (TextView) findViewById(R.id.valstatus1);
                    tv.setText("-");
                }
                if(isRegistered2) {
                    op=2;
                    ip = ip2Head;
                    port = port2Head;
                    alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                    byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);
                    NetworkService.startActionHeadIO(this, INTENT_STATUS, op, ip, port, dataToSend);
                    updateDetails(2, SHOW.Nothing);
                    TextView tv = (TextView) findViewById(R.id.valstatus2);
                    tv.setText("-");
                }
            }
            if(v.getId()==R.id.btndetail1) {
                if(show1==SHOW.Details) {
                    updateDetails(1, SHOW.Buttons);
                }
                else {
                    whichPipe = 1;
                    op = 11;
                    reqChallenge = true;
                }
            }
            if(v.getId()==R.id.btnstathour1) {
                if(show1==SHOW.HourStat) {
                    updateDetails(1, SHOW.Buttons);
                }
                else {
                    whichPipe = 1;
                    op = 21;
                    reqChallenge = true;
                }
            }
            if(v.getId()==R.id.btnstatday1) {
                if(show1==SHOW.DayStat) {
                    updateDetails(1, SHOW.Buttons);
                }
                else {
                    whichPipe = 1;
                    op = 31;
                    reqChallenge = true;
                }
            }
            if(v.getId()==R.id.btndetail2) {
                if(show2==SHOW.Details) {
                    updateDetails(2, SHOW.Buttons);
                }
                else {
                    whichPipe = 2;
                    op = 12;
                    reqChallenge = true;
                }
            }
            if(v.getId()==R.id.btnstathour2) {
                if(show2==SHOW.HourStat) {
                    updateDetails(2, SHOW.Buttons);
                }
                else {
                    whichPipe = 2;
                    op = 22;
                    reqChallenge = true;
                }
            }
            if(v.getId()==R.id.btnstatday2) {
                if(show2==SHOW.DayStat) {
                    updateDetails(2, SHOW.Buttons);
                }
                else {
                    whichPipe = 2;
                    op = 32;
                    reqChallenge = true;
                }
            }
            if(v.getId()==R.id.graph1) {
                eu.liebrand.pipemonitor.BarChartView g=(eu.liebrand.pipemonitor.BarChartView)findViewById(R.id.graph1);
                int seriesCount=g.getSeriesCount();
                g.setCurrentSeries((g.getCurrentSeries()+1) % seriesCount);
                g.invalidate();
            }
            if(v.getId()==R.id.graph2) {
                eu.liebrand.pipemonitor.BarChartView g=(eu.liebrand.pipemonitor.BarChartView)findViewById(R.id.graph2);
                int seriesCount=g.getSeriesCount();
                g.setCurrentSeries((g.getCurrentSeries()+1) % seriesCount);
                g.invalidate();
            }
            if(reqChallenge) {
                switch (whichPipe) {
                    case 1:
                        ip = ip1Head;
                        port = port1Head;
                        alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                        break;
                    case 2:
                        ip = ip2Head;
                        port = port2Head;
                        alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                        break;
                }
                byte[] dataToSend = NetworkService.prepareHeadRequest(alias, fields);
                NetworkService.startActionHeadIO(this, INTENT_STATUS, op, ip, port, dataToSend);
            }
        } catch (KeyStoreException | UnrecoverableKeyException | InvalidKeyException | CertificateException | NoSuchAlgorithmException | SignatureException | IOException e) {
            Log.e("PipeMonitor", e.getMessage());
            showFailureDialog("Exception", e.getMessage(), ip, port);
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

    private void updateUI(Intent intent, int step, Bundle data) {
        boolean success;
        String response, ip;
        int port;
        String alias;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Hashtable<String, Object> rcvFields = null;

        switch(step) {
            case 1:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                sendRequest(data, ip1Head, port1Head, alias, 3, "status");
                break;
            case 2:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                sendRequest(data, ip2Head, port2Head, alias, 4, "status");
                break;
            case 3:
            case 4:
                success = data.getBoolean("success");
                response = data.getString("response");
                ip = (step == 3 ? ip1Head : ip2Head);
                port = (step == 3 ? port1Head : port2Head);
                TextView tv;
                ImageView ivLeft, ivRight;
                if (step == 3) {
                    tv = (TextView) findViewById(R.id.valstatus1);
                    ivLeft = (ImageView) findViewById(R.id.iconserver1left);
                    ivRight = (ImageView) findViewById(R.id.iconserver1right);
                } else {
                    tv = (TextView) findViewById(R.id.valstatus2);
                    ivLeft = (ImageView) findViewById(R.id.iconserver2left);
                    ivRight = (ImageView) findViewById(R.id.iconserver2right);
                }
                if (success) {
                    try {
                        rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (((String) (rcvFields.get("status"))).equals("ok")) {
                        boolean isTailConnected = (Boolean) rcvFields.get("tailConnected");
                        //Date startTime = convertToDate((JSONObject)rcvFields.get("startTime"));

                        //Date lastPing = convertToDate((JSONObject) rcvFields.get("lastPing"));
                        //long seconds = (new Date().getTime() - lastPing.getTime()) / 1000;
                        //boolean isOk = isTailConnected && (seconds < 180);
                        String statusStrg=(String) rcvFields.get("connStatus");
                        tv.setText(statusStrg);
                        if(statusStrg.equals("wait")) {
                            ivLeft.setImageResource(R.mipmap.ic_wait);
                            ivRight.setImageResource(R.mipmap.ic_wait);
                        }
                        if(statusStrg.equals("idle")) {
                            ivLeft.setImageResource(R.mipmap.ic_ready);
                            ivRight.setImageResource(R.mipmap.ic_ready);
                        }
                        if(statusStrg.equals("busy")) {
                            ivLeft.setImageResource(R.mipmap.ic_running);
                            ivRight.setImageResource(R.mipmap.ic_running);
                        }
                        updateDetails((step==3? 1 : 2), SHOW.Buttons);
                    } else {
                        showFailureDialog("Error querying Status [Step Status]", response, ip, port);
                        tv.setText(R.string.statusdead);
                        updateDetails((step==3? 1 : 2), SHOW.Nothing);
                    }
                } else {
                    showFailureDialog("Error querying Status [Step Status]", response, ip, port);
                    tv.setText(R.string.statusdead);
                    updateDetails((step==3? 1 : 2), SHOW.Nothing);
                }

                break;
            case 11:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                sendRequest(data, ip1Head, port1Head, alias, 13, "status");
                break;
            case 12:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                sendRequest(data, ip2Head, port2Head, alias, 14, "status");
                break;
            case 13:
            case 14:
                try {
                    rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (((String) (rcvFields.get("status"))).equals("ok")) {
                    TextView tvInfo1, tvInfo2, tvInfo3, tvInfo4;
                    if(step==13) {
                        updateDetails(1, SHOW.Details);
                        tvInfo1 = (TextView)findViewById(R.id.valserverinfo1a);
                        tvInfo2 = (TextView)findViewById(R.id.valserverinfo1b);
                        tvInfo3 = (TextView)findViewById(R.id.valserverinfo1c);
                        tvInfo4 = (TextView)findViewById(R.id.valserverinfo1d);
                    }
                    else {
                        updateDetails(2, SHOW.Details);
                        tvInfo1 = (TextView)findViewById(R.id.valserverinfo2a);
                        tvInfo2 = (TextView)findViewById(R.id.valserverinfo2b);
                        tvInfo3 = (TextView)findViewById(R.id.valserverinfo2c);
                        tvInfo4 = (TextView)findViewById(R.id.valserverinfo2d);
                    }
                    Date now=new Date();
                    Date startTime = convertToDate((JSONObject)rcvFields.get("startTime"));
                    boolean isTailConnected= (Boolean) rcvFields.get("tailConnected");
                    String info1 = "Head is running since " + tdiffToText(timeDifference(startTime, now));
                    String info2 = String.format("Tail connected %d times", rcvFields.get("reconnects"));
                    String info3 = "Tail is currently " + (isTailConnected ? "connected" : "not connected");
                    String info4 = "";
                    if(rcvFields.get("lastPing").equals("never")) {
                         info4="No ping received yet. First ping is expected 180 seconds after connect";
                    }
                    else {
                        Date lastPing = convertToDate((JSONObject) rcvFields.get("lastPing"));
                        info4 = "Last Ping was " + tdiffToText(timeDifference(lastPing, now)) + " ago";
                    }
                    tvInfo1.setText(info1);
                    tvInfo2.setText(info2);
                    tvInfo3.setText(info3);
                    tvInfo4.setText(info4);
                }
                break;
            case 21:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                sendRequest(data, ip1Head, port1Head, alias, 23, "statisticHour");
                break;
            case 22:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                sendRequest(data, ip2Head, port2Head, alias, 24, "statisticHour");
                break;
            case 23:
            case 24:
                try {
                    rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (((String) (rcvFields.get("status"))).equals("ok")) {
                    try {
                        TreeMap<Long, JSONObject> tm= new TreeMap<>();
                        JSONArray arr = (JSONArray) rcvFields.get("keys");
                        if(arr==null) {
                            AlertDialog notify=new AlertDialog.Builder(this).create();
                            notify.setTitle("Statistics");
                            notify.setMessage("No data to show. (Statistics tracking may have been started only recently");
                            notify.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            notify.show();
                            //no data received
                            updateDetails(step == 23 ? 1 : 2, SHOW.Buttons);
                        }
                        else {
                            for (int i = 0; i < arr.length(); i++) {
                                String key = arr.getString(i);
                                JSONObject v = (JSONObject) rcvFields.get(key);
                                long ts = v.getLong("timestamp");
                                tm.put(ts, v);
                            }
                            int[] dtaBytesIn = new int[tm.size()];
                            int[] dtaBytesOut = new int[tm.size()];
                            int[] dtaPacketsIn = new int[tm.size()];
                            int[] dtaPacketsOut = new int[tm.size()];
                            int[] dtaReconnects = new int[tm.size()];
                            int idx = 0;
                            BarChartView graphView = (BarChartView) findViewById(step == 23 ? R.id.graph1 : R.id.graph2);
                            for (Map.Entry<Long, JSONObject> entry : tm.entrySet()) {
                                JSONObject v = entry.getValue();
                                Long key = entry.getKey();
                                dtaBytesIn[idx] = v.getInt("bytesIn");
                                dtaBytesOut[idx] = v.getInt("bytesOut");
                                dtaPacketsIn[idx] = v.getInt("packetsIn");
                                dtaPacketsOut[idx] = v.getInt("packetsOut");
                                dtaReconnects[idx++] = v.getInt("reconnects");
                            }
                            graphView.clear();
                            graphView.addSeries(dtaBytesIn, "Bytes Incoming");
                            graphView.addSeries(dtaBytesOut, "Bytes Outgoing");
                            graphView.addSeries(dtaPacketsIn, "Packets Incoming");
                            graphView.addSeries(dtaPacketsOut, "Packets Outcoming");
                            graphView.addSeries(dtaReconnects, "Reconnect");
                            graphView.setSpacing(-1, 10);
                            graphView.setChartProps(-1, true, true, true);
                            updateDetails(step == 23 ? 1 : 2, SHOW.HourStat);
                            graphView.setCurrentSeries(0);
                        }
                    }
                    catch (JSONException e) {
                        Log.e("PipeMonitor", "Stats Hour", e);
                    }
                }
                break;
            case 31:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip1Tail + "_" + String.valueOf(port1Tail) + "1";
                sendRequest(data, ip1Head, port1Head, alias, 33, "statisticDay");
                break;
            case 32:
                alias = NetworkService.KEY_ALIAS_PREFIX + ip2Tail + "_" + String.valueOf(port2Tail) + "2";
                sendRequest(data, ip2Head, port2Head, alias, 34, "statisticDay");
                break;
            case 33:
            case 34:
                try {
                    rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
                    if (((String) (rcvFields.get("status"))).equals("ok")) {
                        TreeMap<Long, JSONObject> tm= new TreeMap<>();
                        JSONArray arr = (JSONArray) rcvFields.get("keys");
                        if(arr==null) {
                            AlertDialog notify=new AlertDialog.Builder(this).create();
                            notify.setTitle("Statistics");
                            notify.setMessage("No data to show. (Statistics tracking may have been started only recently");
                            notify.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            notify.show();
                            //no data received
                            updateDetails(step == 33 ? 1 : 2, SHOW.Buttons);
                        }
                        else {
                            for (int i = 0; i < arr.length(); i++) {
                                String key = arr.getString(i);
                                JSONObject v = (JSONObject) rcvFields.get(key);
                                long ts = v.getLong("timestamp");
                                tm.put(ts, v);
                            }
                            int[] dtaBytesIn = new int[tm.size()];
                            int[] dtaBytesOut = new int[tm.size()];
                            int[] dtaPacketsIn = new int[tm.size()];
                            int[] dtaPacketsOut = new int[tm.size()];
                            int[] dtaReconnects = new int[tm.size()];
                            int idx = 0;
                            BarChartView graphView = (BarChartView) findViewById(step == 33 ? R.id.graph1 : R.id.graph2);
                            for (Map.Entry<Long, JSONObject> entry : tm.entrySet()) {
                                JSONObject v = entry.getValue();
                                Long key = entry.getKey();
                                dtaBytesIn[idx] = v.getInt("bytesIn");
                                dtaBytesOut[idx] = v.getInt("bytesOut");
                                dtaPacketsIn[idx] = v.getInt("packetsIn");
                                dtaPacketsOut[idx] = v.getInt("packetsOut");
                                dtaReconnects[idx++] = v.getInt("reconnects");
                            }
                            graphView.clear();
                            graphView.addSeries(dtaBytesIn, "Bytes Incoming");
                            graphView.addSeries(dtaBytesOut, "Bytes Outgoing");
                            graphView.addSeries(dtaPacketsIn, "Packets Incoming");
                            graphView.addSeries(dtaPacketsOut, "Packets Outcoming");
                            graphView.addSeries(dtaReconnects, "Reconnect");
                            graphView.setSpacing(-1, 10);
                            graphView.setChartProps(-1, true, true, true);
                            updateDetails(step == 33 ? 1 : 2, SHOW.DayStat);
                            graphView.setCurrentSeries(0);
                            graphView.prepareCharts();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    Log.e("PipeMonitor", "Stats Day", e);
                }

                break;
        }
    }

    private void sendRequest(Bundle data, String ip, int port, String alias, int nextStep, String opKey) {
        boolean success = data.getBoolean("success");
        String response = data.getString("response");
        if (success) {
            Hashtable<String, Object> rcvFields = null;
            try {
                rcvFields = NetworkService.processHeadResponse(data.getByteArray("data"));
            } catch (IOException e) {
                e.printStackTrace();
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
                }
            } else {
                showFailureDialog("Error querying Status [Step Challenge]", response, ip, port);
            }
        } else {
            showFailureDialog("Error querying Status [Step Challenge]", response, ip, port);
        }

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

    private int[] timeDifference(Date earlier, Date later) {
        long mills = Math.abs(later.getTime() - earlier.getTime());
        int [] result=new int[4];

        result[3]=(int) (mills/(1000*60*60*24));
        result[2] = (int) (mills/(1000 * 60 * 60)) % 24;
        result[1] = (int) (mills/(1000*60)) % 60;
        result[0] = (int) (mills / 1000) % 60;
        return result;
    }

    private String tdiffToText(int[] td) {
        StringBuilder sb=new StringBuilder();
        if(td[3]>0) {
            sb.append(td[3]);
            sb.append((td[3]==1)? " day" : " days");
            if(td[2]>0 || td[1]>0 || td[0]>0) {
                sb.append(((td[2]==0 && td[1]==0) || (td[2]==0 && td[0]==0) || (td[1]==0 && td[0]==0))? " and " : ", ");
            }
        }
        if(td[2]>0) {
            sb.append(td[2]);
            sb.append((td[2]==1)? " hour" : " hours");
            if(td[1]>0 || td[0]>0) {
                sb.append((td[1]>0 && td[0]>0)? ", " : " and ");
            }
        }
        if(td[1]>0) {
            sb.append(td[1]);
            sb.append((td[1]==1)? " minute" : " minutes");
            if(td[0]>0) {
                sb.append(" and ");
            }
        }
        if(td[0]>0) {
            sb.append(td[0]);
            sb.append((td[0]==1)? " second" : " seconds");
        }
        return sb.toString();
    }

    private void updateDetails(int whichServer, SHOW whatToShow) {
        int [] buttonIds=null;
        int [] infoIds=null;
        int [] graphIds=null;
        int [] picIds=null;
        Button btnDetails=null;
        Button btnStatHour=null;
        Button btnStatDay=null;
        eu.liebrand.pipemonitor.BarChartView graph=null;
        switch (whichServer) {
            case 1:
                buttonIds=new int[] {R.id.btndetail1, R.id.btnstathour1, R.id.btnstatday1};
                infoIds=new int[] {R.id.valserverinfo1a, R.id.valserverinfo1b, R.id.valserverinfo1c, R.id.valserverinfo1d};
                graphIds=new int[] {R.id.graph1};
                picIds=new int[] { R.id.iconserver1left, R.id.iconserver1right, R.id.anline1, R.id.animgroup1 };
                btnDetails=(Button)findViewById(R.id.btndetail1);
                btnStatHour=(Button)findViewById(R.id.btnstathour1);
                btnStatDay=(Button)findViewById(R.id.btnstatday1);
                graph=(eu.liebrand.pipemonitor.BarChartView)findViewById(R.id.graph1);
                show1=whatToShow;
                break;
            case 2:
                buttonIds=new int[] {R.id.btndetail2, R.id.btnstathour2, R.id.btnstatday2};
                infoIds=new int[] {R.id.valserverinfo2a, R.id.valserverinfo2b, R.id.valserverinfo2c, R.id.valserverinfo2d};
                graphIds=new int[] {R.id.graph2};
                picIds=new int[] { R.id.iconserver2left, R.id.iconserver2right, R.id.anline2, R.id.animgroup2 };
                btnDetails=(Button)findViewById(R.id.btndetail2);
                btnStatHour=(Button)findViewById(R.id.btnstathour2);
                btnStatDay=(Button)findViewById(R.id.btnstatday2);
                graph=(eu.liebrand.pipemonitor.BarChartView)findViewById(R.id.graph2);
                show2=whatToShow;
                break;
        }
        int [][] toShow=null;
        int [][] toHide=null;
        switch (whatToShow) {
            case Nothing:
                toHide=new int[4][];
                toHide[0]=buttonIds;
                toHide[1]=infoIds;
                toHide[2]=graphIds;
                toHide[3]=picIds;
                break;
            case Buttons:
                toShow=new int[2][];
                toShow[0]=buttonIds;
                toShow[1]=picIds;
                toHide=new int[2][];
                toHide[0]=infoIds;
                toHide[1]=graphIds;
                btnDetails.setText(getResources().getString(R.string.detailsShow));
                btnStatHour.setText(getResources().getString(R.string.lasthour));
                btnStatDay.setText(getResources().getString(R.string.lastday));
                break;
            case Details:
                toShow=new int[1][];
                toShow[0]=infoIds;
                toHide=new int[1][];
                toHide[0]=graphIds;
                btnDetails.setText(getResources().getString(R.string.detailsHide));
                btnStatHour.setText(getResources().getString(R.string.lasthour));
                btnStatDay.setText(getResources().getString(R.string.lastday));
                break;
            case HourStat:
                toShow=new int[1][];
                toShow[0]=graphIds;
                toHide=new int[1][];
                toHide[0]=infoIds;
                btnDetails.setText(getResources().getString(R.string.detailsShow));
                btnStatHour.setText(getResources().getString(R.string.lasthide));
                btnStatDay.setText(getResources().getString(R.string.lastday));
                break;
            case DayStat:
                toShow=new int[1][];
                toShow[0]=graphIds;
                toHide=new int[1][];
                toHide[0]=infoIds;
                btnDetails.setText(getResources().getString(R.string.detailsShow));
                btnStatHour.setText(getResources().getString(R.string.lasthour));
                btnStatDay.setText(getResources().getString(R.string.lasthide));
                break;
        }
        if(toShow!=null) {
            for (int[] s : toShow) {
                for (int ss : s) {
                    View v = findViewById(ss);
                    v.setVisibility(View.VISIBLE);
                }
            }
        }
        if(toHide!=null) {
            for (int[] h : toHide) {
                for (int hh : h) {
                    View v = findViewById(hh);
                    v.setVisibility(View.GONE);
                }
            }
        }
        btnDetails.setOnClickListener(this);
        btnStatHour.setOnClickListener(this);
        btnStatDay.setOnClickListener(this);
        graph.setOnClickListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        myReceiver = new MyReceiver();
        registerReceiver(myReceiver, new IntentFilter(MainActivity.INTENT_STATUS));
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
            if (intent.getAction().equals(INTENT_STATUS)) {
                updateUI(intent, bundle.getInt("nextStep"), bundle);
            }
        }

    }
}
