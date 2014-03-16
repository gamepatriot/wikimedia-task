package com.peteschmitz.wikimediatask.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.GridView;

import com.peteschmitz.wikimediatask.android.R;
import com.peteschmitz.wikimediatask.android.adapter.ArticleAdapter;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridView = (GridView) findViewById(R.id.grid_view);
        ArticleAdapter adapter = new ArticleAdapter(
                this,
                getResources().getStringArray(R.array.us_presidents_array)
        );
        gridView.setAdapter(adapter);
    }

}
