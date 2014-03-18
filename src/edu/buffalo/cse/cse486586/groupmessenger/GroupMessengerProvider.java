package edu.buffalo.cse.cse486586.groupmessenger;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;
import android.database.sqlite.*;
/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */

public class GroupMessengerProvider extends ContentProvider {

	DBHelper db;
	private SQLiteDatabase OurDatabase;

	//Database declarations
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";

	private static final String DATABASE_NAME="GroupMessenger.db"; 
	public static final String DATABASE_TABLE = "Messages";

	private static class DBHelper extends SQLiteOpenHelper{

		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null,1);
//			Log.v("constructor", "inside the constructor");
			// TODO Auto-generated constructor stub;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" +
					KEY_FIELD + " TEXT NOT NULL, " + 
					VALUE_FIELD + " TEXT NOT NULL);"
					);
			Log.v("oncreate", " after craete table ");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			/*			db.execSQL("DROP TABLE IF EXITS " + DATABASE_NAME);
			onCreate(db);
			 */		}

	}  

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// You do not need to implement this.
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		/*
		 * TODO: You need to implement this method. Note that values will have two columns (a key
		 * column and a value column) and one row that contains the actual (key, value) pair to be
		 * inserted.
		 * 
		 * For actual storage, you can use any option. If you know how to use SQL, then you can use
		 * SQLite. But this is not a requirement. You can use other storage options, such as the
		 * internal storage option that I used in PA1. If you want to use that option, please
		 * take a look at the code for PA1.
		 */
		long rowid=OurDatabase.insert(DATABASE_TABLE, null, values);		
		if(rowid>0)
		{
			Uri newuri=ContentUris.withAppendedId(uri, rowid);	
			return newuri;
		}		
		Log.e("insert", values.toString());
		throw new SQLException("Insert failed");
	}

	@Override
	public boolean onCreate() {
		Context context = getContext();
		db=new DBHelper(context);
		OurDatabase=db.getWritableDatabase();
		OurDatabase.execSQL("DROP TABLE IF EXISTS "+ DATABASE_TABLE );
		db.onCreate(OurDatabase);
		if(db == null)
			return false;
		else
			return true; 
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		/*
		 * TODO: You need to implement this method. Note that you need to return a Cursor object
		 * with the right format. If the formatting is not correct, then it is not going to work.
		 * 
		 * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
		 * still need to be careful because the formatting might still be incorrect.
		 * 
		 * If you use a file storage option, then it is your job to build a Cursor * object. I
		 * recommend building a MatrixCursor described at:
		 * http://developer.android.com/reference/android/database/MatrixCursor.html
		 */
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(DATABASE_TABLE);
		queryBuilder.appendWhere("key = '" + selection +"'");
		Cursor cursor = queryBuilder.query(OurDatabase, projection, null,
				null, null, null, sortOrder);
		return cursor;		
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}

}
