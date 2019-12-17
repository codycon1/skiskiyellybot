package com.example.ftdi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.*;
import android.util.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ScrollView;

import com.ftdi.j2xx.FT_Device;
import com.ftdi.j2xx.D2xxManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import 	android.text.method.ScrollingMovementMethod;
import in.excogitation.zentone.*;
import in.excogitation.zentone.library.ToneStoppedListener;
import in.excogitation.zentone.library.ZenTone;

import java.io.*;
import java.lang.Object;
import java.lang.StringBuilder;
import java.lang.Math.*;

//Test change XDD

public class MainActivity extends Activity {
    private final static String TAG = "FPGA_FIFO Activity";

    private static D2xxManager ftD2xx = null;
    private FT_Device ftDev;

    static final int READBUF_SIZE  = 256;
    byte[] rbuf  = new byte[READBUF_SIZE];
    char[] rchar = new char[READBUF_SIZE];
    int mReadSize=0;

    TextView tvRead;
    EditText etWrite;
    Button btOpen;
    Button btWrite;
    Button btClose;
    EditText sampleData;

    boolean mThreadIsStopped = true;
    Handler mHandler = new Handler();
    Thread mThread;

    String buf = "";
    String filepath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        filepath = MainActivity.this.getFilesDir() +"//" + "apples";
        File samples = new File(filepath);
        try {
            samples.createNewFile();
        } catch (Exception e) {
            tvRead.append(e.toString());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = (TextView) findViewById(R.id.tvRead);
        textView.setMovementMethod(new ScrollingMovementMethod());

        tvRead = (TextView) findViewById(R.id.tvRead);
        etWrite = (EditText) findViewById(R.id.etWrite);

        btOpen = (Button) findViewById(R.id.btOpen);
        btWrite = (Button) findViewById(R.id.btWrite);
        btClose = (Button) findViewById(R.id.btClose);

        sampleData = (EditText) findViewById(R.id.sampleData);

        updateView(false);

        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e(TAG,ex.toString());
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

    }


    //Sample button for tone generation

    public void onClickAAA(View v){
        ZenTone.getInstance().generate(660, 1, 1.0f, new ToneStoppedListener() {
            @Override
            public void onToneStopped() {
                Boolean isPlaying = false;
            }
        });
    }

    public void sendSample(View v){
        try {
            FileInputStream fis = new FileInputStream (new File(filepath));
            byte[] dataaray = new byte[fis.available()];
            while (fis.read(dataaray) != -1) {
                tvRead.append(new String(dataaray));
            }
            fis.close();
        }
        catch (Exception e) {
            tvRead.append("Exception" + "File write failed: " + e.toString());
        }
    }

    public void onClickOpen(View v) {
        openDevice();
    }


    public void onClickWrite(View v) {
        if(ftDev == null) {
            return;
        }

        synchronized (ftDev) {
            if(ftDev.isOpen() == false) {
                Log.e(TAG, "onClickWrite : Device is not open");
                return;
            }

            ftDev.setLatencyTimer((byte)16);

            String writeString = "s";
            byte[] writeByte = writeString.getBytes();
            ftDev.write(writeByte, writeString.length());
        }
    }

    public void onClickClose(View v) {
        closeDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThreadIsStopped = true;
        unregisterReceiver(mUsbReceiver);
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
*/

    private void openDevice() {
        if(ftDev != null) {
            if(ftDev.isOpen()) {
                if(mThreadIsStopped) {
                    updateView(true);
                    SetConfig(57600, (byte)8, (byte)1, (byte)0, (byte)0);
                    ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                    ftDev.restartInTask();
                    new Thread(mLoop).start();
                }
                return;
            }
        }

        int devCount = 0;
        devCount = ftD2xx.createDeviceInfoList(this);

        Log.d(TAG, "Device number : "+ Integer.toString(devCount));

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xx.getDeviceInfoList(devCount, deviceList);

        if(devCount <= 0) {
            return;
        }

        if(ftDev == null) {
            ftDev = ftD2xx.openByIndex(this, 0);
        } else {
            synchronized (ftDev) {
                ftDev = ftD2xx.openByIndex(this, 0);
            }
        }

        if(ftDev.isOpen()) {
            if(mThreadIsStopped) {
                updateView(true);
                SetConfig(57600, (byte)8, (byte)1, (byte)0, (byte)0);
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
                new Thread(mLoop).start();
            }
        }
    }

    private Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            int i;
            int readSize;
            mThreadIsStopped = false;
            while(true) {
                if(mThreadIsStopped) {
                    break;
                }

/*                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
*/
                synchronized (ftDev) {
                    readSize = ftDev.getQueueStatus();
                    if(readSize>0) {
                        mReadSize = readSize;
                        if(mReadSize > READBUF_SIZE) {
                            mReadSize = READBUF_SIZE;
                        }
                        ftDev.read(rbuf,mReadSize);

                        // cannot use System.arraycopy
                        for(i=0; i<mReadSize; i++) {
                            rchar[i] = (char)rbuf[i];
                        }
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                String x = String.copyValueOf(rchar,0,mReadSize);
                                String[] y = x.split(",",5);
                                writeToFile(x);
                                tvRead.clearComposingText();
                                //tvRead.append(x); // Here's the line that updates the data stream
                                feedback(y);
                            }
                        });

                    } // end of if(readSize>0)
                } // end of synchronized
            }
        }
    };

    public void feedback(String[] input) {
        if(input[4] != null){
            return;
        }
        int[] values = {0,0,0,0,0};
        for(int i = 0; i < 5; i++){
            try{
                double tmp = Integer.parseInt(input[i]);
                values[i] = (int) tmp;
                //tvRead.append(tmp+"\n");
            }
            catch (Exception NumberFormatException){}
        } // Data values are now split CSV style

        //if(values[0] <= 1000){
            ZenTone.getInstance().generate(values[0], 1, 1.0f, new ToneStoppedListener() {
                @Override
                public void onToneStopped() {
                    Boolean isPlaying = false;
                }
            });
        //}
    }
    private void writeToFile(String data) {
        try {
            OutputStream fos = new FileOutputStream(filepath, true);
            fos.write(data.getBytes());
            fos.close();
            //tvRead.append(MainActivity.this.getFilesDir()+"");
        }
        catch (IOException e) {
            tvRead.append("Exception" + "File write failed: " + e.toString());
        }
    }

    private void closeDevice() {
        mThreadIsStopped = true;
        updateView(false);
        if(ftDev != null) {
            ftDev.close();
        }
    }

    private void updateView(boolean on) {
        if(on) {
            btOpen.setEnabled(false);
            btWrite.setEnabled(true);
            btClose.setEnabled(true);
        } else {
            btOpen.setEnabled(true);
            btWrite.setEnabled(false);
            btClose.setEnabled(false);
        }
    }

    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (ftDev.isOpen() == false) {
            Log.e(TAG, "SetConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // TODO : flow ctrl: XOFF/XOM
        // TODO : flow ctrl: XOFF/XOM
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);
    }

    // done when ACTION_USB_DEVICE_ATTACHED
    @Override
    protected void onNewIntent(Intent intent) {
        openDevice();
    };

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // never come here(when attached, go to onNewIntent)
                openDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                closeDevice();
            }
        }
    };

}