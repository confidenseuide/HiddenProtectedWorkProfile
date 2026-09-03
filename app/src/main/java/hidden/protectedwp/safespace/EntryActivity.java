package hidden.protectedwp.safespace;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class EntryActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {		
        super.onCreate(b);      
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
	    
}
