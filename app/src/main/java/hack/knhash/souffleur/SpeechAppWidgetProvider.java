package hack.knhash.souffleur;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.EditText;
import android.widget.RemoteViews;

/**
 * Created by knhash on 9/1/17.
 */
public class SpeechAppWidgetProvider extends AppWidgetProvider {

    String speechTitle = "";
    Intent intent;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        TheAdapter mDbHelper = new TheAdapter(context);
        mDbHelper.open();
        Cursor note = mDbHelper.fetchNote(999);
        if(note == null) {
            speechTitle = context.getString(R.string.bringabait);
            note.close();
        }
        else if( note.moveToLast() ){
            speechTitle = note.getString(
                    note.getColumnIndexOrThrow(TheAdapter.KEY_TITLE));
            note.close();
        }

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch ExampleActivity
            if(speechTitle.equals(context.getString(R.string.bringabait))) {
                intent = new Intent(context, EditActivity.class);
                long id = 999;
                intent.putExtra(TheAdapter.KEY_ROWID, id);
            }
            else {
                intent = new Intent(context, MainActivity.class);
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);


            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.speech_appwidget);
            views.setTextViewText(R.id.speech_widget_text, speechTitle);
            views.setOnClickPendingIntent(R.id.speech_widget_frame, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}