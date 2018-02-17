# Flower Care Register

Android app to register the Chinese versions of the Flower Care Smart Monitor (aka Xiaomi Flower Monitor Tool) outside China.

![Picture of the device from the manufacturer's site](http://img.site.huahuacaocao.net/production/production_17.jpg)

This app can help you if you are faced with this error:

<img src="/screenshots/official_app_error1.png?raw=true" width="45%"/> <img src="/screenshots/official_app_error2.png?raw=true" width="45%"/>

## The flow of the program

1. Enter the Flower Care username and password (used to sign in to the official application).
2. Search for nearby bluetooth devices
3. Select the Flower Care device
4. The app gets a random Chinese proxy from [gimmeproxy.com](https://gimmeproxy.com/api/getProxy?country=CN&protocol=http)
5. The app sets the proxy and checks its IP with [www.ip-api.com](http://www.ip-api.com/json)
6. The app sends a login request to the [huahuacaocao servers](https://api.huahuacaocao.net/api)
7. On successful login the app registers the device through the proxy

## Permissions

* Bluetooth ("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"): used to scan for Bluetooth devices
* Internet ("android.permission.INTERNET"): used to get the proxy data and contact the [huahuacaocao servers](https://api.huahuacaocao.net/api)

On Android 6.0 and above:
* Location ("android.permission.ACCESS_COARSE_LOCATION"): this permission is required to scan for Bluetooth devices, as described [here](https://stackoverflow.com/a/37015725/8590802) ([Official docs](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner.html#startScan(java.util.List<android.bluetooth.le.ScanFilter>,%20android.bluetooth.le.ScanSettings,%20android.app.PendingIntent))). The app does not access the location directly.
