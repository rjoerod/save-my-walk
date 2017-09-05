package rjoerod.savemywalk.adapter;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import rjoerod.savemywalk.fragment.ConfirmationFragment;

import static android.content.ContentValues.TAG;


/**
 * Created by Ryan on 6/26/2017.
 * Modified from links below:
 * http://www.vogella.com/tutorials/AndroidListView/article.html#adapterown
 * https://stackoverflow.com/questions/17545060/custom-view-with-button-in-arrayadapter
 */

public class InteractiveListAdapter extends ArrayAdapter<String> {

    private ListView listView;
    private List<String> routeIDs;
    private List<String> routeNames;
    private Context context;

    static class ViewHolder {
        TextView text;
        Button loadBtn;
        Button deleteBtn;
    }

    public InteractiveListAdapter(Context context, List<String> routeIDs,
                                                   List<String> routeNames, ListView listView) {
        super(context, -1, routeIDs);
        this.context = context;
        this.routeIDs = routeIDs;
        this.routeNames = routeNames;
        this.listView = listView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        final ViewGroup finalParent = parent;

        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            rowView = inflater.inflate(rjoerod.savemywalk.R.layout.saved_routes_list_item, null);
            // configure view holder
            ViewHolder holder = new ViewHolder();
            holder.text = (TextView) rowView.findViewById(rjoerod.savemywalk.R.id.route_name);
            holder.loadBtn = (Button) rowView.findViewById(rjoerod.savemywalk.R.id.load_button);
            holder.deleteBtn = (Button) rowView.findViewById(rjoerod.savemywalk.R.id.delete_button);
            rowView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        final String someRouteID = routeIDs.get(position);
        final String someRouteName = routeNames.get(position);
        holder.text.setText(someRouteName);

        // set onclick to load route to map
        holder.loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "A Route " + someRouteID + " was loaded.");
                loadBtn(someRouteID);
            }
        });

        // set onclick to delete route from file
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = ConfirmationFragment
                        .newDeleteItemInstance("Permanently delete the file?", someRouteID);
                context = ((Activity)(finalParent.getContext()));
                FragmentActivity activity = (FragmentActivity)(context);
                FragmentManager fm = activity.getSupportFragmentManager();
                dialog.show(fm, "ConfirmationFragment");
            }
        });
      return rowView;
    }

    private void loadBtn(String someRouteID) {
        try {
            // Prepare to overwrite temp
            File fileDirectory = context.getDir("temp", Context.MODE_PRIVATE);
            File tempFile = new File(fileDirectory, "temp");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempFile));

            // prepare to read from rt#
            fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
            File someRouteFile = new File(fileDirectory, someRouteID);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(someRouteFile));

            if(someRouteFile.exists()) {
                // read rt# into temp
                String someLine = bufferedReader.readLine();
                // ignore first line
                someLine = bufferedReader.readLine();
                // read to end of file
                while( !someLine.equals("STOP") ) {
                    bufferedWriter.write(someLine);
                    bufferedWriter.newLine();
                    someLine = bufferedReader.readLine();
                }
                bufferedWriter.write("STOP");
                bufferedWriter.newLine();
            } else {
                Log.e(TAG, "Major File Error: " + someRouteID + " doesn't exist.");
            }
            bufferedWriter.flush();
            bufferedWriter.close();
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
