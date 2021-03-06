package de.nico.ha_manager.activities;

/* 
 * @author Nico Alt
 * @author Devin
 * See the file "LICENSE" for the full license governing this code.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

import de.nico.ha_manager.R;
import de.nico.ha_manager.database.Source;
import de.nico.ha_manager.helper.Converter;
import de.nico.ha_manager.helper.CustomAdapter;
import de.nico.ha_manager.helper.Homework;
import de.nico.ha_manager.helper.Subject;
import de.nico.ha_manager.helper.Theme;
import de.nico.ha_manager.helper.Utils;

/**
 * The main class of HW-Manager.
 */
public final class Main extends FragmentActivity {

    /**
     * {@link java.util.ArrayList} containing a {@link java.util.HashMap} with the homework.
     */
    private static ArrayList<HashMap<String, String>> hwArray = new ArrayList<>();

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        Theme.set(this, false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expandable_list);
        setTitle(getString(R.string.title_homework));
        update();

        // If subject list is empty
        if (!(Subject.get(this).length > 0))
            Subject.setDefault(this);
    }

    @Override
    public final void onResume() {
        super.onResume();
        update();
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Preferences.class));
                finish();
                return true;

            case R.id.action_delete:
                deleteAll();
                return true;

            case R.id.action_add:
                startActivity(new Intent(this, AddHomework.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public final void onCreateContextMenu(final ContextMenu menu, final View v,
                                          final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        // menu.add(0, v.getId(), 0, getString(R.string.dialog_completed));
        menu.add(0, v.getId(), 1, getString(R.string.dialog_edit));
        menu.add(0, v.getId(), 2, getString(R.string.dialog_delete));
    }

    @Override
    public final boolean onContextItemSelected(final MenuItem item) {
        final ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item
                .getMenuInfo();
        if (item.getTitle() == getString(R.string.dialog_completed)) {
            final ExpandableListView hwList = (ExpandableListView) findViewById(R.id.expandableListView_main);
            if (Utils.crossOneOut(this, hwArray, ExpandableListView.getPackedPositionGroup(info.packedPosition)))
                update();
            return true;
        }
        if (item.getTitle() == getString(R.string.dialog_edit)) {
            editOne(hwArray, ExpandableListView.getPackedPositionGroup(info.packedPosition));
            return true;
        }
        if (item.getTitle() == getString(R.string.dialog_delete)) {
            deleteOne(hwArray, ExpandableListView.getPackedPositionGroup(info.packedPosition));
            update();
            return true;
        }
        return false;
    }

    /**
     * Updates homework list.
     */
    private void update() {
        // Remove old content
        hwArray.clear();
        final Source s = new Source(this);

        // Get content from SQLite Database
        try {
            s.open();
            hwArray = s.get(this);
            s.close();
        } catch (Exception ex) {
            Log.e("Update Homework List", ex.toString());
        }
        setOnClick();
    }

    /**
     * Sets fake onClickListener which creates a {@link android.view.ContextMenu}.
     */
    private void setOnClick() {
        final ExpandableListView hwList = (ExpandableListView) findViewById(R.id.expandableListView_main);
        final ExpandableListAdapter e = CustomAdapter.expandableEntry(this, hwArray);
        hwList.setAdapter(e);
        registerForContextMenu(hwList);
        /*
        Still does not work...
        Utils.crossOut(e, hwArray);
         */
    }

    /**
     * Edits a homework.
     *
     * @param ArHa {@link java.util.ArrayList} containing a {@link java.util.HashMap} with the homework.
     * @param pos  Position where the homework is.
     */
    private void editOne(final ArrayList<HashMap<String, String>> ArHa, final int pos) {
        final String currentID = "ID = " + ArHa.get(pos).get(Source.allColumns[0]);
        final Intent intent = new Intent(this, AddHomework.class);
        final Bundle mBundle = new Bundle();
        mBundle.putString(Source.allColumns[0], currentID);
        for (int i = 1; i < Source.allColumns.length; i++)
            mBundle.putString(Source.allColumns[i],
                    ArHa.get(pos).get(Source.allColumns[i]));
        intent.putExtras(mBundle);
        startActivity(intent);
    }

    /**
     * Deletes a homework.
     *
     * @param ArHa {@link java.util.ArrayList} containing a {@link java.util.HashMap} with the homework.
     * @param pos  Position where the homework is.
     */
    private void deleteOne(final ArrayList<HashMap<String, String>> ArHa, final int pos) {
        final ArrayList<HashMap<String, String>> tempArray = Converter.toTmpArray(ArHa, pos);
        final String currentID = "ID = " + ArHa.get(pos).get(Source.allColumns[0]);
        final SimpleAdapter alertAdapter = CustomAdapter.entry(this, tempArray);

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog
                .setTitle(getString(R.string.dialog_delete))
                .setAdapter(alertAdapter, null)
                .setPositiveButton((getString(android.R.string.yes)),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public final void onClick(final DialogInterface d, final int i) {
                                Homework.delete(Main.this, currentID);
                                update();
                            }
                        })
                .setNegativeButton((getString(android.R.string.no)), null)
                .show();
    }

    /**
     * Deletes all homework.
     */
    private void deleteAll() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog
                .setTitle(getString(R.string.dialog_delete))
                .setMessage(getString(R.string.dialog_really_delete_hw))
                .setPositiveButton((getString(android.R.string.yes)),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public final void onClick(final DialogInterface d, final int i) {
                                Homework.delete(Main.this, null);
                                update();
                            }
                        })
                .setNegativeButton((getString(android.R.string.no)), null)
                .show();
    }
}
