package com.image.bhargav.facecatcher;

import android.os.Bundle;
import android.app.Activity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class DrawScreen extends Activity {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://192.168.0.3:3000/");
        } catch (URISyntaxException e) {
            System.out.println("Exception : "+e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_screen);

        mSocket.connect();

        final View touchView = findViewById(R.id.draw_layout);
        final TextView textView = (TextView)findViewById(R.id.coordinates);

        touchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float x = event.getX();
                float y = event.getY();

                textView.setText("Touch coordinates : "+x+","+y);

                attemptSend(x+","+y);

                return true;
            }
        });
    }

    private void attemptSend(String message) {

        if (TextUtils.isEmpty(message)) {
            return;
        }

        mSocket.emit("event", message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
    }

    public void onClearTouch(View view){
        mSocket.emit("clear", "DUMMY");
    }
}
