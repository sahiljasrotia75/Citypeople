package com.citypeople.project.utilities;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtil {

    private static final String TAG = ImageUtil.class.getSimpleName();
    private static final String TEMP_IMAGE_NAME = "tempImage";

    public static Bitmap getBitmapFromUri(Activity activity, String compressedImagePath) {
        if (compressedImagePath != null && !compressedImagePath.isEmpty())
        {
            try {
                File file = new File(compressedImagePath);
                if (!file.exists())
                    file.mkdir();
                return MediaStore.Images.Media.getBitmap(activity.getContentResolver(), Uri.fromFile(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "Panditkart Gallery");
        if (!file.exists()) {
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + "Compressed.jpg");

    }

    public static String getPDFFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "Panditkart PDFs");
        if (!file.exists()) {
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + "Compressed.pdf");

    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    private static File getTempFile(Context context) {
        File imageFile = new File(context.getExternalCacheDir(), TEMP_IMAGE_NAME);
        imageFile.getParentFile().mkdirs();
        return imageFile;
    }

    public static Bitmap centerCropBitmap(Bitmap bitmap) {
        Bitmap result;
        if (bitmap.getWidth() >= bitmap.getHeight()){
            result = Bitmap.createBitmap(
                    bitmap,
                    bitmap.getWidth()/2 - bitmap.getHeight()/2,
                    0,
                    bitmap.getHeight(),
                    bitmap.getHeight()
            );

        }else{
            result = Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.getHeight()/2 - bitmap.getWidth()/2,
                    bitmap.getWidth(),
                    bitmap.getWidth()
            );
        }
        return result;
    }

    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        //String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        ContentResolver contentResolver = inContext.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,"image.jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg , image/jpg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+File.separator+"Panditkart Images");

        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        return uri;//Uri.parse(path);
    }

    public static String getRealPathFromURI(Activity activity, Uri uri) {
        //Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
        /*if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            cursor.close();
            return cursor.getString(idx);
        }*/
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        // deprecated:
        // Cursor cursor = managedQuery(uri, projection, null, null, null);

        if (cursor != null) {

            int columnIndex = 0;
            try {
                columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            } catch (IllegalArgumentException e) {
                Log.e(ImageUtil.TAG, "While getting path for file "+ e);
            } finally {
                try {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                    cursor = null;
                } catch (Exception e) {
                    Log.e(ImageUtil.TAG, "While closing cursor "+ e);
                }
            }
        }
        return "";
    }

    public static Bitmap compressBitmap(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if (bitmap != null && bitmap.compress(Bitmap.CompressFormat.PNG, 60, out)) {
            bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
            byte[] imageInByte = out.toByteArray();
            long lengthbmp = imageInByte.length;
            Log.e(TAG, "compressBitmap: size"+(lengthbmp/1024.0));
        }
        return bitmap;
    }

    public static void calculateImageSize(Context context, Bitmap bitmap) {
        if (bitmap == null)
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageInByte = stream.toByteArray();
        long lengthbmp = imageInByte.length;

        Log.e("image size: ", ""+(lengthbmp/(1024*2)));
    }
}
