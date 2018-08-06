package com.akado.wheelview.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.akado.wheelview.WheelView;

public class MainActivity extends AppCompatActivity {

    WheelView wheelView;

    private String[] items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        items = new String[]{
                "Text 1",
                "Text 2",
                "Text 3",
                "Text 4",
                "Text 5",
                "Text 6",
                "Text 7",
                "Text 8",
                "Text 9",
                "Text 10",
                "Text 11",
                "Text 12",
                "Text 13",
                "Text 14",
                "Text 15",
                "Text 16",
                "Text 17",
                "Text 18",
                "Text 19",
                "Text 20"
        };

        wheelView = findViewById(R.id.wheelView);
        wheelView.setAdapter(new WheelViewAdapter());
    }

    class WheelViewAdapter implements WheelView.Adapter {

        @Override
        public int getItemsCount() {
            return items.length;
        }

        @Override
        public String getItem(int index) {
            return items[index];
        }
    }
}
