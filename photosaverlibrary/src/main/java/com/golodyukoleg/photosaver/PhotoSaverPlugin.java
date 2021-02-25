package com.golodyukoleg.photosaver;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// not currently used
interface INativePhotoSave
{
    String saveSync(String p);
    void saveAsync(String p);
}

public class PhotoSaverPlugin implements INativePhotoSave {

    private static final String TAG = "NativePhotoSave";

    //name of the GameObject in Unity that will receive callback
    private static final String GAME_OBJECT_NAME = "PluginBridge";

    private Activity activity;


    // plugin initialization with unity activity to have the context later on
    public PhotoSaverPlugin(Activity activity)
    {
        this.activity = activity;
        Log.d(TAG, "Initialized PhotoSaverPlugin class");
    }

    //not currently used
    @Override
    public String saveSync(String p) {
        galleryAddPic(p);
        return "scanned sync";
    }

    //not currently used
    @Override
    public void saveAsync(String p) {
        try {
            Log.d(TAG, "async saving");
            // Assuming these calculations results required async methods
            galleryAddPic(p);
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "HandleAsyncSave", "success");
        } catch (Exception exception) {
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "HandleException", exception.toString());
        }
    }


    //the method that is called from Unity
    private void saveImage(byte[] data, String filename, String album) throws IOException {
        boolean saved;
        OutputStream fos;
        String path = "";


        //if we are on Android 10 or later we use ContentResolver to initialize OutputStream
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = activity.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + album);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);

            //remember image path to use it later in the gallery scanning process
            path = imageUri.getPath();
        } else {

            if(activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Need to request WRITE_EXTERNAL_STORAGE");
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
                return;
            }

            //otherwise we do it the old way

            //put together dir path
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + album;


            File file = new File(imagesDir);

            //create directory
            if (!file.exists()) {
                file.mkdir();
            }

            //create file
            File image = new File(imagesDir, filename + ".jpg");
            path = image.getPath();

            //initialize OutputStream
            fos = new FileOutputStream(image);

        }

        //if all good then write and close stream
        if(fos != null) {
            fos.write(data);

            fos.flush();
            fos.close();
        }

        //try to force gallery to refresh itself to show new image
        //report the result to Unity
        try {
            Log.d(TAG, "scanning to CameraRoll: " + path);
            // Assuming these calculations results required async methods
            galleryAddPic(path);
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "HandleAsyncSave", "success");
        } catch (Exception exception) {
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "HandleException", exception.toString());
        }
    }

    private void galleryAddPic(String currentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        activity.sendBroadcast(mediaScanIntent);
    }
}
