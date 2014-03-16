package com.peteschmitz.wikimediatask.android.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.GridView;

import com.peteschmitz.wikimediatask.android.R;
import com.peteschmitz.wikimediatask.android.adapter.ArticleAdapter;
import com.peteschmitz.wikimediatask.android.network.WikiArticleImageURLTask;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridView = (GridView) findViewById(R.id.grid_view);
        ArticleAdapter adapter = new ArticleAdapter(this, new String[100]);
        gridView.setAdapter(adapter);

        new WikiArticleImageURLTask(this, "Dog"){
            @Override
            protected void onPostExecute(String s) {
                if (s != null){

                }
            }
        }
                .execute();
    }

}
