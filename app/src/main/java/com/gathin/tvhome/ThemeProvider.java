package com.gathin.tvhome;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lets tools/theme.sh write the theme files into {@link Config#dir} on firmware where the adb shell is
 * locked out of Android/data (TCL G07, Android 11). Only the files {@link ThemeFiles} allows, and only for
 * callers holding WRITE_SECURE_SETTINGS, which the adb shell has and ordinary apps cannot get.
 *
 *   content write --uri content://com.gathin.tvhome.theme/config.json < /data/local/tmp/config.json
 *   content write --uri content://com.gathin.tvhome.theme/wallpapers/dusk.jpg < /data/local/tmp/dusk.jpg
 *   content delete --uri content://com.gathin.tvhome.theme/config.json
 *   content delete --uri content://com.gathin.tvhome.theme/wallpapers      (every image in the collection)
 */
public class ThemeProvider extends ContentProvider {

    private File file(Uri uri) throws FileNotFoundException {
        String path = ThemeFiles.path(uri.getPathSegments());
        File dir = Config.dir(getContext());
        if (dir == null || path == null) throw new FileNotFoundException(String.valueOf(uri));
        File f = new File(dir, path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new FileNotFoundException(String.valueOf(uri));
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if ("/tmdb-credential".equals(uri.getPath())) {
            if (!"w".equals(mode)) throw new FileNotFoundException("Write-only credential import");
            return ParcelFileDescriptor.open(new TmdbAuth(getContext()).file(), ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        File f = file(uri);
        if (mode.contains("w")) {
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        File dir = Config.dir(getContext());
        if (dir != null && ThemeFiles.isCollection(uri.getPathSegments())) {
            File[] images = new File(dir, ThemeFiles.WALLPAPERS).listFiles((d, name) -> ThemeFiles.collectionImage(name));
            int n = 0;
            if (images != null) for (File f : images) if (f.delete()) n++;
            return n;
        }
        try {
            return file(uri).delete() ? 1 : 0;
        } catch (FileNotFoundException e) {
            return 0;
        }
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
        if ("/startup".equals(uri.getPath())) {
            android.content.SharedPreferences values = StartupState.preferences(getContext());
            MatrixCursor cursor = new MatrixCursor(new String[]{"enabled", "boot_count", "handled_boot",
                    "attempt_boot", "resume_boot"});
            cursor.addRow(new Object[]{StartupState.enabled(getContext()) ? 1 : 0,
                    StartupState.bootCount(getContext()), values.getInt("handled_boot", -1),
                    values.getInt("attempt_boot", -1), values.getInt("resume_boot", -1)});
            return cursor;
        }
        if (!"/tmdb-credential".equals(uri.getPath())) return null;
        MatrixCursor cursor = new MatrixCursor(new String[]{"configured"});
        cursor.addRow(new Object[]{new TmdbAuth(getContext()).configured() ? 1 : 0});
        return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) {
        if (!"/startup".equals(uri.getPath()) || v.size() != 1 || !v.containsKey("enabled")) return 0;
        Integer enabled = v.getAsInteger("enabled");
        if (enabled == null || (enabled != 0 && enabled != 1)) return 0;
        return StartupState.configure(getContext(), enabled == 1) ? 1 : 0;
    }
}
