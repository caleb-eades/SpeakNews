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

public class NewFeedActivity extends ListActivity {
  ArrayList headlines = new ArrayList();
  ArrayList links = new ArrayList();
  Context context = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.create_rss);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    // make create button invisible
    findViewById(R.id.create_feed_button).setVisibility(View.INVISIBLE);
    EditText urlView = (EditText) findViewById(R.id.edit2_field);
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
    // override hardware back button
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      Intent intent = new Intent(this, RssListActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void createFeed(View view) {
    // Hide soft keyboard
    InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    EditText nameView = (EditText) findViewById(R.id.edit1_field);
    EditText urlView = (EditText) findViewById(R.id.edit2_field);
    String name = nameView.getText().toString();
    String url = urlView.getText().toString();
    // parse rss for verification
    RssParser parser = new RssParser();
    parser.execute(url);
    // open database
    DatabaseHandler db = new DatabaseHandler(this);
    Feed feed = db.getFeed(name);
    // if name is empty, require one
    if (name.equals("")) {
      Toast toast = Toast.makeText(context, "Please name your RSS Feed", Toast.LENGTH_SHORT);
      toast.show();
    // require names to be uniqe, check if database entry for name is actually empty
    } else if (feed.getName() != "") {
     Toast toast = Toast.makeText(context, "Names must be unique", Toast.LENGTH_SHORT);
      toast.show();
    // if all is in order, create the feed
    } else if (URLUtil.isValidUrl(url) && headlines.size() != 0) {
      db.addFeed(new Feed(name, url));
      Intent intent = new Intent(this, RssListActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
    // in case something goes wrong
    } else {
      Toast toast = Toast.makeText(context, "URL is not valid", Toast.LENGTH_SHORT);
      toast.show();
    }
  }

  public void previewFeed(View view) {
    // Hide soft keyboard
    InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    // preview the rss
    EditText urlView = (EditText) findViewById(R.id.edit2_field);
    if (URLUtil.isValidUrl(urlView.getText().toString())) {
      new RssParser().execute(urlView.getText().toString());
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
      // set the listview adapter
      ArrayAdapter adapter = new ArrayAdapter(context, R.layout.inflate_list, R.id.list_content, headlines);
      setListAdapter(adapter);
      // grab the buttons
      Button create = (Button) findViewById(R.id.create_feed_button);
      Button preview = (Button) findViewById(R.id.preview_feed_button);
   
      if (headlines.size() == 0) { // Failure
        Toast toast = Toast.makeText(context, "URL is not valid", Toast.LENGTH_SHORT);
        toast.show();
      } else { //Success
        Animation animTranslate = AnimationUtils.loadAnimation(context, R.anim.anim_translate);
        create.setVisibility(View.VISIBLE);
        preview.setVisibility(View.GONE);
        create.startAnimation(animTranslate);
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
        // set the visibility of the buttons
        Button create = (Button) findViewById(R.id.create_feed_button);
        create.setVisibility(View.INVISIBLE);
        Button preview = (Button) findViewById(R.id.preview_feed_button);
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
