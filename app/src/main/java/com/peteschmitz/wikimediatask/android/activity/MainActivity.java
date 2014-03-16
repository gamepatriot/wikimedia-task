package com.peteschmitz.wikimediatask.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.peteschmitz.wikimediatask.android.R;
import com.peteschmitz.wikimediatask.android.adapter.ArticleAdapter;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridView = (GridView) findViewById(R.id.grid_view);
        ArticleAdapter adapter = new ArticleAdapter(
                this,
                getResources().getStringArray(R.array.us_presidents_names)
        );
        SwingBottomInAnimationAdapter swingAdapter = new SwingBottomInAnimationAdapter(adapter, 100);
        swingAdapter.setAbsListView(gridView);

        gridView.setAdapter(swingAdapter);
        //gridView.setAdapter(adapter);
    }

}
