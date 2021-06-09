/**
 * Copyright 2018-2021 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.computervision;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import android.util.Log;

/**
 * This class creates a TCP socket connection to a server, allowing byte streams to be
 * sent and received.
 */
public class SocketClientTcp {

    private static final String TAG = "SocketClientTcp";
    public String mHost;
    private int mPort;
    private OnMessageReceived mMessageListener;
    private boolean mRunning = false;

    private OutputStream mOutputStream;
    private InputStream mInputStream;

    /**
     * Creates a client which has a TCP socket connection to a server.
     *
     * @param host  The server to connect to.
     * @param port  The server's port number.
     * @param listener  Handles messages received from the server.
     */
    public SocketClientTcp(String host, int port, final OnMessageReceived listener) {
        mMessageListener = listener;
        mHost = host;
        mPort = port;
    }

    /**
     * Writes a stream of bytes over the socket.
     *
     * @param bytes  The byte array to write.
     */
    public void write(byte[] bytes) {
        Log.d(TAG, "write "+bytes.length + " bytes");
        try {
            int packageSize = 8192;
            int remainingCount = bytes.length;
            ByteBuffer writeBuf = ByteBuffer.allocate(packageSize);
            int index = 0;
            while (remainingCount > 0) {
                int realWriteLength = Math.min(packageSize, remainingCount);
                writeBuf.clear();
                writeBuf.rewind();
                writeBuf.put(bytes, index, realWriteLength);
                writeBuf.flip();
                byte[] writeArr = new byte[realWriteLength];
                writeBuf.get(writeArr);
                mOutputStream.write(writeArr);
                mOutputStream.flush();
                index += realWriteLength;
                remainingCount -= realWriteLength;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopClient(){
        mRunning = false;
    }

    /**
     * Creates the socket connection to the server, then continually waits for incoming messages
     * and sends them to the listener.
     */
    public void run() {
        mRunning = true;
        try {
            Log.i(TAG, "Connecting to "+ mHost +":"+mPort+"...");
            //create a socket to make the connection with the server
            Socket socket = new Socket(mHost, mPort);
            mOutputStream = socket.getOutputStream();
            mInputStream = socket.getInputStream();

            try {
                while (mRunning) {
                    //First 4 bytes is the length of the data to be received
                    byte[] bytes = new byte[4];
                    int numBytesRead = mInputStream.read(bytes);
                    if(numBytesRead == -1) {
                        throw new RuntimeException("Could not get length of buffer");
                    }
                    int length = ByteBuffer.wrap(bytes).getInt();
                    byte[] bufArray = new byte[length];
                    numBytesRead = mInputStream.read(bufArray);
                    String serverMessage = new String(bufArray, Charset.forName("utf-8"));
                    Log.d(TAG, "Read "+numBytesRead+" bytes. Received Message: '" + serverMessage + "'");
                    if (mMessageListener != null) {
                        mMessageListener.messageReceived(serverMessage);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                //after it is closed, which means a new socket instance has to be created.
                Log.i(TAG, "Closing socket "+socket);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Encodes the byte stream into the format required by the computer vision server, and sends
     * it over the socket.
     *
     * @param opcode  Opcode to send to the server.
     * @param bytes  The raw bytes of the image to be sent.
     */
    public void send(int opcode, byte[] bytes) {
        //Build the byte array according to the server's parsing rules
        //package header fixed length + opcode length + payload length
        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + bytes.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(opcode);
        bb.putInt(bytes.length);
        bb.put(bytes);
        write(bb.array());
    }

    /**
     * The interface for receiving messages from the server.
     */
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}