package dsi.darshan.cardea;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Highlight;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity implements OnChartValueSelectedListener {

    BluetoothAdapter mBluetoothAdapter;
    int BlREQUEST_ENABLE_BT = 0;
    Set<BluetoothDevice> pairedDevices;
    UUID uuid;
    ArrayAdapter mArrayAdapter;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();
    ConnectThread connectThread;
    ConnectedThread connectedThread;
    static final int MESSAGE_READ = 123454;
    public static BluetoothDevice mBluetoothDevice;
    LineChart mChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (checkForBluetoothCompitablity()) {
            enableBluetooth();
        }
        getPairedDevices();
        configureGraph();
    }

    private void configureBluetooth() {
        new AlertDialog.Builder(this)
                .setTitle("Select a device..")
                .setSingleChoiceItems(mArrayAdapter, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothDevice = devices.get(which);
                        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                        Log.d("RAJ", "" + mBluetoothDevice.getAddress());
                        mBluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress());
                        setPin("1234");
                        connectThread = new ConnectThread(mBluetoothDevice, "A");
                        connectThread.start();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();

    }

    private void configureGraph() {
        mChart = (LineChart) findViewById(R.id.chart);
        mChart.setDrawGridBackground(true);
        mChart.setOnChartValueSelectedListener(this);

        // no description text
        mChart.setDescription("");
        mChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable value highlighting
        mChart.setHighlightEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.GRAY);

        LineData data = new LineData();
        data.setDrawValues(false);
//        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();


        // modify the legend ...
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaxValue(250f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    private void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BlREQUEST_ENABLE_BT);
        }
    }

    private void getPairedDevices() {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                devices.add(device);
                Log.d("RAJ", "" + device.getName() + "\n" + device.getAddress());
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    private boolean checkForBluetoothCompitablity() {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Device doesn't have bluetooth ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d("RAJ", "onRecieve called");
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                devices.add(device);
                Log.d("RAJ", "" + device.getName() + "\n" + device.getAddress());
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    public void viewVisibleDevices(View view) {
        mBluetoothAdapter.startDiscovery();
        configureBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
            connectThread.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }


    private void addEntry(float az) {

        LineData data = mChart.getData();

        if (data != null) {

            LineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well


            if (set == null) {
                set = createSet();
                set.setDrawCubic(true);
                set.setCubicIntensity(0.2f);
                set.setDrawFilled(false);
                set.setDrawCircles(false);
                set.setLineWidth(3f);
                set.setCircleSize(5f);
                set.setHighLightColor(Color.rgb(244, 117, 117));
                set.setColor(Color.GREEN);
                set.setFillColor(ColorTemplate.getHoloBlue());
                data.addDataSet(set);
            }


            // add a new x-value first
            data.addXValue(Math.round(az) + " ");
            data.addEntry(new Entry(az, set.getEntryCount()), 0);

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRange(12);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getXValCount() - 7);

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);

            // redraw the chart
            // mChart.invalidate();
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setDrawValues(false);
//        set.setValueTextColor(Color.WHITE);
//        set.setValueTextSize(10f);
        return set;
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String message;

        public ConnectThread(BluetoothDevice device, String msg) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            message = msg;
            Log.d("RAJ", "connect thread started");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.d("RAJ", "connect thread constructor exception " + e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Thread.sleep(1000);
                mmSocket.connect();
                Log.d("RAJ", "connected successfully");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connection successful ", Toast.LENGTH_SHORT).show();
                    }
                });

                // Do work to manage the connection (in a separate thread)
                connectedThread = new ConnectedThread(mmSocket);
                for (int i = 0; i < 5; i++) {
                    connectedThread.write(message.getBytes());
                }
                connectedThread.start();
            } catch (IOException e) {
                // Unable to connect; close the socket and get out
                Log.d("RAJ", "connect thread failed");
                Log.d("RAJ", "" + e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                    }
                });
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.d("RAJ", "closing socket exception in connect thread" + e1);

                }
                return;
            } catch (InterruptedException e) {
                Log.d("RAJ", "Exception while sleep " + e);
            }


        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d("RAJ", "cancel connect thread exception " + e);
            }
        }
    }

    ArrayList<String> strings = new ArrayList<>();
    ArrayList<String> stringIndex = new ArrayList<>();

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            Log.d("RAJ", "connected thread started");

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("RAJ", "getting streans exception " + e);

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = null;
            // buffer store for the stream
            int bytes; // bytes returned from read()
            Log.d("RAJ", "connect thread started success");
            int length = 13;
            buffer = new byte[length];
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    int val = buffer[0];
                    Log.d("RAJ", "" + val);
                    addEntry((float) val);

                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.d("RAJ", "reading bytes exception " + e);

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                Log.d("RAJ", "connected started and writing");
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("RAJ", "writing exception " + e);

            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d("RAJ", " close exeption in connected thread " + e);

            }
        }
    }

    private void addDataToServer(int val) {
        {
            strings.add("" + val);
            if (strings.size() == 500) {
                ++i;
                String vals = strings.toString();
                vals = vals.replace('[', ' ');
                vals = vals.replace(']', ' ');
                vals = vals.replaceAll(" ", "");
                sendingData(vals, "" + i);
                strings.clear();
            }
        }
    }

    private void parse(String msg) {
        msg = msg.replace('[', ' ');
        msg = msg.replaceAll(" ", "");
        msg = msg.replace(']', ',');
        int start;
        int end;
        String s;
        for (int i = 0; i < msg.length(); i++) {
            start = 0;
            end = msg.indexOf(',');
            s = msg.substring(start, end);
            msg = msg.substring(end + 1);
            float val = Float.parseFloat(s);
            Log.d("RAJ", "" + val);
            addEntry(val);
        }

        /*int start,end;
        start = msg.indexOf('[')+1;
        end = msg.indexOf(']');
        for(int i=start;i<end;i++){
//            String s = msg.substring();

        }*/

//        addEntry(val);
    }


    static int i = 0;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    int val = readBuf[0];
                    addDataToServer(val);

                    // construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    Log.d("RAJ", "" + msg.obj);


//                    Log.d("RAJ", "" + readMessage);
//                    for (int i = 0; i < readMessage.length(); i++) {
//                        Log.d("RAJ", "" + (float) readMessage.charAt(i));
//                        addEntry((float) readMessage.charAt(i));
//                    }
//                    Log.d("RAJ", "" + readMessage.getBytes());
//                    for (int i = 0; i < readMessage.length(); i++) {
//                        Log.d("RAJ", "" + (int) readMessage.charAt(i));
//                        int j = (int) readMessage.charAt(i);
//                        if (j == 65533) {
//                            j = 0;
//                        }
//                        stringBuilder.append(readMessage);
//                        addEntry((float) j);
//
//                    }
//                    byte[] b = readMessage.getBytes(Charset.forName("UTF-8"));
//                    Log.d("RAJ", "" + b + " ");
//                    for (int i = 0; i < b.length; i++)
//                        Log.d("RAJ", "" + (int) b[i]);


                    break;
            }
        }


    };

    public void stopThread(View view) {
        Toast.makeText(this, "stopping", Toast.LENGTH_SHORT).show();
        try {
            connectedThread.write("S".getBytes());
            connectedThread.cancel();
        } catch (Exception e) {
            Log.d("RAJ", "" + e);
        }
    }

    private void sendingData(String vals, String index) {
        String params[] = {vals, index};
        new sendingTask().execute(params);
    }


    private void setPin(String pinCode) {
        try {
            byte[] ar = pinCode.getBytes();
            Method m2 = mBluetoothDevice.getClass().getMethod("setPin", new Class[]{byte[].class});
            m2.invoke(mBluetoothDevice, ar);
        } catch (NoSuchMethodException e) {
            Log.e("Bluetooth", "Failed to set pin code", e);
        } catch (InvocationTargetException e) {
            Log.d("Bluetooth", "Failed to set pin code", e);
        } catch (IllegalAccessException e) {
            Log.e("Bluetooth", "Failed to set pin code", e);
        }

    }

    public class sendingTask extends AsyncTask<String, Void, Void> {

        String postUrl = "http://vonturing.in/ECG/update.php";
        String ID = "index_ID", data_event = "data_event", data_packet = "data_packet", VALUES = "data_values", TIME = "data_time", id = "2";
        Calendar calendar;
        String vals, index;


        @Override
        protected Void doInBackground(String... params) {
            vals = params[0];
            index = params[1];
            Log.d("RAJ", "background strated");
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(postUrl);
            HttpContext localContext = new BasicHttpContext();
            calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int date = calendar.get(Calendar.DATE);
            int hour = calendar.get(Calendar.HOUR);
            int min = calendar.get(Calendar.MINUTE);
            int sec = calendar.get(Calendar.SECOND);
            String time = "" + year + "-" + month + "-" + date + " " + hour + ":" + min + ":" + sec;
            try {
                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                entity.addPart(ID, new StringBody(id));
                entity.addPart(VALUES, new StringBody(vals));
                entity.addPart(TIME, new StringBody(time));
                entity.addPart(data_packet, new StringBody(index));
                entity.addPart(data_event, new StringBody("1"));
                Log.d("RAJ", "sending: id= " + id + "\n vals = " + vals + "\n time= " + time + " index = " + index);
                httpPost.setEntity(entity);
                HttpResponse response = httpClient.execute(httpPost, localContext);
                HttpEntity entity1 = response.getEntity();
                String resp = EntityUtils.toString(entity1);
                Log.d("RAJ", "resp " + resp);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void addFilter(View view) {
        Toast.makeText(this, "filter added", Toast.LENGTH_SHORT).show();
        try {
            connectedThread.write("F".getBytes());
        } catch (Exception e) {
            Log.d("RAJ", "" + e);
        }
    }

    public void removeFilter(View view) {
        Toast.makeText(this, "filter removed", Toast.LENGTH_SHORT).show();
        try {
            connectedThread.write("X".getBytes());
        } catch (Exception e) {
            Log.d("RAJ", "" + e);
        }
    }

    public void saveFile(View view) {

        File file = new File(Environment.DIRECTORY_PICTURES + File.separator
                + System.currentTimeMillis());
        String path = file.getPath();

        if (mChart.saveToGallery(path, 50)) {
            Toast.makeText(getApplicationContext(), "Saving SUCCESSFUL!",
                    Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                    .show();
    }

}
