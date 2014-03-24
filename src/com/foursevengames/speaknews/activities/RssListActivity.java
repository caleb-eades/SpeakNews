package com.foursevengames.speaknews;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class RssListActivity extends ListActivity {
  public static ArrayList titles = new ArrayList();
  public static ArrayList urlStrings = new ArrayList();
  public static ArrayAdapter adapter;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.rss_list);
    getData();
    registerForContextMenu(getListView());
    // set adapter for listview
    adapter = new ArrayAdapter(this, R.layout.inflate_list, R.id.list_content, titles);
    setListAdapter(adapter);
  }
  // create overflow menu
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.rss_list_menu, menu);
    return true;
  }
  // overflow menu options
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.new_feed_button:
        startActivity(new Intent(this, NewFeedActivity.class));
        overridePendingTransition(R.anim.anim_left_in, R.anim.anim_left_out);
        return true;
/*      case R.id.help_button:
        startActivity(new Intent(this, HelpView.class));
        return true;
*/      default:
        return false;
    }
  }
  // create context menu
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.layout.context_menu, menu);
  }
  // context item selected
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    // get the item's info
    String title = (String) titles.get(info.position);
    String url = (String) urlStrings.get(info.position);
    // open database
    DatabaseHandler db = new DatabaseHandler(this);

    switch (item.getItemId()) {
      case R.id.delete_rss_feed:
        db.deleteFeed(new Feed(title, url));
        adapter.remove(title);
        getData();
        adapter.notifyDataSetChanged();
        return true;
      case R.id.edit_rss_feed:
        Intent intent = new Intent(this, EditFeedActivity.class);
        intent.putExtra("name", title);
        intent.putExtra("url", url);
        startActivity(intent);
        overridePendingTransition(R.anim.anim_left_in, R.anim.anim_left_out);
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  // go to feed
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent intent = new Intent(this, FeedViewActivity.class);
    intent.putExtra("urlString", urlStrings.get(position).toString());
    intent.putExtra("title", titles.get(position).toString());
    intent.putExtra("position", position);
    startActivity(intent);
    overridePendingTransition(R.anim.anim_left_in, R.anim.anim_left_out);
  }
  // refresh data from database
  private void getData() {
    DatabaseHandler db = new DatabaseHandler(this);
    ArrayList<Feed> feeds = db.getAllFeeds();
    titles.clear();
    urlStrings.clear();

    for (Feed feed : feeds) {
      titles.add(feed.getName());
      urlStrings.add(feed.getRssUrl());
    }
  }
}
