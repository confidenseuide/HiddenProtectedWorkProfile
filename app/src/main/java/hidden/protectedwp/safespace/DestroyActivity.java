package hidden.protectedwp.safespace;

import android.app.*;

public class DestroyActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
		
		wipe.wipe(this);	
			
	}
		
		
}
