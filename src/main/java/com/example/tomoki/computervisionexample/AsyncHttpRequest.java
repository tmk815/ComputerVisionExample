package com.example.tomoki.computervisionexample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import static android.content.ContentValues.TAG;

/**
 * Created by tomoki on 2017/10/28.
 */

public class AsyncHttpRequest extends AsyncTask<Uri.Builder, Void, String> {

    private Activity mainActivity;
    private String ImageUrl, api, jsonText = "";
    private Bitmap bmp;

    public AsyncHttpRequest(Activity activity) {

        // 呼び出し元のアクティビティ
        this.mainActivity = activity;

        EditText eUrl = mainActivity.findViewById(R.id.url);
        EditText eApi = mainActivity.findViewById(R.id.api);
        ImageUrl = eUrl.getText().toString();
        api = eApi.getText().toString();

    }

    //ここが非同期で実行される
    @Override
    protected String doInBackground(Uri.Builder... builder) {
        final String json = "{" +
                "\"url\":\"" + ImageUrl + "\"" +
                "}";
        try {
            //URLから画像を取得
            bmp = downloadImage(ImageUrl);

            String buffer = "";
            HttpURLConnection con = null;
            URL url = new URL("https://westus.api.cognitive.microsoft.com/vision/v1.0/ocr?language=unk&detectOrientation=true");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setInstanceFollowRedirects(false);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("Ocp-Apim-Subscription-Key", api);
            OutputStream os = con.getOutputStream();
            PrintStream ps = new PrintStream(os);
            ps.print(json);
            Log.d("画像のURL", ImageUrl);
            ps.close();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            buffer = reader.readLine();

            JSONObject rootObject = new JSONObject(buffer);
            JSONArray regionArray = rootObject.getJSONArray("regions");
            for (int i = 0; i < regionArray.length(); i++) {
                JSONObject number = regionArray.getJSONObject(i);
                JSONArray linesArray = number.getJSONArray("lines");
                for (int j = 0; j < linesArray.length(); j++) {
                    JSONObject linesNumber = linesArray.getJSONObject(j);
                    JSONArray wordsArray = linesNumber.getJSONArray("words");
                    for (int k = 0; k < wordsArray.length(); k++) {
                        JSONObject[] textObject = new JSONObject[wordsArray.length()];
                        textObject[k] = wordsArray.getJSONObject(k);
                        jsonText += textObject[k].getString("text");
                    }
                    Log.d("HTTP REQ", jsonText);
                    jsonText += "\n";
                }
            }


            con.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d("MalformedURLException", e.toString());
        } catch (ProtocolException e) {
            e.printStackTrace();
            Log.d("ProtocolException", e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("IOException", e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Exception", e.toString());
        }
        return jsonText;
    }

    private Bitmap downloadImage(String address) {
        Bitmap bmp = null;

        try {
            URL url = new URL(address);
            // インスタンス生成
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            // タイムアウト設定
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(20000);
            // リクエストメソッド
            urlConnection.setRequestMethod("GET");
            // リダイレクトを自動で許可しない設定
            urlConnection.setInstanceFollowRedirects(false);
            // ヘッダーの設定
            urlConnection.setRequestProperty("Accept-Language", "jp");
            // 接続
            urlConnection.connect();

            int resp = urlConnection.getResponseCode();
            switch (resp) {
                case HttpURLConnection.HTTP_OK:
                    InputStream is = urlConnection.getInputStream();
                    bmp = BitmapFactory.decodeStream(is);
                    is.close();
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.d(TAG, "error");
            e.printStackTrace();
        }
        return bmp;
    }

    // 非同期処理の終わった後に呼び出される
    @Override
    protected void onPostExecute(String result) {
        EditText et = mainActivity.findViewById(R.id.result);
        ImageView imageView = mainActivity.findViewById(R.id.img);
        imageView.setImageBitmap(bmp);
        et.setText(result);
    }
}