package com.example.noizumi.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int IMAGE_INIT = 0;
    private static final int IMAGE_NEXT = 1;
    private static final int IMAGE_PREV = 2;
    private static final int TIMER_DELAY = 2000; // 2秒
    private boolean ButtonMode = true;
    private Button nextButton;
    private Button prevButton;
    private Button playButton;
    Handler handler = new Handler();
    ContentResolver resolver;
    Cursor cursor = null;
    int lastCount = 0;


    // エラーメッセージ表示用警告ダイアログ
    private void showAlertDialog(String mes) {
        // AlertDialog.Builderクラスを使ってAlertDialogの準備をする
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("注意");
        alertDialogBuilder.setMessage(mes);

        // 肯定ボタンに表示される文字列、押したときのリスナーを設定する
        alertDialogBuilder.setPositiveButton("OK",null);

        // AlertDialogを作成して表示する
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void handlerReset() {
        playButton.setText("再生");
        handler.removeCallbacksAndMessages(null);
        ButtonMode = true;
        nextButton.setEnabled(ButtonMode);
        prevButton.setEnabled(ButtonMode);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nextButton = (Button) findViewById(R.id.nextButton);
        prevButton = (Button) findViewById(R.id.prevButton);
        playButton = (Button) findViewById(R.id.playButton);
        lastCount = getImageInfo();

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo(IMAGE_INIT);
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo(IMAGE_INIT);
        }

        // 「進む」ボタンの押下
        View.OnClickListener nextButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentsInfo(IMAGE_NEXT);
            }
        };
        nextButton.setOnClickListener(nextButtonClickListener);

        // 「戻る」ボタンの押下
        View.OnClickListener prevButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentsInfo(IMAGE_PREV);
            }
        };
        prevButton.setOnClickListener(prevButtonClickListener);

        // 「再生/停止」ボタンの押下
        View.OnClickListener playButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!ButtonMode) {
                    playButton.setText("再生");
                    handler.removeCallbacksAndMessages(null);
                }else{
                    handler.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            handler.postDelayed(this, TIMER_DELAY);
                            getContentsInfo(IMAGE_NEXT);
                        }
                    },TIMER_DELAY);
                    playButton.setText("停止");
                }
                ButtonMode = !ButtonMode;
                nextButton.setEnabled(ButtonMode);
                prevButton.setEnabled(ButtonMode);
            }
        };
        playButton.setOnClickListener(playButtonClickListener);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo(IMAGE_INIT);
                }
                break;
            default:
                break;
        }
    }

    private int getImageInfo(){
        Cursor checkGallery;
        // 画像の情報を取得する
        resolver = getContentResolver();
        checkGallery = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );
        if(cursor == null){
            cursor = checkGallery;
        }else if(checkGallery.getCount() != lastCount)  {
            cursor.close();
            cursor = checkGallery;
        }
        return cursor.getCount();
    }

    private void viewImage(){
        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

        ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
        imageVIew.setImageURI(imageUri);
    }

    private boolean getContentsInfo(int mode) {
        int count;

        count = getImageInfo();

        if(count != lastCount){
            handlerReset();
            if(count == 0) {
                showAlertDialog("画像がありません。");
                ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
                imageVIew.setImageDrawable(null);
            }else{
                showAlertDialog("ギャラリーの内容が変更されました。\n処理を初期化します。");
                if(cursor.moveToFirst()){
                    viewImage();
                }
            }
            lastCount = count;
            return false;
        }

        if(mode == IMAGE_INIT) {
            if(cursor.moveToFirst()){
                viewImage();
            }
        }

        if(mode == IMAGE_NEXT) {
            if(cursor.isLast()) {
                if(cursor.moveToFirst()){
                    viewImage();
                }
            }else{
                if (cursor.moveToNext()) {
                    viewImage();
                }

            }
        }
        if(mode == IMAGE_PREV) {
            if(cursor.isFirst()) {
                if(cursor.moveToLast()){
                    viewImage();
                }
            }else{
                if (cursor.moveToPrevious()) {
                    viewImage();
                }

            }
        }
        return true;
    }
}