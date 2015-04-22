Updating
=======
Updating, Upgrading and Monitoring, right away from your mobile app. It’s about updating and communicating with your app installed on various devices and monitoring app statistics right away from your pocket.

##Setup

**For Eclipse/ADT user**: please see tag [eclipse_project](https://github.com/goupdating/android-eclipse-sdk), download the source codes, check out the Updating to see how the library works.

####To start with

Download the library and add it in your *project* as an *android library*. Once sdk added as a library *copy and paste below line of codes in your desired class or MainActivity*.

```java
import com.android.updating.Updating;
//If you are writing an app with the minimum target set to API level 14 or greater,
//session handling is completely automatic

public class MyActivity extends Activity {
    @Override
    public void onCreate() {
        super.onCreate();
        // init updating
        Updating.init(this, "YOUR_APP_KEY");
        //For app key check your email or login to your account.
    }
}
```

After integrating java code please ensure that your application's manifest(AndroidManifest.xml) allow below permissions.
```html
<uses-permission android:name="android:permission.INTERNET">
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

##Thanks to below libraries and Contributors

#### [James Smith - loopj](http://loopj.com/android-async-http/)

