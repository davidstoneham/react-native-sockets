package com.stonem.sockets;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;

import android.util.Log;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.nio.charset.Charset;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.UnknownHostException;

/**
 * Created by David Stoneham on 2017-08-03.
 */
public class SocketServer {
    public ServerSocket serverSocket;

    private final String eTag = "REACT-NATIVE-SOCKETS";
    private final String event_closed = "socketServer_closed";
    private final String event_data = "socketServer_data";
    private final String event_error = "socketServer_error";
    private final String event_connect = "socketServer_connected";
    private final String event_clientConnect = "socketServer_clientConnected";
    private final String event_clientDisconnect = "socketServer_clientDisconnected";
    private SparseArray<Object> mClients = new SparseArray<Object>();
    private int socketServerPORT;
    private ReactContext mReactContext;
    private boolean isOpen = false;
    private final byte EOT = 0x04;

    public SocketServer(int port, ReactContext reactContext) {
        mReactContext = reactContext;
        socketServerPORT = port;
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public void close() {
        try {
            if (serverSocket != null) {
                isOpen = false;
                for (int i = 0; i < mClients.size(); i++) {
                    int key = mClients.keyAt(i);
                    Object socket = mClients.get(key);
                    if (socket != null && socket instanceof Socket) {
                        try {
                            ((Socket) socket).close();
                        } catch (IOException e) {
                            handleIOException(e);
                        }
                    }
                }
                serverSocket.close();
                serverSocket = null;
                Log.d(eTag, "server closed");
            }
        } catch (IOException e) {
            handleIOException(e);
        }
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    public void onDestroy() {
        if (serverSocket != null) {
            close();
        }
    }

    public void write(String message, int cId) {
        new AsyncTask<Object, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Object... params) {
                int cId = (int) params[0];
                String message = (String) params[1];
                Object socket = mClients.get(cId);
                if (socket != null && socket instanceof Socket) {
                    try {
                        OutputStream outputStream = ((Socket) socket).getOutputStream();
                        PrintStream printStream = new PrintStream(outputStream);
                        printStream.print(message + (char) EOT);
                        printStream.flush();
                        outputStream.flush();

                        Log.d(eTag, "server sent message: " + message);
                    } catch (IOException e) {
                        handleIOException(e);
                    }
                }
                return null;
            }

            protected void onPostExecute(Void dummy) {
            }
        }.execute(cId, message);
    }

    private class SocketServerThread extends Thread {
        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(socketServerPORT);

                isOpen = true;
                WritableMap eventParams = Arguments.createMap();
                sendEvent(mReactContext, event_connect, eventParams);

                while (isOpen) {
                    Socket socket = serverSocket.accept();
                    count++;

                    mClients.put(socket.getPort(), socket);

                    eventParams = Arguments.createMap();
                    eventParams.putInt("id", socket.getPort());

                    sendEvent(mReactContext, event_clientConnect, eventParams);

                    Log.d(eTag, "#" + count + " from " + socket.getInetAddress() + ":" + socket.getPort());

                    Thread socketServerReplyThread = new Thread(new SocketServerReplyThread(socket));
                    socketServerReplyThread.start();
                }
            } catch (IOException e) {
                handleIOException(e);
            }
        }

    }

    private class SocketServerReplyThread extends Thread {
        private Socket hostThreadSocket;
        private int cId;
        private boolean clientConnected = true;

        SocketServerReplyThread(Socket socket) {
            hostThreadSocket = socket;
            cId = hostThreadSocket.getPort();
        }

        @Override
        public void run() {
            try {
                String data = "";
                InputStream inputStream = hostThreadSocket.getInputStream();
                while (isOpen && clientConnected) {
                    int incomingByte = inputStream.read();

                    if (incomingByte == -1) {
                        clientConnected = false;
                        //debug log
                        Log.v(eTag, "Client disconnected");
                        //emit event
                        WritableMap eventParams = Arguments.createMap();
                        eventParams.putInt("client", cId);
                        sendEvent(mReactContext, event_clientDisconnect, eventParams);
                    } else if (incomingByte == EOT) {
                        //debug log
                        Log.d(eTag, "client received message: " + data);
                        //emit event
                        WritableMap eventParams = Arguments.createMap();
                        eventParams.putInt("client", cId);
                        eventParams.putString("data", data);
                        sendEvent(mReactContext, event_data, eventParams);
                        //clear incoming
                        data = "";
                    } else {
                        data += (char) incomingByte;
                    }

                }
            } catch (IOException e) {
                handleIOException(e);
            }
        }

    }

    private void handleIOException(IOException e) {
        //debug log
        Log.e(eTag, "Server IOException", e);
        //emit event
        String message = e.getMessage();
        WritableMap eventParams = Arguments.createMap();
        eventParams.putString("error", message);
        if (message.equals("Socket closed")) {
            sendEvent(mReactContext, event_closed, eventParams);
            isOpen = false;
        } else {
            sendEvent(mReactContext, event_error, eventParams);
        }
    }

}