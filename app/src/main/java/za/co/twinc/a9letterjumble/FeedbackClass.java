package za.co.twinc.a9letterjumble;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

/**
 * Created by wilco on 2018/03/04.
 * Display a feedback prompt dialog from any activity by using this class
 */

class FeedbackClass {
    private Context context;

    FeedbackClass (Context c){
        context = c;
    }

    void showFeedbackDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.feedback));
        builder.setMessage(context.getResources().getString(R.string.feedback_message));

        builder.setPositiveButton(context.getResources().getString(R.string.email_us), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:dev.twinc@gmail.com?subject=9%20Letter%20Jumble%20feedback"));
                try {
                    context.startActivity(emailIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, context.getResources().getString(R.string.txt_no_email),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create().show();
    }
}
