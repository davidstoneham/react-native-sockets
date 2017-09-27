package com.stonem.sockets;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;

/**
 * Created by David Stoneham on 2017-08-03.
 */
public class SocketsModule extends ReactContextBaseJavaModule {
    private final String eTag = "REACT-NATIVE-SOCKETS";

    private ReactContext mReactContext;

    SocketServer server;
    SocketClient client;

    public SocketsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
    }

    @Override
    public void onCatalystInstanceDestroy() {
        try {
            new GuardedAsyncTask<Void, Void>(getReactApplicationContext()) {
                @Override
                protected void doInBackgroundGuarded(Void... params) {
                    if (client != null) {
                        client.disconnect(false);
                    }
                    if (server != null) {
                        server.close();
                    }
                }
            }.execute().get();
        } catch (InterruptedException ioe) {
            Log.e(eTag, "onCatalystInstanceDestroy", ioe);
        } catch (ExecutionException ee) {
            Log.e(eTag, "onCatalystInstanceDestroy", ee);
        }
    }

    @ReactMethod
    public void startServer(int port) {
        server = new SocketServer(port, mReactContext);
    }

    @ReactMethod
    public void startClient(ReadableMap params) {
        client = new SocketClient(params, mReactContext);
    }

    @ReactMethod
    public void write(String message) {
        if (client != null) {
            client.write(message);
        }
    }

    @ReactMethod
    public void disconnect() {
        if (client != null) {
            client.disconnect(true);
            client = null;
        }
    }

    @ReactMethod
    public void emit(String message, int clientAddr) {
        if (server != null) {
            server.write(message, clientAddr);
        }
    }

    @ReactMethod
    public void close() {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    @ReactMethod
    public void getIpAddress(Callback successCallback, Callback errorCallback) {
        WritableArray ipList = Arguments.createArray();
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    if (inetAddress.isSiteLocalAddress()) {
                        ipList.pushString(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(eTag, "getIpAddress SocketException", e);
            errorCallback.invoke(e.getMessage());
        }
        successCallback.invoke(ipList);
    }

    @ReactMethod
     public void isServerAvailable(String host, int port, int timeOut, Callback successCallback, Callback errorCallback) {
        final Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), timeOut);
            successCallback.invoke(true);
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (Exception e) {
                }
        }
    }


    @Override
    public String getName() {
        return "Sockets";
    }
}