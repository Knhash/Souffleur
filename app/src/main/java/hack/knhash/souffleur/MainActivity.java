package hack.knhash.souffleur;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    private static final int DELETE_ID = Menu.FIRST + 2;
    private static final int EDIT_ID = Menu.FIRST + 1;
    public TheAdapter mDbHelper;

    Cursor mNotesCursor;
    SimpleCursorAdapter notes;

    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.list) ListView listview;
    //Here are the Ads
    private AdView mAdView;
    //Here are the Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        //Ads Ahoy
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //Analytics Ahoy
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNote();
            }
        });

        mDbHelper = new TheAdapter(this);
        mDbHelper.open();
        //final ListView listview = (ListView) findViewById(R.id.list);
        fillData(listview);
        registerForContextMenu(listview);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent i = new Intent(MainActivity.this, EditActivity.class);
                i.putExtra(TheAdapter.KEY_ROWID, id);
                startActivityForResult(i, ACTIVITY_EDIT);

                // [START click analytics]
                Bundle params = new Bundle();
                params.putString(TheAdapter.KEY_ROWID, String.valueOf(id));
                mFirebaseAnalytics.logEvent("Item Clicked", params);
                // [STOP click analytics]
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.BACKUP) {
            isStoragePermissionGrantedExport();
            return true;
        }

        if (id == R.id.RESTORE) {
            isStoragePermissionGrantedImport();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fillData(ListView listview) {
        // Get all of the rows from the database and create the item list
        mNotesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(mNotesCursor);

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{TheAdapter.KEY_TITLE,TheAdapter.KEY_DATE};

        // and an array of the fields we want to bind those fields to)
        int[] to = new int[]{R.id.text1,R.id.text3};

        // Now create a simple cursor adapter and set it to display
        notes =
                new SimpleCursorAdapter(this, R.layout.note_row, mNotesCursor, from, to);
        listview.setAdapter(notes);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(2, DELETE_ID, 2, R.string.menu_delete);
        menu.add(1, EDIT_ID, 1, R.string.menu_edit);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case DELETE_ID:
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

                mNotifyMgr.cancel((int) info.id);
                mDbHelper.deleteNote(info.id);

                ListView listview = (ListView) findViewById(R.id.list);
                fillData(listview);
                return true;
            case EDIT_ID:
                info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                Intent i = new Intent(MainActivity.this, EditActivity.class);
                i.putExtra(TheAdapter.KEY_ROWID, info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createNote() {
        Intent i = new Intent(this, EditActivity.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        ListView listview = (ListView) findViewById(R.id.list);
        fillData(listview);
    }

    private void exportDB(){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;//=null;
        FileChannel destination;//=null;
        String currentDBPath = getString(R.string.data_path)+ getString(R.string.package_name) + getString(R.string.databases)+getString(R.string.data_file);
        String backupDBPath = getString(R.string.data_path)+ getString(R.string.package_name) +getString(R.string.databases)+getString(R.string.backup_file);
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        if (!backupDB.exists()) {
            if (!backupDB.getParentFile().mkdirs()) {
                Log.e("File Create Error", "Problem creating Backup file");
                Toast.makeText(this, R.string.problem_file_create, Toast.LENGTH_SHORT).show();
            }
            if (backupDB.exists()) {
                Toast.makeText(this, R.string.file_created, Toast.LENGTH_SHORT).show();
            }
        }
        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            Toast.makeText(this, R.string.notes_backedup, Toast.LENGTH_SHORT).show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void importDB(){
        File sd = Environment.getExternalStorageDirectory();
        File data = Environment.getDataDirectory();
        FileChannel source;//=null;
        FileChannel destination;//=null;
        String currentDBPath = getString(R.string.data_path)+ getString(R.string.package_name) +getString(R.string.databases)+getString(R.string.data_file);
        String backupDBPath = getString(R.string.data_path)+ getString(R.string.package_name) +getString(R.string.databases)+getString(R.string.backup_file);
        File currentDB = new File(data, currentDBPath);
        File backupDB = new File(sd, backupDBPath);
        if (!backupDB.exists()) {
            Toast.makeText(this, R.string.restore_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            source = new FileInputStream(backupDB).getChannel();
            destination = new FileOutputStream(currentDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            ListView listview = (ListView) findViewById(R.id.list);
            fillData(listview);
            Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public  boolean isStoragePermissionGrantedExport() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("Permission","Permission is granted");
                exportDB();
                return true;
            } else {

                Log.v("Permission","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("Permission","Permission is granted");
            return true;
        }
    }

    public  boolean isStoragePermissionGrantedImport() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("Permission","Permission is granted");
                importDB();
                return true;
            } else {

                Log.v("Permission","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("Permission","Permission is granted");
            return true;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = TheProvider.CONTENT_URI;
        return new CursorLoader(this, uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
        notes.swapCursor(arg1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        notes.swapCursor(null);
    }
}
