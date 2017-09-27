# react-native-sockets
Java Socket Native Plugin for React Native for Android

This is a basic implementation of Java sockets on Android allowing running a native socket server and client.

Requires RN 0.47 or higher


## Features
* No limit to length of messages sent between client and server
* Client can auto-reconnect on server loss

## Setup
#### Step 1 - NPM Install

```shell
npm install --save react-native-sockets
```
#### Step 2 - Update Gradle Settings

```gradle
// file: android/settings.gradle
...

include ':react-native-sockets'
project(':react-native-sockets').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-sockets/android')
```

#### Step 3 - Update app Gradle Build

```gradle
// file: android/app/build.gradle
...

dependencies {
    ...
    compile project(':react-native-sockets')
}
```

#### Step 4 - MainApplication.java

```java
...
import com.stonem.sockets.SocketsPackage;

...
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          ...
          new SocketsPackage()
          ...
```


## Using The Plugin

### Server

```js
import Sockets from 'react-native-sockets';
...
```
#### Create a socket server
```js
    let port = 8080;
    Sockets.startServer(port);
```

#### Emit message to client
```js
    Sockets.emit("message to client", clientAddr);
```

#### Close server
```js
   Sockets.close();
```

#### Event listeners
```js
    import { DeviceEventEmitter } from 'react-native';

    //on started
     DeviceEventEmitter.addListener('socketServer_connected', () => {
      console.log('socketServer_connected');
    });
    //on error
    DeviceEventEmitter.addListener('socketServer_error', (data) => {
      console.log('socketServer_error',data.error);
    });
    //on client connected
    DeviceEventEmitter.addListener('socketServer_clientConnected', (client) => {
      console.log('socketServer_clientConnected', client.id);
    });
    //on new message
    DeviceEventEmitter.addListener('socketServer_data', (payload) => {
      console.log('socketServer_data message:', payload.data);
      console.log('socketServer_data client id:', payload.client);
    });
    //on server closed
    DeviceEventEmitter.addListener('socketServer_closed', (data) => {
      console.log('socketServer_closed',data.error);
    });
    //on client disconnected
    DeviceEventEmitter.addListener('socketServer_clientDisconnected', (data) => {
      console.log('socketServer_clientDisconnected client id:', data.client);
    });
```

### Client

```js
import Sockets from 'react-native-sockets';
...
```
#### Connect to a socket server
```js
    config={
        address: "192.168.1.1", //ip address of server
        port: 8080, //port of socket server
        reconnect:true, //OPTIONAL (default false): auto-reconnect on lost server
        reconnectDelay:500, //OPTIONAL (default 500ms): how often to try to auto-reconnect
        maxReconnectAttempts:10, //OPTIONAL (default infinity): how many time to attemp to auto-reconnect

    }
     Sockets.startClient(config);
```

#### Send message to server
```js
    Sockets.write("message to server");
```

#### Disconnect client
```js
   Sockets.disconnect();
```

#### Event listeners
```js
    import { DeviceEventEmitter } from 'react-native';

    //on connected
     DeviceEventEmitter.addListener('socketClient_connected', () => {
      console.log('socketClient_connected');
    });
    //on error
    DeviceEventEmitter.addListener('socketClient_error', (data) => {
      console.log('socketClient_error',data.error);
    });
    //on new message
    DeviceEventEmitter.addListener('socketClient_data', (payload) => {
      console.log('socketClient_data message:', payload.data);
    });
    //on client closed
    DeviceEventEmitter.addListener('socketClient_closed', (data) => {
      console.log('socketClient_closed',data.error);
    });
```


### Misc Functions

```js
import Sockets from 'react-native-sockets';
...
```
#### Get device IP address
Returns an array of ip address for the device.
```js
    Sockets.getIpAddress(ipList => {
      console.log('Ip address list', ipList);  
    }, err => {
      console.log('getIpAddress_error', err);
    })
```

#### Check server is available
Checks if a socket server is available for connection on the network
```js
    ipAddress="192.168.1.1";
    port=8080;
    timeout=1000; //milliseconds
    Sockets.isServerAvailable(ipAddress,port,timeout,success => {
        Alert.alert("Socket server is available");
    }, err => {
        Alert.alert("Socket server is not available");
    })
```

## Running the example
An example app is located in the example folder. Just download and run npm install and run it on an android device.
