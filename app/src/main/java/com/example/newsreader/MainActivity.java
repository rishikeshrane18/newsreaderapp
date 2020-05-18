package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String>titles = new ArrayList<>();
    ArrayList<String>content = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });

        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER,title VARChAR,content VARCHAR)");

        updatelistView();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }



    }
    public void updatelistView(){

        Cursor cursor = articleDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex = cursor.getColumnIndex("content");
        int titleIndex = cursor.getColumnIndex("title");

        if (cursor.moveToFirst()){
            titles.clear();
            content.clear();

            do {
                titles.add(cursor.getString(titleIndex));
                content.add(cursor.getString(contentIndex));
            }while (cursor.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }

    }
    public class DownloadTask extends AsyncTask<String,Void,String>{


        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection;



            try {
                url= new URL(strings[0]);
                urlConnection =(HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = reader.read();

                while (data!=-1){
                    char current = (char) data;
                    result += current;
                    data = reader.read();



                }
                Log.i("URL-CONTENT",result);
                JSONArray jsonArray = new JSONArray(result);
                int noofitems = 20;
                if (jsonArray.length() < 20){
                    noofitems = jsonArray.length();

                }
                articleDB.execSQL("DELETE FROM articles");
                for (int i=0;i<noofitems;i++){

                    String articleId = jsonArray.getString(i);
                    url = new URL("https:/hacker-news.firebaseio.com/v0/item/"+articleId+"json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);
                    data = reader.read();
                    String articleInfo = "";
                    while (data!= -1){
                        char current = (char) data;
                        articleInfo += data;
                        data = reader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title")&& !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        url = new URL(articleUrl);
                        urlConnection= (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);
                        data = reader.read();
                        String articleContent = "";
                        while (data!=-1){
                            char current = (char) data;
                            articleInfo+=current;
                            data = reader.read();
                        }
                        Log.i("article-content",articleContent);
                        String sql ="INSERT INTO articles(articleId,title,content) VALUES (?,?,?)";
                        SQLiteStatement statement = articleDB.compileStatement(sql);

                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleId);

                    }

                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updatelistView();
        }
    }

}
