package protectedwp.safespace;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StairActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Запускаем Main и вычищаем старые задачи, чтобы не было мусора в стеке
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        startActivity(intent);
        
        // Оставляем без finish(), как договаривались — Stair останется фундаментом в стеке
    }
}
