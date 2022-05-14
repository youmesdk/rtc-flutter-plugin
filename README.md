# youme_rtc_engine

Youme RTC SDK Plugin for flutter.

# Usage 
To use this plugin, please add youme_rtc_engine as a dependency to your pubspec.yaml file

# Getting Started
- Get some basic and advanced examples from the example folder.

## Privacy Permission 
Youme RTC Video SDK requires Camera and Microphone permission to start a video call.

### Android
See the required device permissions from the AndroidManifest.xml file.
``` xml
<manifest>
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.BLUETOOTH" />
  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission android:name="android.permission.READ_PRIVILEGED_PHONE_STATE"
    tools:ignore="ProtectedPermissions" />
</manifest>
```

### iOS & macOS

Open the `Info.plist` and add:

- `Privacy - Microphone Usage Description`，and add some description into the Value column.
- `Privacy - Camera Usage Description`, and add some description into the Value column.

