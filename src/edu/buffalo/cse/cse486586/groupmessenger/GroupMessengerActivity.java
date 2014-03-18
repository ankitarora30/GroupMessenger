package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

//import edu.buffalo.cse.cse486586.simplemessenger.SimpleMessengerActivity.ClientTask;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
	static final String TAG = GroupMessengerActivity.class.getSimpleName();
	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	static final int SERVER_PORT = 10000;
	static int key_counter = -1;
	static int last_key=-1;
	String myPort;

	/**The data structure used as a buffer on the client side*/
	ArrayList<String> al = new ArrayList<String>();

	/** Called when the Activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);

		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {	
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		final EditText editText = (EditText) findViewById(R.id.editText1);

		Button button =(Button) findViewById(R.id.button4);
		button.setOnClickListener(new OnClickListener() {

			/**Called whenever the send button is clicked */
			@Override
			public void onClick(View v) {
				String msg = editText.getText().toString() + "\n";
				editText.setText("");
				TextView localTextView = (TextView) findViewById(R.id.textView1);
				localTextView.append("\t\t" + msg); // This is one way to display a string.
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

			}
		});	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}
	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@SuppressWarnings("deprecation")
		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];

			Socket soc=null;
			DataInputStream input;
			String s;
			while(true)
			{
				try{
					soc=serverSocket.accept();            
					input= new DataInputStream(soc.getInputStream());
					s=input.readLine();
					String seq_request[] = s.split("\\|");
					if(seq_request[0].equals("-1")&& myPort.equals("11108") )
					{
						key_counter++;
						s=Integer.toString(key_counter)+"|"+seq_request[1];
						Log.v(TAG, "returning the msg with the sequence number");
						PrintStream seq_out = new PrintStream(soc.getOutputStream());            
						seq_out.append(s);
						seq_out.flush();
						soc.close();
					}
					else
					{
						Log.v(TAG, "calling on publish progress");
						publishProgress(s);
					}
				}
				catch(IOException e){
					Log.e(TAG, "Error");
				}         
			}
		}
		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
			uriBuilder.scheme(scheme);
			return uriBuilder.build();
		}

		/**Called to insert data into database and deliver msg to the client*/
		protected void deliver(String...strings){

			String strReceived = strings[0].trim();
			String array[]=strReceived.split("\\|");

			TextView remoteTextView = (TextView) findViewById(R.id.textView1);
			remoteTextView.append(array[1] + "\t\t\n");	

			/*Inserting values in the database using the content resolver */
			ContentValues values = new ContentValues();
			values.put("key",array[0]);
			values.put("value",array[1]);
			final Uri mUri;

			mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
			try {
				getContentResolver().insert(mUri, values);
			}
			catch (Exception e) {
				Log.e(TAG, e.toString());
			}			
		}

		protected void onProgressUpdate(String...strings) {
			/*
			 * The following code checks what sequence number is received and takes 
			 * the necessary step.
			 */
			String strReceived = strings[0].trim();
			String array[]=strReceived.split("\\|");

			if((Integer.parseInt(array[0])-last_key)==1)
			{
				Log.v(TAG, "key inserting in database "+array[0]);
				last_key=Integer.parseInt(array[0]);
				deliver(strReceived);
				if(!al.isEmpty())
				{
					for(int i=0;i<al.size();i++)
					{
						String buf_retrieve=al.get(i);
						String array_buffer[]=buf_retrieve.split("\\|");

						if((Integer.parseInt(array_buffer[0])-last_key)==1) //check if it has the expected value
						{
							Log.v(TAG, "key inserting in database "+array_buffer[0]);
							deliver(buf_retrieve);
							al.remove(i);
							last_key=Integer.parseInt(array_buffer[0]);
							i=-1;
						}
					}
				}
			}
			else
			{
				al.add(strReceived);
				Log.v(TAG, "entry inserting in buffer "+strReceived);				
			}			
			return;
		}
	}

	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {
			try {
				/*To send request and the message to sequencer get a sequence number
				 *  from the sequencer */
				String seq_port=REMOTE_PORT0;
				Socket seq_socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						Integer.parseInt(seq_port));
				String get_seq_for_msg = "-1"+"|"+msgs[0];
				PrintStream seq_out = new PrintStream(seq_socket.getOutputStream());            
				seq_out.append(get_seq_for_msg);
				seq_out.flush();
				Log.d("request seq sent", "request for seq number sent");

				/*To receive the sequence number from the sequencer */				
				DataInputStream input= new DataInputStream(seq_socket.getInputStream());
				String s=input.readLine();
				seq_socket.close();
				Log.d("seq_number obtained", s);

				/*To multicast the sequence number and msg to all clients including self */
				String msgToSend = s;
				int i =0;
				String remotePorts[]={REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
				for(i=0;i<5;i++)
				{
					Log.v(TAG, "Multicasting");					
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(remotePorts[i]));
					PrintStream out = new PrintStream(socket.getOutputStream());            
					out.append(msgToSend);
					out.flush();
					socket.close();
				}
			} catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
			} catch (IOException e) {
				Log.e(TAG, "ClientTask socket IOException");
			}
			return null;
		}
	}
}