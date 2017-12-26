package com.example.andrew.ml2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //random uuid.
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static List<Byte> encodedBytes;
    private static List<Byte> SampleCounter;
    private static List<Byte> TempVal;
    private static List<Byte> MicInVal;
    private static List<Byte> MicExVal;
    private static List<Byte> ECGVal;
    private static List<Byte> ECGVal2;
    private static List<Byte> OxRedVal;
    private static List<Byte> OxRedVal2;
    private static List<Byte> OxIRVal;
    private static List<Byte> OxIRVal2;
    BluetoothSocket socket;
    private static int byteCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView mTextView = (TextView) findViewById(R.id.textView); //creating window
        BluetoothDevice mBluetoothDevice;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //cannot create a bluetooth connection
            Toast.makeText(MainActivity.this, "No Bt", Toast.LENGTH_LONG).show();
        }
        //mac address of device - already known
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice("00:06:66:D7:F6:8D");
        try {
            socket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
        }

        while (!socket.isConnected()) { //tries to connect until it does
            try {
                System.out.println();
                socket.connect();
                System.out.println("Connecting");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("failed to connect");
            }
        }

        //we have connected so now we take data and continuously add to this list to be analyzed
        encodedBytes = Collections.synchronizedList(new LinkedList<Byte>());
        SampleCounter = new LinkedList<Byte>();
        TempVal = new LinkedList<Byte>();
        MicInVal= new LinkedList<Byte>();
        MicExVal= new LinkedList<Byte>();
        ECGVal= new LinkedList<Byte>();
        ECGVal2= new LinkedList<Byte>();
        
        System.out.println("Connected");

        //producer thread.
        Thread bt = new Thread(new Runnable() {
           public void run() {
               try { //while there is input data coming from socket, add it to encodedBytes
                   while (socket.isConnected()) {
                       synchronized (encodedBytes) {
                           InputStream input = socket.getInputStream();
                           int next = input.read();
                           if (next >= 0) {
                               Byte n = (byte) next;
                               encodedBytes.add(n);
                               if (encodedBytes.size() >= 2) {
                                   encodedBytes.notify();
                               }
                           }
                       }
                   }
                   socket.close();
               } catch (IOException e) { e.printStackTrace(); }
           }
        });

        Thread a = new Thread(new Runnable() {
           public void run() {
               boolean begin = false;
               String hexString = "";
               byte curr = 0; //current byte
               byte prev; //previous byte
               try {
                   while (socket.isConnected()) {
                       synchronized (encodedBytes) {
                           //wait until there is something to read in encodedBytes.
                           while (encodedBytes.size() == 0) {
                               encodedBytes.wait();
                           }
                           prev = curr;
                           curr = encodedBytes.remove(0);
                           //remove values until first 0066 is reached. only happens the first time.
                           if(byteToHex(prev).equals("00") && byteToHex(curr).equals("66") && !begin) {
                               begin = true;
                               hexString = "";
                               byteCount = 0;
                           }

                           if (begin) {
                               hexString += byteToHex(curr);

                               //add here
                               add(curr);
                               byteCount++;

                               if (byteCount == 24) {
                                   byteCount = 0;
                                   System.out.println(hexString);
                                   hexString = "";
                               }
                           }
                       }
                   }
               } catch (InterruptedException e) { e.printStackTrace(); }
           }
        });

        bt.start();
        a.start();
//        try {
//            b.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }

    //Converts a single byte to hex.
    private static String byteToHex(byte n) {
        char[] hexChars = new char[2];
        int v = n & 0xFF;
        hexChars[1] = hexArray[v >>> 4]; //Since the bytes are sent in reverse, the lower bit value is the larger value.
        hexChars[0] = hexArray[v & 0x0F];
        return new String(hexChars);
    }
    //adds a byte to the correct list. These bytes have NOT had their bits swapped.
    private static void add(byte curr) {
        if (byteCount >= 2 && byteCount < 4) {
            SampleCounter.add(curr);
        } else if(byteCount >= 4 && byteCount < 6) {
            TempVal.add(curr);
        } else if(byteCount >= 6 && byteCount < 8) {
            MicInVal.add(curr);
        } else if(byteCount >= 8 && byteCount < 10) {
            MicExVal.add(curr);
        } else if(byteCount >= 10 && byteCount < 12) {
            ECGVal.add(curr);
        } else if(byteCount >= 12 && byteCount < 14) {
            ECGVal2.add(curr);
        } else if(byteCount >= 14 && byteCount < 16) {
            OxRedVal.add(curr);
        } else if(byteCount >= 16 && byteCount < 18) {
            OxRedVal2.add(curr);
        } else if(byteCount >= 18 && byteCount < 20) {
            OxIRVal.add(curr);
        } else if(byteCount >= 20 && byteCount < 22) {
            OxIRVal2.add(curr);
        }
    }
}