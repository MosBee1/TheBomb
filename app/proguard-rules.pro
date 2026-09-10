-keepattributes SourceFile,LineNumberTable
-keepclassmembers enum io.github.mosbee1.thebomb.data.model.DurationUnit {
    *;
}
EOF cat > app/src/main/AndroidManifest.xml <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application
        android:name=".TheBombApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.TheBomb">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".popup.PopupActivity"
            android:configChanges="orientation|screenSize|screenDensity|keyboardHidden|uiMode"
            android:excludeFromRecents="true"
            android:exported="false"
            android:launchMode="singleTask"
            android:noHistory="true"
            android:showWhenLocked="true"
            android:taskAffinity=""
            android:theme="@style/Theme.TheBomb.Popup"
            android:turnScreenOn="true" />

        <activity
            android:name=".consent.DeletionConsentActivity"
            android:configChanges="orientation|screenSize|screenDensity|keyboardHidden|uiMode"
            android:excludeFromRecents="true"
            android:exported="false"
            android:noHistory="true"
            android:taskAffinity=""
            android:theme="@style/Theme.TheBomb.Popup" />

        <service
            android:name=".service.ScreenshotWatcherService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <receiver
            android:name=".service.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <receiver
            android:name=".notifications.NotificationActionReceiver"
            android:exported="false" />

    </application>

</manifest>
