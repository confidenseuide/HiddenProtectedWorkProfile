package background.work.around;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class MainActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
        try {
        Context appContext = getApplicationContext();
        Intent serviceIntent = new Intent(appContext, RiderService.class);
        appContext.startForegroundService(serviceIntent);             
        } catch (Throwable t) {}
        finish();
    }
}
