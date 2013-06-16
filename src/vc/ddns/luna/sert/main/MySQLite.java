package vc.ddns.luna.sert.main;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.StringTokenizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLite extends SQLiteOpenHelper {

	private String tag;

	private Random rnd = new Random();

	//コンストラクタ
	public MySQLite(Context context, String tag){
		//任意のデータベースファイル名と、バージョンを指定する
		super(context, "manager.db", null, 1);
		this.tag = tag;

	}

	/**
	 * このデータベースを初めて使用するときに実行される処理
	 * テーブルの作成や初期データの投入を行う
	 */
	@Override
	public void onCreate(SQLiteDatabase db){
		//テーブルを作成
		//資金管理テーブル
		if(tag.equals("fund"))
			db.execSQL(
					"create table fund ("
							+ "_id integer primary key autoincrement not null, "
							+ "date text not null, "
							+ "dayCount INTEGER, "
							+ "cashSum INTEGER, "
							+ "depoSum INTEGER, " + "inOrOut TEXT, " + "cashOrDepo TEXT, " + "category TEXT, "
							+ "comment TEXT, " + "money INTEGER)" );

		setFirstEntry(db, new SimpleDateFormat("yyyy'/'M'/'d").format(new Date()), -2, 0, 0);
	}

	/**
	 * アプリケーションの更新などによって、
	 * データベースのバージョンが上がった場合の処理
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

	}

	//テーブルの初期値設定
	public void setFirstEntry(SQLiteDatabase db, String date,
			int dayCount, int cashSum, int depoSum){
		//挿入するデータはContentValuesに格納
		ContentValues val = new ContentValues();

		if(tag.equals("fund")){
			val.put("date", date); val.put("dayCount", dayCount); val.put("cashSum", cashSum); val.put("depoSum", depoSum);

		}else if(tag.equals("move")){
			val.put("date", date);
			val.put("allSum", 0.0);
			val.put("daySum", 0.0);

			for(int i=0; i<24; i++)
				val.put("hour_"+i, 0);
		}

		//テーブルに１件追加
		db.insert(tag, null, val);
	}

	//初期設定値を読み込む
	public String getFirstValue(SQLiteDatabase db){
		SimpleDateFormat sdf = new SimpleDateFormat();
		Calendar calendar;
		int year, month, day;
		String[] readData;

		Date date = new Date();

		sdf.applyPattern("yyyy");
		year = Integer.parseInt(sdf.format(date));

		sdf.applyPattern("M");
		month = Integer.parseInt(sdf.format(date));

		sdf.applyPattern("d");
		day = Integer.parseInt(sdf.format(date));//今の日にちを取得

		while(true){
			readData = searchByDate(db, new String[]{"date = ?"},
					new String[]{year + "/" + month + "/" + day});
			if(readData.length != 0){
				StringTokenizer st = new StringTokenizer(readData[0], ",");
				st.nextToken();
				if(readData[0] != "" &&
					!st.nextToken().equals("0"))
					break;
				else{}
			}else
				setFirstEntry(db, new SimpleDateFormat("yyyy'/'M'/'d").format(new Date()), 0, 0, 0);

			day--;

			if(day == 0){
				month--;

				if(month == 0){
					month = 12;
					year--;
				}

				calendar = new GregorianCalendar(year, month-1, 1);
				day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			}
		}

		return readData[0];
	}

	//データ追加
	public void inputdateEntry(SQLiteDatabase db, String data){
		int dayCount = 0;
		String[] read;
		StringTokenizer st = new StringTokenizer(data, ",");
		String date = st.nextToken();

		read = searchByDate(db, new String[]{"date = ?"}, new String[]{date});
		if(read.length != 0)
			if(read[read.length-1] != ""){
				StringTokenizer readSt = new StringTokenizer(read[0], ",");
				readSt.nextToken();
			}else{}
		else
			setFirstEntry(db, date, 0, 0, 0);

		dayCount = rnd.nextInt(9998) + 1;

		//更新するデータはContentValuesに格納
		ContentValues val = new ContentValues();

		val.put("date", date); val.put("dayCount", dayCount);
		val.put("cashSum", Integer.parseInt(st.nextToken())); val.put("depoSum", Integer.parseInt(st.nextToken()));
		val.put("inOrOut", st.nextToken()); val.put("cashOrDepo", st.nextToken());
		val.put("category", st.nextToken()); val.put("comment", st.nextToken());
		val.put("money", Integer.parseInt(st.nextToken()));

		//テーブルに１件追加
		db.insert(tag, null, val);
	}

	//データ更新
	public void updateEntry(SQLiteDatabase db, String date, int cashSum, int depoSum){
		ContentValues val = new ContentValues();

		String[] read = searchByDate(db, new String[]{"date = ?"}, new String[]{date});
		StringTokenizer st = new StringTokenizer(read[0], ",");
		st.nextToken();
		String dayCountStr = st.nextToken();

		if(dayCountStr.equals("0"))
			val.put("dayCount", -1);

		val.put("cashSum", cashSum);
		val.put("depoSum", depoSum);

		//データを更新する
		db.update(tag, val, "date = ? and dayCount = ?", new String[]{date, dayCountStr});
	}

	public void updateEntry(SQLiteDatabase db, String[] searchId, String[] searchValue,
			String[] replaceData){
		ContentValues val = new ContentValues();

		if(searchByDate(db, new String[]{"date = ?"}, new String[]{replaceData[0]}).length == 0)
			setFirstEntry(db, replaceData[0], 0, 0, 0);

		String searchStr = "";
		int i=0;
		while(true){
			searchStr += searchId[i];
			i++;
			if(i == searchId.length)
				break;
			else
				searchStr += " and ";
		}

		val.put("date", replaceData[0]);
		val.put("comment", replaceData[1]);
		val.put("money", Integer.parseInt(replaceData[2]));

		//データを更新する
		db.update(tag, val, searchStr, searchValue);
	}

	//データを削除する
	public void deleteEntry(SQLiteDatabase db, String[] searchId, String[] searchValue){
		String searchStr = "";
		int i=0;
		while(true){
			searchStr += searchId[i];
			i++;
			if(i == searchId.length)
				break;
			else
				searchStr += " and ";
		}

		db.delete(tag, searchStr, searchValue);
	}

	//データを検索
	public String[] searchByDate(SQLiteDatabase db, String[] searchId, String[] searchValue){
		//Cursorを確実にcloseするために、finallyを記述する
		Cursor cursor = null;

		String searchStr = "";
		int i=0;
		while(true){
			searchStr += searchId[i];
			i++;
			if(i == searchId.length)
				break;
			else
				searchStr += " and ";
		}

		try{
			//テーブルからデータのセットを検索する
			cursor = db.query(tag, null,
					searchStr, searchValue,
					null, null, null);
			//検索結果をcursorから読み込んで返す
			return readCursor(cursor);

		}finally{
			//Cursorをcloseする
			if(cursor != null)
				cursor.close();
		}
	}

	//検索結果から読み込み
	private String[] readCursor(Cursor cursor){
		String[] result;

		int[] readId = new int[0];
		int recordNum = cursor.getCount();
		int readNum = 0;

		result = new String[recordNum];

		//資金検索
		if(tag.equals("fund")){
			readId = new int[9];

			readId[0] = cursor.getColumnIndex("date");
			readId[1] = cursor.getColumnIndex("dayCount");   readId[2] = cursor.getColumnIndex("cashSum");
			readId[3] = cursor.getColumnIndex("depoSum");    readId[4] = cursor.getColumnIndex("inOrOut");
			readId[5] = cursor.getColumnIndex("cashOrDepo"); readId[6] = cursor.getColumnIndex("category");
			readId[7] = cursor.getColumnIndex("comment");    readId[8] = cursor.getColumnIndex("money");
		}

		if(readId.length != 0){
			while(cursor.moveToNext()){
				result[readNum] = "";
				//データを読み込み文字列にまとめる
				for(int i=0; i<readId.length; i++){
					result[readNum] += cursor.getString(readId[i]) + ",";
				}

				readNum++;
			}
		}
		return result;
	}
}
