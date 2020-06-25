package com.golodyukoleg.photosaver;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
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

interface INativePhotoSave
{
    String saveSync(String p);
    void saveAsync(String p);
}

public class PhotoSaverPlugin implements INativePhotoSave {

    private static final String TAG = "NativePhotoSave";
    private static final String GAME_OBJECT_NAME = "PluginBridge";

    private Activity activity;

    public PhotoSaverPlugin(Activity activity)
    {
        this.activity = activity;
        Log.d(TAG, "Initialized PhotoSaverPlugin class");
    }

    @Override
    public String saveSync(String p) {
        galleryAddPic(p);
        return "scanned sync";
    }

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

    private void saveImage(byte[] data, String filename, String album) throws IOException {
        boolean saved;
        OutputStream fos;
        String path = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = activity.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + album);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
            path = imageUri.getPath();
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + album;

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            File image = new File(imagesDir, filename + ".jpg");
            path = image.getPath();
            fos = new FileOutputStream(image);

        }
        fos.write(data);

//        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();

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
