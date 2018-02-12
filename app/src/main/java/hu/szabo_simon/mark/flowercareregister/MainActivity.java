package hu.szabo_simon.mark.flowercareregister;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonOne = (Button) findViewById(R.id.BtnSearchDevice);
        buttonOne.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), SearchDevicesActivity.class);
                startActivityForResult(i, 1);
            }
        });
    }
}
