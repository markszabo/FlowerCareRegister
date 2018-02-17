# Flower Care Register

Android app to register the Chinese versions of the Flower Care Smart Monitor (aka Xiaomi Flower Monitor Tool) outside China.

![Picture of the device from the manufacturer's site](http://img.site.huahuacaocao.net/production/production_17.jpg)

This app can help you if you are faced with this error:

<img src="/screenshots/official_app_error1.png?raw=true" width="45%"/> <img src="/screenshots/official_app_error2.png?raw=true" width="45%"/>

The background of this error is the fact, that the manufacturer (huahuacaocao) produces the Flower Monitor in two versions, one for the domestic, Chinese market and one for the International market. However numerous online shops (aliexpress, dx.com etc.) sell the Chinese version worldwide, but if one tries to register it from outside China the app shows the above error message. The location of the user is checked based on their IP, so fake GPS alone is not enough, one needs to use Chinese proxy or VPN to have a Chinese IP. However the location is only checked when the device is added. Once it's added it can be used everywhere. So this application helps you add the device to your account, so that you can use it with the official "Flower Care" app.

Fun fact: the main difference between the Chinese and the International version is that their MAC address is from a different range. The Chinese versions starts with C4:7C:8D while the International version starts with 88:0F:10.

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
