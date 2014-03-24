package com.foursevengames.speaknews;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.InputStream;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.NullPointerException;
import java.lang.RuntimeException;
import java.lang.Thread;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class EditFeedActivity extends ListActivity {
  ArrayList headlines = new ArrayList();
  ArrayList links = new ArrayList();
  Context context = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.edit_rss);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    // set visibility of preview button
    Button preview = (Button) findViewById(R.id.preview_feed_button2);
    preview.setVisibility(View.GONE);
    // set content on edittexts to the values from the intent
    EditText urlView = (EditText) findViewById(R.id.edit_url);
    EditText nameView = (EditText) findViewById(R.id.edit_name);
    nameView.setText(getIntent().getExtras().getString("name"));
    urlView.setText(getIntent().getExtras().getString("url"));

    watchText(urlView);
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // app icon in action bar clicked; go home
        // back button
        Intent intent = new Intent(this, RssListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    // hardware back button override
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      Intent intent = new Intent(this, RssListActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void updateFeed(View view) {
    // close the soft keyboard
    InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    // grabbing edit texts
    EditText nameView = (EditText) findViewById(R.id.edit_name);
    EditText urlView = (EditText) findViewById(R.id.edit_url);
    String name = nameView.getText().toString();
    String url = urlView.getText().toString();
    // opening database, grabbing the feed to update; suspect bug
    DatabaseHandler db = new DatabaseHandler(this);
    // name validation
    if (name.equals("")) {
      Toast toast = Toast.makeText(context, "Please name your RSS Feed", Toast.LENGTH_SHORT);
      toast.show();
    // check url validity and parsability
    } else if (URLUtil.isValidUrl(url)) {
      Feed feed = db.getFeed(getIntent().getExtras().getString("name"));
      feed.setName(name);
      feed.setRssUrl(url);
      db.updateFeed(feed);

      Intent intent = new Intent(this, RssListActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);

    } else {
      Toast toast = Toast.makeText(context, "URL is not valid", Toast.LENGTH_SHORT);
      toast.show();
    }
  }

  public void previewFeed(View view) {
    // close the soft keyboard
    InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    // get the url edit text
    EditText urlView = (EditText) findViewById(R.id.edit_url);
    String url = urlView.getText().toString();
    if (URLUtil.isValidUrl(url)) {
      new RssParser().execute(url);
    } else {
      Toast toast = Toast.makeText(context, "URL is not valid", Toast.LENGTH_SHORT);
      toast.show(); 
    }
  }

  public class RssParser extends AsyncTask<String, Void, ArrayList> {
    protected ArrayList doInBackground(String... urlString) {
      try {
        URL url = new URL(urlString[0]);
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser xpp = factory.newPullParser();

        xpp.setInput(getInputStream(url), "UTF_8");
        boolean insideItem = false;

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
          if (eventType == XmlPullParser.START_TAG) {
            if (xpp.getName().equalsIgnoreCase("item") || xpp.getName().equalsIgnoreCase("entry")) {
              insideItem = true;
            } else if (xpp.getName().equalsIgnoreCase("title")) {
              if (insideItem)
                headlines.add(xpp.nextText()); //extract the headline
            } else if (xpp.getName().equalsIgnoreCase("link")) {
              if (insideItem)
                links.add(xpp.nextText()); //extract the link of article
            }
          }else if(eventType==XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")){
            insideItem=false;
          }else if(eventType==XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("entry")){
            insideItem=false;
          }
            eventType = xpp.next(); //move to next element
          }
      } catch (MalformedURLException e) {
        e.printStackTrace();
        headlines = new ArrayList();
      } catch (XmlPullParserException e) {
        e.printStackTrace();
        headlines = new ArrayList();
      } catch (IOException e) {
        e.printStackTrace();
        headlines = new ArrayList();
      } catch (RuntimeException e) {
        e.printStackTrace();
        headlines = new ArrayList();
      }
      return headlines;
    }

    protected void onPostExecute(ArrayList headlines) {
      ArrayAdapter adapter = new ArrayAdapter(context, R.layout.inflate_list, R.id.list_content, headlines);
      setListAdapter(adapter);
      Button update = (Button) findViewById(R.id.update_feed_button);
      Button preview = (Button) findViewById(R.id.preview_feed_button2);
   
      if (headlines.size() == 0) { // Failure
        Toast toast = Toast.makeText(context, "URL is not valid", Toast.LENGTH_SHORT);
        toast.show();
      } else { //Success
        Animation animTranslate = AnimationUtils.loadAnimation(context, R.anim.anim_translate);
        update.setVisibility(View.VISIBLE);
        preview.setVisibility(View.GONE);
        update.startAnimation(animTranslate);
      }
    }
  }

  public void watchText(EditText et) {
    et.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }
      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        Animation animTranslate = AnimationUtils.loadAnimation(context, R.anim.anim_translate);
        Button update = (Button) findViewById(R.id.update_feed_button);
        update.setVisibility(View.INVISIBLE);
        Button preview = (Button) findViewById(R.id.preview_feed_button2);
        if (preview.getVisibility() != View.VISIBLE){
          preview.setVisibility(View.VISIBLE);
          preview.startAnimation(animTranslate);
        }
      }
      @Override
      public void afterTextChanged(Editable editable) {
      }
    });
  }


  public InputStream getInputStream(URL url) {
    try {
      return url.openConnection().getInputStream();
    } catch (IOException e) {
      return null;
    }
  }
} 
