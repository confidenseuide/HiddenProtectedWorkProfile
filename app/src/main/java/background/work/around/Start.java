package background.work.around;

import android.content.*;

public class Start {

    public static void RunService(Context context) {
        try{
          
        Intent intent = new Intent(context.getPackageName() + ".START");
        intent.setPackage(context.getPackageName());            
        context.sendBroadcast(intent);
          
        } catch(Throwable t) {}
    }
}
