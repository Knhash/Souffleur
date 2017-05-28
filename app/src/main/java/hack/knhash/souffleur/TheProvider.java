package hack.knhash.souffleur;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by knhash on 14/1/17.
 */

public class TheProvider extends ContentProvider {

    public static final String PROVIDER_NAME = "hack.knhash.souffleur";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/TheAdapter");
    public static final UriMatcher uriMatcher;
    /** Constants to identify the requested operation */
    private static final int CUST = 1;

    static {
                uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
                uriMatcher.addURI(PROVIDER_NAME, "TheAdapter", CUST);
            }
    public TheAdapter adapter;

    @Override
    public boolean onCreate() {
        adapter = new TheAdapter(getContext());
        adapter.open();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if(uriMatcher.match(uri) == CUST){
            return adapter.fetchAllNotes();
        }else{
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
