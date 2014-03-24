package com.foursevengames.speaknews;

import android.app.ListActivity;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.IOException;
import java.lang.RuntimeException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class FeedViewActivity extends ListActivity implements TextToSpeech.OnInitListener {
  Context context = this;
  ArrayAdapter adapter;
  static ArrayList<String> headlines = new ArrayList<String>();
  ArrayList<String> links = new ArrayList<String>();
  private TextToSpeech tts;
  int positionInParent;
  int positionClicked = 0;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.feed_view);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    this.setTitle(getIntent().getExtras().getString("title"));
    //registerForContextMenu(getListView());
    registerToShare();
    // assign adapter to listview
    adapter = new ArrayAdapter(context, R.layout.inflate_list, R.id.list_content);
    setListAdapter(adapter);
    adapter.setNotifyOnChange(true);
    //swipeListen(); uncomment with method when scroll lag is fixed
    positionInParent = getIntent().getExtras().getInt("position");
    // scroll to the position of the button they clicked
    if (positionClicked > 5) {
      getListView().smoothScrollToPosition(positionClicked);
    }
    // fade left button if first feed
    if (positionInParent == 0) {
      findViewById(R.id.previous_button).setBackgroundResource(R.drawable.previous_icon_light);
    }
    // fade right button if last feed
    if (positionInParent +1 == RssListActivity.titles.size()) {
      findViewById(R.id.next_button).setBackgroundResource(R.drawable.next_icon_light);
    }
    tts = new TextToSpeech(this, this);
    new RssParser().execute(getIntent().getExtras().getString("urlString"));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // app icon in action bar clicked; go home
        Intent intent = new Intent(this, RssListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  // delete this when the methods below are uncommented
  // forcibly creates share options without a context menu
  public void registerToShare() {
    getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
        String url = (String) links.get(position);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(shareIntent,"Share with"));
        return true;
      }
    });
  }
/*  Uncomment later when there are more items to add to 
    the the context menu; for now, having one is bad form

  // create context menu
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.layout.feed_context_menu, menu);
  }
  // context item selected
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    // get the item's info
    String url = (String) links.get(info.position);

    switch (item.getItemId()) {
      case R.id.share_item:
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(shareIntent,"Share with"));
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }
*/
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      Intent intent = new Intent(this, RssListActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    tts.stop();
    positionClicked = position;
    // change the stop button back to play
    Button play = (Button) findViewById(R.id.play_button);
    play.setBackgroundResource(R.drawable.play_icon);
    // go to browser
    Uri uri = Uri.parse((String) links.get(position));
    startActivity(new Intent(Intent.ACTION_VIEW, uri));
  }

  private void say(ArrayList<String> headlines) {
    for (String headline : headlines)  {
      tts.speak(headline, TextToSpeech.QUEUE_ADD, null);
      tts.playSilence(600, TextToSpeech.QUEUE_ADD, null);
    }
  }

  public void play(View v) {
    // logic controlling the play button
    if (tts.isSpeaking()) {
      tts.stop();
      v.setBackgroundResource(R.drawable.play_icon);
    } else {
      say(headlines);
      v.setBackgroundResource(R.drawable.stop_icon);
    }
  }

  public void previousFeed(View v) {
    // only go to the previous feed if there is one
    if (positionInParent != 0) {
      tts.stop();
      tts.shutdown();
      findViewById(R.id.play_button).setBackgroundResource(R.drawable.play_icon);
      Intent intent = new Intent(this, FeedViewActivity.class);
      intent.putExtra("urlString", RssListActivity.urlStrings.get(positionInParent - 1).toString());
      intent.putExtra("title", RssListActivity.titles.get(positionInParent - 1).toString());
      intent.putExtra("position", positionInParent - 1);
      finish();
      overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
      startActivity(intent);
    }
  }

  public void nextFeed(View v) {
    // only go to the next feed if there is one
    if (positionInParent != RssListActivity.titles.size()-1) {
      tts.stop();
      tts.shutdown();
      findViewById(R.id.play_button).setBackgroundResource(R.drawable.play_icon);
      Intent intent = new Intent(this, FeedViewActivity.class);
      intent.putExtra("urlString", RssListActivity.urlStrings.get(positionInParent + 1).toString());
      intent.putExtra("title", RssListActivity.titles.get(positionInParent + 1).toString());
      intent.putExtra("position", positionInParent + 1);
      finish();
      overridePendingTransition(0, R.anim.anim_left_out);
      startActivity(intent);
    }
  }
/*  Uncomment for swipe gesture listening. Currently causes scrolling to be sluggish
  public void swipeListen() {
    // override some methods in custom swipe listener for left and right
    getListView().setOnTouchListener(new OnSwipeTouchListener() {
      @Override
      public void onSwipeRight() {
        // swiping from left to right, go to previous feed
        if (positionInParent != 0) {
          tts.stop();
          tts.shutdown();
          findViewById(R.id.play_button).setBackgroundResource(R.drawable.play_icon);
          Intent intent = new Intent(context, FeedViewActivity.class);
          intent.putExtra("urlString", RssListActivity.urlStrings.get(positionInParent - 1).toString());
          intent.putExtra("title", RssListActivity.titles.get(positionInParent - 1).toString());
          intent.putExtra("position", positionInParent - 1);
          finish();
          overridePendingTransition(R.anim.anim_right_in, R.anim.anim_right_out);
          startActivity(intent);
        }

      }
      @Override
      public void onSwipeLeft() {
        // swiping right to left, go to next feed
        if (positionInParent != RssListActivity.titles.size()-1) {
          tts.stop();
          tts.shutdown();
          findViewById(R.id.play_button).setBackgroundResource(R.drawable.play_icon);
          Intent intent = new Intent(context, FeedViewActivity.class);
          intent.putExtra("urlString", RssListActivity.urlStrings.get(positionInParent + 1).toString());
          intent.putExtra("title", RssListActivity.titles.get(positionInParent + 1).toString());
          intent.putExtra("position", positionInParent + 1);
          finish();
          overridePendingTransition(0, R.anim.anim_left_out);
          startActivity(intent);
        }
      }
    });
  }
*/
  public class RssParser extends AsyncTask<String, Void, ArrayList<String>> {
    protected ArrayList<String> doInBackground(String... urlString) {
      try {
        headlines.clear();
        links.clear();
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
              Log.d("#############################  Parsing RSS Feed", "Inside element " + xpp.getName().toString());
              insideItem = true;
            } else if (xpp.getName().equalsIgnoreCase("title")) {
              Log.d("#############################  Parsing RSS Feed", "### Inside element " + xpp.getName().toString());
              if (insideItem) {
                String temp = xpp.nextText();
                headlines.add(temp); //extract the headline
                Log.d("#############################  Parsing RSS Feed", "### Adding to headlines: " + temp);
              }
            } else if (xpp.getName().equalsIgnoreCase("link")) {
              Log.d("#############################  Parsing RSS Feed", "### Inside element " + xpp.getName().toString());
              if (insideItem) {
                if (xpp.getAttributeCount() == 0 ) {
                  String temp = xpp.nextText();
                  links.add(temp); //extract the link of article
                  Log.d("#############################  Parsing RSS Feed", "### Adding to links: " + temp);
                } else {
                  int i = 0;
                  while (!xpp.getAttributeName(i).equalsIgnoreCase("href")) {
                    i++;
                  }
                  links.add(xpp.getAttributeValue(i));
                  Log.d("#############################  Parsing RSS Feed", "### Adding to links: " + xpp.getAttributeValue(i));
                }
              }
            }
          }else if(eventType==XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")){
            Log.d("#############################  Parsing RSS Feed", "Exiting element " + xpp.getName().toString());
            insideItem=false;
          }else if(eventType==XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("entry")){
            Log.d("#############################  Parsing RSS Feed", "Exiting element " + xpp.getName().toString());
            insideItem=false;
          }
          eventType = xpp.next(); //move to next element       
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (XmlPullParserException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
      return headlines;
    }

    protected void onPostExecute(ArrayList<String> titles) {
      if (titles.size() == 0) { // Failure
        Toast toast = Toast.makeText(context, "RSS Retrieval failed. Check data connection", Toast.LENGTH_SHORT);
        toast.show();
      } else {
        adapter.clear();
        adapter.addAll(titles);
        headlines = titles;
      }
    }
  }

  public InputStream getInputStream(URL url) {
    try {
      return url.openConnection().getInputStream();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public void onInit(int status) {
  // TODO Auto-generated method stub
    Log.d(this.getClass().getCanonicalName(), "onInit");
    if (status == TextToSpeech.SUCCESS) {
      int result = tts.setLanguage(Locale.UK);
      // tts.setPitch(5); // set pitch level
      //tts.setSpeechRate(1); // set speech speed rate
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.e("TTS", "Language is not supported");
      }  
    } else {
      Log.e("TTS", "Initilization Failed");
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(this.getClass().getCanonicalName(), "onDestroy");
    // Don't forget to shutdown!
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
  }

}
