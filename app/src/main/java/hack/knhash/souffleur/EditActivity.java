package hack.knhash.souffleur;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
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

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EditActivity extends AppCompatActivity {

    //TODO: Handle cancel event

    private TheAdapter mDbHelper;
    //private EditText mTitleText;
    //private EditText mBodyText;
    //private TextView mCountText;
    private Long mRowId;
    public String notstat;
    TextToSpeech tts;

    @BindView(R.id.title)  EditText mTitleText;
    @BindView(R.id.body)  EditText mBodyText;
    @BindView(R.id.counter)  TextView mCountText;

    int curPromptStart=-1, curPromptEnd=-1;

    final int REQ_CODE=666;
    boolean finished=false;
    StringBuilder currentSpeech=new StringBuilder("s");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_edit);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab1);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startListening();
                Log.i("LOG_TAG", "After function call");
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDbHelper = new TheAdapter(this);
        mDbHelper.open();

        //mTitleText = (EditText) findViewById(R.id.title);
        //mBodyText = (EditText) findViewById(R.id.body);
        //mCountText = (TextView) findViewById(R.id.counter);
        notstat = getString(R.string.notstat_unset);

        mRowId = (savedInstanceState == null) ? null :
                (Long) savedInstanceState.getSerializable(TheAdapter.KEY_ROWID);
        if (mRowId == null) {
            Bundle extras = getIntent().getExtras();
            mRowId = extras != null ? extras.getLong(TheAdapter.KEY_ROWID)
                    : null;
        }

        populateFields();

        //Prevent Keyboard on start
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //NEW STUFF


        tts =  new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    {
                        //Toast.makeText(getApplicationContext(), speak,Toast.LENGTH_SHORT).show();
                        Log.i("LOG_TAG", "Inside TTS Check");
                    }

                } else {
                    Log.e("LOG_TAG", "Initilization Failed!");
                }
            }
        });

    }

    private void populateFields() {
        if (mRowId != null) {
            Cursor note = mDbHelper.fetchNote(mRowId);
            startManagingCursor(note);
            mTitleText.setText(note.getString(
                    note.getColumnIndexOrThrow(TheAdapter.KEY_TITLE)));
            mBodyText.setText(note.getString(
                    note.getColumnIndexOrThrow(TheAdapter.KEY_BODY)));
            mCountText.setText(note.getString(
                    note.getColumnIndexOrThrow(TheAdapter.KEY_DATE)));
            notstat = note.getString(
                    note.getColumnIndexOrThrow(TheAdapter.KEY_UPDATE));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(TheAdapter.KEY_ROWID, mRowId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }

    private void saveState() {
        String title = mTitleText.getText().toString();
        String body = mBodyText.getText().toString();

        Date date = new Date();
        String Date= DateFormat.getDateInstance().format(date);
        mCountText.setText(Date);

        String count = mCountText.getText().toString();

        if (mRowId == null && !title.equals("")) {
            long id = mDbHelper.createNote(title, body, count, notstat);
            if (id > 0) {
                mRowId = id;
            }
            return;
        }

        else if (title.equals("") && !body.equals("")){
            Toast.makeText(this, R.string.toast_auto_title, Toast.LENGTH_SHORT).show();
            String temptitle = getString(R.string.temp_title);
            long id = mDbHelper.createNote(temptitle, body, count, notstat);
            if (id > 0) {
                mRowId = id;
            }
            return;
        }

        else if (title.equals("") && body.equals("")){
            Toast.makeText(this, R.string.toast_discarded, Toast.LENGTH_SHORT).show();
            return;
        }

        else {
            mDbHelper.updateNote(mRowId, title, body, count, notstat);
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.SHARE) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "  " + mTitleText.getText().toString() + "\n\n");
            sendIntent.putExtra(Intent.EXTRA_TEXT, mBodyText.getText().toString());
            sendIntent.setType(getString(R.string.text_type));
            startActivity(sendIntent);
            return true;
        }

        if (id == R.id.DELETE) {
            mDbHelper.deleteNote(mRowId);
            finish();
            notstat = getString(R.string.notstat_unset);
            Toast.makeText(this, R.string.toast_discarded, Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.PASTEBIN) {
            new AsyncTaskPost().execute(mBodyText.getText().toString());
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    void startListening() {
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.start_speaking));
        startActivityForResult(intent, REQ_CODE);
    }

    void analyzeSpeech() {
        //Log.i("LOG_TAG", "Saved speech: "+mBodyText.getText());

        //If end of speech reached, finished=true
        //Else prompt next sentence

        int currentSpeechLength=currentSpeech.length();
        String body = mBodyText.getText().toString();

        //aa

        //Maintain curPromptStart and curPromptEnd as global variables

        if( currentSpeechLength<body.length()  && !finished) {

            if (curPromptStart == -1) {
                for (curPromptStart = currentSpeechLength; body.charAt(curPromptStart) != '.' && curPromptStart > 0; --curPromptStart)
                    ;
                for (curPromptEnd = currentSpeechLength; curPromptEnd < body.length() && body.charAt(curPromptEnd) != '.'; ++curPromptEnd)
                    ;
                prompt(body.substring(curPromptStart, curPromptEnd));
            } else {
                curPromptStart = ++curPromptEnd;
                for (; curPromptEnd < body.length() && body.charAt(curPromptEnd) != '.'; ++curPromptEnd) ;
                prompt(body.substring(curPromptStart, curPromptEnd));
                if(curPromptEnd >= body.length()) {
                    curPromptStart = curPromptEnd = -1;
                    finished = true;
                }
            }
        }

        else {
            finished=true;
        }


        //bb
    }

    void prompt(final String speak) {
        //Start prompting mBodyText from speakFrom
        tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                finish();
            }
        });
        tts.speak(speak, TextToSpeech.QUEUE_FLUSH, null);
        Log.i("LOG_TAG", "Inside : prompt ");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data/*.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)*/ != null) {
            currentSpeech.append(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0));
            currentSpeech.append(" ");
            curPromptStart=curPromptEnd=-1;
            //Log.i("LOG_TAG", "Current speech: "+currentSpeech);
        }

        if(resultCode == RESULT_CANCELED) {
            Log.i("LOG_TAG", "Inside : Cancellation ");
            tts.stop();
            tts.shutdown();
        }

        else {
            if(!finished) {
                analyzeSpeech();
            }

            if(finished) {
                Log.i("LOG_TAG", "Inside : finished 1 ");
                tts.stop();
                tts.shutdown();
                //finish();
            }
            if(!finished) {
                startListening();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finished=true;
        //finish();
        Log.i("LOG_TAG", "Inside : back pressed ");
        tts.stop();
        tts.shutdown();
        curPromptStart=curPromptEnd=-1;

    }

    private class AsyncTaskPost extends AsyncTask<String, Integer, String> {

        String responseBody;

        @Override
        protected String doInBackground(String... params) {

            return postData(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", result);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(EditActivity.this, result + getString(R.string.copiedToClipboard), Toast.LENGTH_LONG).show();
        }

        public String postData(String text) {
            // Create a new HttpClient and Post Header
            Log.e("WHERE", "POSTDATA");
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://pastebin.com/api/api_post.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("api_dev_key", "API_KEY_HERE"));
                nameValuePairs.add(new BasicNameValuePair("api_option", "paste"));
                nameValuePairs.add(new BasicNameValuePair("api_paste_code", text));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                responseBody = EntityUtils.toString(response.getEntity());

            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                responseBody = "Post Error";
            } catch (IOException e) {
                // TODO Auto-generated catch block
                responseBody = "IO Error";
            }
            return responseBody;
        }
    }
}