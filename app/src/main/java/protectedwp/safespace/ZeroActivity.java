package protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.*;

public class ZeroActivity extends Activity {

  @Override
    protected void onResume() {
        super.onResume();        
         this.createDeviceProtectedStorageContext()
            .getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isDone", false)
            .commit();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }
}
