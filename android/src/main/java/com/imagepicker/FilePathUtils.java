package com.imagepicker;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import androidx.loader.content.CursorLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class FilePathUtils {

  public static String getAbsolutePath(Context context, Uri imageUri) {
    String absolutePath;
    if (Build.VERSION.SDK_INT < 11) {
      absolutePath = getAbsolutePathFromURI_BelowAPI11(context, imageUri);
    } else if (Build.VERSION.SDK_INT < 19) {
      absolutePath = getAbsolutePathFromURI_API11to18(context, imageUri);
    } else {
      absolutePath = getAbsolutePathFromURI_API19(context, imageUri);
    }
    return absolutePath;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is Google Photos.
   */
  public static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }

  private static boolean isGoogleDocsUri(Uri uri) {
    return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
  }

  private static boolean isOneDriveUri(Uri uri) {
    return "com.microsoft.skydrive.content.StorageAccessProvider".equals(uri.getAuthority());
  }

  private static boolean isBoxUri(Uri uri) {
    return "com.box.android.documents".equals(uri.getAuthority());
  }

  public static boolean isWhatsAppFile(Uri uri) {
    return "com.whatsapp.provider.media".equals(uri.getAuthority());
  }

  public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
    Cursor cursor = null;
    String column = MediaStore.Images.Media.DATA;
    String[] projection = {column};
    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }


  private static String copyFileToCache(Uri uri, Context context) {
    Uri returnUri = uri;
    Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);
    /*
     * Get the column indexes of the data in the Cursor,
     *     * move to the first row in the Cursor, get the data,
     *     * and display it.
     * */
    int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
    int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
    returnCursor.moveToFirst();
    String name = (returnCursor.getString(nameIndex));
    String size = (Long.toString(returnCursor.getLong(sizeIndex)));
    File file = new File(context.getCacheDir(), name);

    try {
      InputStream inputStream = context.getContentResolver().openInputStream(uri);
      FileOutputStream outputStream = new FileOutputStream(file);
      int read = 0;
      int maxBufferSize = 1 * 1024 * 1024;
      int bytesAvailable = inputStream.available();

      //int bufferSize = 1024;
      int bufferSize = Math.min(bytesAvailable, maxBufferSize);

      final byte[] buffers = new byte[bufferSize];
      while ((read = inputStream.read(buffers)) != -1) {
        outputStream.write(buffers, 0, read);
      }
      Log.e("File Size", "Size " + file.length());
      inputStream.close();
      outputStream.close();
      Log.e("File Path", "Path " + file.getPath());
      Log.e("File Size", "Size " + file.length());
    } catch (Exception e) {
      Log.e("Exception", e.getMessage());
    }
    return file.getPath();
  }


  @SuppressLint("NewApi")
  public static String getAbsolutePathFromURI_API11to18(Context context, Uri contentUri) {
    String[] proj = {MediaStore.Images.Media.DATA};
    String result = null;

    CursorLoader cursorLoader = new CursorLoader(context, contentUri, proj, null, null, null);
    Cursor cursor = cursorLoader.loadInBackground();

    if (cursor != null) {
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      result = cursor.getString(column_index);
      cursor.close();
    }
    return result;
  }

  public static String getAbsolutePathFromURI_BelowAPI11(Context context, Uri contentUri) {
    String[] proj = {MediaStore.Images.Media.DATA};
    Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
    int column_index = 0;
    String result = "";
    if (cursor != null) {
      column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      result = cursor.getString(column_index);
      cursor.close();
      return result;
    }
    return result;
  }

  @SuppressLint("NewApi")
  public static String getAbsolutePathFromURI_API19(final Context context, final Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    String selection = null;
    String[] selectionArgs = null;
    // DocumentProvider
    if (isKitKat) {
      // ExternalStorageProvider

      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        String fullPath = getPathFromExtSD(split);
        if (fullPath != "") {
          return fullPath;
        } else {
          return null;
        }
      }
      // DownloadsProvider

      if (isDownloadsDocument(uri)) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          final String id;
          Cursor cursor = null;
          try {
            cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
              String fileName = cursor.getString(0);
              String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
              if (!TextUtils.isEmpty(path)) {
                return path;
              }
            }
          } finally {
            if (cursor != null)
              cursor.close();
          }
          id = DocumentsContract.getDocumentId(uri);
          if (!TextUtils.isEmpty(id)) {
            if (id.startsWith("raw:")) {
              return id.replaceFirst("raw:", "");
            }
            String[] contentUriPrefixesToTry = new String[]{
              "content://downloads/public_downloads",
              "content://downloads/my_downloads"
            };
            for (String contentUriPrefix : contentUriPrefixesToTry) {
              try {
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));


                return getDataColumn(context, contentUri, null, null);
              } catch (NumberFormatException e) {
                //In Android 8 and Android P the id is not a number
                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
              }
            }


          }
        } else {
          Uri contentUri = null;
          final String id = DocumentsContract.getDocumentId(uri);

          if (id.startsWith("raw:")) {
            return id.replaceFirst("raw:", "");
          }
          try {
            contentUri = ContentUris.withAppendedId(
              Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          if (contentUri != null) {

            return getDataColumn(context, contentUri, null, null);
          }
        }
      }


      // MediaProvider
      if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;

        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        selection = "_id=?";
        selectionArgs = new String[]{split[1]};


        return getDataColumn(context, contentUri, selection,
          selectionArgs);
      }

      if (isGoogleDocsUri(uri) || isOneDriveUri(uri) || isBoxUri(uri) || isWhatsAppFile(uri)) {
        return copyFileToCache(uri, context);
      }


      if ("content".equalsIgnoreCase(uri.getScheme())) {

        if (isGooglePhotosUri(uri)) {
          return uri.getLastPathSegment();
        }
        if (isGoogleDocsUri(uri) || isOneDriveUri(uri) || isBoxUri(uri) || isWhatsAppFile(uri)) {
          return copyFileToCache(uri, context);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

          return copyFileToCache(uri, context);
        } else {
          return getDataColumn(context, uri, null, null);
        }

      }
      if ("file".equalsIgnoreCase(uri.getScheme())) {
        return uri.getPath();
      }
    }

    return null;
  }

  private static String getPathFromExtSD(String[] pathData) {
    final String type = pathData[0];
    final String relativePath = "/" + pathData[1];
    String fullPath = "";
    // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
    // something like "71F8-2C0A", some kind of unique id per storage
    // don't know any API that can get the root path of that storage based on its id.
    //
    // so no "primary" type, but let the check here for other devices
    if ("primary".equalsIgnoreCase(type)) {
      fullPath = Environment.getExternalStorageDirectory() + relativePath;
      if (fileExists(fullPath)) {
        return fullPath;
      }
    }
    // Environment.isExternalStorageRemovable() is `true` for external and internal storage
    // so we cannot relay on it.
    //
    // instead, for each possible path, check if file exists
    // we'll start with secondary storage as this could be our (physically) removable sd card
    fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
    if (fileExists(fullPath)) {
      return fullPath;
    }
    fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
    if (fileExists(fullPath)) {
      return fullPath;
    }
    return fullPath;
  }

  private static boolean fileExists(String filePath) {
    File file = new File(filePath);
    return file.exists();
  }

}

