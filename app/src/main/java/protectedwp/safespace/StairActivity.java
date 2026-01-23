package protectedwp.safespace;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StairActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();        
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);        
        startActivity(intent);
            }
}
