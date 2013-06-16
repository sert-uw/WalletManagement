package vc.ddns.luna.sert.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class WalletManagementActivity extends Activity implements OnClickListener {
	private final static int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private final static int MP = ViewGroup.LayoutParams.MATCH_PARENT;

	private static final int REQUEST_GALLERY = 0;
	private static final int REQUEST_FONT1 = 1;
	private static final int REQUEST_FONT2 = 2;

	private boolean changeFlag1, changeFlag2, changeFlag3, replaceFlag, settingFlag;

	private MySQLite sql;//SQLオブジェクト
	private SQLiteDatabase db;//データベース

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'M'/'d");
	private SimpleDateFormat calendarFormat = new SimpleDateFormat("yyyy'/'M");
	private DecimalFormat moneyFormat = new DecimalFormat("#,###");

	private String readData = "";

	private String beforeLayout = "";
	private String beforeLayout2 = "";
	private String settingBeforeLayout = "";

	private String dateStr;

	private List<String> readList = new ArrayList<String>();

	//デザインデータ
	private DesignInit design;

	View layoutView = null;

	//メインレイアウトのデータ
	private TextView info1;
	private TextView info2;
	private int cashSum = 0;
	private int depoSum = 0;

	//詳細レイアウトのデータ
	private int selectedNumber1 = 0;
	private int selectedNumber2 = 0;

	//内訳レイアウトのデータ
	private TextView itemDate;
	private LinearLayout itemLinear;

	//カレンダーレイアウトのデータ
	private String calendarDateStr;
	private TextView calendarDate;

	//検索レイアウトのデータ
	private Spinner dialogSpinner1, dialogSpinner2, dialogSpinner3;
	private EditText dialogComment, dialogYen1, dialogYen2;
	private LinearLayout searchLinear;

	//内訳の詳細レイアウトのデータ
	private TextView itemDetailDate;
	private int listNumber;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		File initFile = this.getFileStreamPath("init.txt");

		try {
			if (!initFile.exists()) {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						openFileOutput("init.txt", MODE_PRIVATE), "Shift-JIS"));
				for (int i = 0; i < 17; i++) {
					bw.write("default");
					bw.newLine();

				}
				bw.close();
			}

			design = new DesignInit(this);
		} catch (IOException e) {
			e.printStackTrace();
		}

		//データベースを開く
		sql = new MySQLite(this, "fund");
		db = sql.getWritableDatabase();

		readData = sql.getFirstValue(db);
		init("main");
	}

	//バックキーが押されたときの処理
	@Override
	public boolean dispatchKeyEvent(KeyEvent e) {
		if (e.getAction() == KeyEvent.ACTION_DOWN) {
			if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				if (!settingFlag) {
					if (!changeFlag1) {
						db.close();
						this.finish();

					} else {
						if (!changeFlag2) {
							init("main");
							return true;

						} else {
							if (!changeFlag3) {
								changeFlag2 = false;
								init(beforeLayout);
								return true;

							} else {
								changeFlag3 = false;
								init(beforeLayout2);
								return true;
							}
						}
					}
				} else {
					settingFlag = false;
					init(settingBeforeLayout);
					return true;
				}
			}
		}
		return super.dispatchKeyEvent(e);
	}

	//メニューの作成
	private final static int MENU_ITEM0 = 0;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem item0 = menu.add(0, MENU_ITEM0, 0, "設定");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_ITEM0:
			settingFlag = true;
			setContentView(R.layout.setting);

			Resources res = getResources();

			findViewById(R.id.settingButton).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					for (int i = 0; i < 17; i++) {
						design.readData[i] = "default";
					}

					design.fontInit();
					design.imageInit();
					design.colorInit();
					design.writeData();
				}
			});

			//各項目のTextViewにクリックリスナーの登録
			for (int i = 1; i < 17; i++) {
				int viewId = res.getIdentifier("settingItem" + i, "id", getPackageName());

				(findViewById(viewId)).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						String tag = v.getTag().toString();
						int tagNumber = Integer.parseInt(tag.replaceAll("[^0-9]", ""));

						if (tagNumber == 1) {
							Intent intent = new Intent();
							intent.setType("image/*");
							intent.setAction(Intent.ACTION_GET_CONTENT);
							startActivityForResult(intent, REQUEST_GALLERY);

						} else if (tagNumber <= 3) {
							Intent intent = new Intent();
							intent.setType("text/*");
							intent.setAction(Intent.ACTION_GET_CONTENT);
							if (tagNumber == 2)
								startActivityForResult(intent, REQUEST_FONT1);
							else if (tagNumber == 3)
								startActivityForResult(intent, REQUEST_FONT2);

						} else {
							settingDialog(
									WalletManagementActivity.this,
									((TextView) v).getText().toString() + "カラー",
									tagNumber,
									design.spinnerNumber[tagNumber - 4], null);
						}
					}
				});
			}
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_GALLERY) {
				design.readData[2] = data.getDataString();
				design.imageInit();
				design.writeData();

			} else if (requestCode == REQUEST_FONT1) {
				design.readData[0] = data.getData().getPath();
				design.fontInit();
				design.writeData();

			} else if (requestCode == REQUEST_FONT2) {
				design.readData[1] = data.getData().getPath();
				design.fontInit();
				design.writeData();
			}
		}
	}

	//ダイアログの表示
	//確認ダイアログ
	private static void showDialog(Context context, String title, String text, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(title);
		ad.setMessage(text);
		ad.setPositiveButton("OK", listener);
		ad.show();
	}

	//削除用ダイアログ
	private static void showDialog(Context context, String title, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(title);
		ad.setPositiveButton("OK", listener);
		ad.setNegativeButton("NO", listener);
		ad.show();
	}

	//設定用ダイアログ(パス入力)
	private void settingDialog(Context context, String title,
			String path, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(title);
		ad.setMessage(path);
		ad.setNegativeButton("キャンセル", listener);
		ad.show();
	}

	//設定用ダイアログ(スピナー)
	private void settingDialog(Context context, String title, int tagNumber,
			int spinnerNumber, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(title);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				context,
				android.R.layout.simple_spinner_item,
				context.getResources().getStringArray(R.array.colorSpinner));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = new Spinner(context);
		spinner.setAdapter(adapter);
		spinner.setSelection(spinnerNumber);
		spinner.setOnItemSelectedListener(design);
		spinner.setTag(tagNumber);
		ad.setView(spinner);
		ad.setPositiveButton("OK", listener);
		ad.show();
	}

	//検索用のダイアログ表示
	private void serchDialog() {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle("検索");
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.wallet_search_dialog, null);
		ad.setView(view);

		view.setBackgroundColor(design.bgColor);

		dialogSpinner1 = (Spinner) view.findViewById(R.id.fundSearchDialogSpinner1);
		dialogSpinner2 = (Spinner) view.findViewById(R.id.fundSearchDialogSpinner2);
		dialogSpinner3 = (Spinner) view.findViewById(R.id.fundSearchDialogSpinner3);
		dialogComment = (EditText) view.findViewById(R.id.fundSearchDialogComment);
		dialogYen1 = (EditText) view.findViewById(R.id.fundSearchDialogYen1);
		dialogYen2 = (EditText) view.findViewById(R.id.fundSearchDialogYen2);

		TextView text = (TextView) view.findViewById(R.id.fundSearchDialogText1);
		text.setTypeface(design.textFont);
		text.setTextColor(design.textColor);

		text = (TextView) view.findViewById(R.id.fundSearchDialogText2);
		text.setTypeface(design.textFont);
		text.setTextColor(design.textColor);

		text = (TextView) view.findViewById(R.id.fundSearchDialogText3);
		text.setTypeface(design.textFont);
		text.setTextColor(design.textColor);

		text = (TextView) view.findViewById(R.id.fundSearchDialogText4);
		text.setTypeface(design.textFont);
		text.setTextColor(design.textColor);

		text = (TextView) view.findViewById(R.id.fundSearchDialogText5);
		text.setTypeface(design.textFont);
		text.setTextColor(design.textColor);

		ad.setPositiveButton("検索",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						getDialogLayoutData();
					}
				});
		ad.show();
	}

	//検索用ダイアログレイアウトのデータ取得
	private void getDialogLayoutData() {
		int length = 0;

		String str1 = (String) dialogSpinner1.getSelectedItem();
		String str2 = (String) dialogSpinner2.getSelectedItem();
		String str3 = (String) dialogSpinner3.getSelectedItem();
		String comment = (String) dialogComment.getText().toString();
		String yen1 = (String) dialogYen1.getText().toString();
		String yen2 = (String) dialogYen2.getText().toString();

		if (!str1.equals("---"))
			length++;
		if (!str2.equals("---"))
			length++;
		if (!str3.equals("---"))
			length++;
		if (!comment.equals(""))
			length++;
		if (!yen1.equals(""))
			length++;
		if (!yen2.equals(""))
			length++;

		if (length == 0)
			return;

		String[] searchId = new String[length];
		String[] searchValue = new String[length];
		length = 0;

		if (!str1.equals("---")) {
			searchId[length] = "inOrOut = ?";
			searchValue[length] = str1;
			length++;
		}
		if (!str2.equals("---")) {
			searchId[length] = "cashOrDepo = ?";
			searchValue[length] = str2;
			length++;
		}
		if (!str3.equals("---")) {
			searchId[length] = "category = ?";
			searchValue[length] = str3;
			length++;
		}
		if (!comment.equals("")) {
			searchId[length] = "comment like ?";
			searchValue[length] = "%" + comment + "%";
			length++;
		}
		if (!yen1.equals("")) {
			searchId[length] = "money >= ?";
			searchValue[length] = yen1;
			length++;
		}
		if (!yen2.equals("")) {
			searchId[length] = "money <= ?";
			searchValue[length] = yen2;
			length++;
		}

		setItemData(searchLinear, searchId, searchValue);
	}

	//各種初期化処理
	public void init(String tag) {

		TextView title = null;
		LayoutInflater inflater = LayoutInflater.from(this);
		settingBeforeLayout = tag;

		//////////////////////////////////////////////////////
		//メイン画面のレイアウト//////////////////////////////
		//////////////////////////////////////////////////////
		if (tag.equals("main")) {
			changeFlag1 = false;
			layoutView = inflater.inflate(R.layout.wallet_main, null);
			title = (TextView) layoutView.findViewById(R.id.fundMainTitle);
			info1 = (TextView) layoutView.findViewById(R.id.fundMainInfo1);
			info2 = (TextView) layoutView.findViewById(R.id.fundMainInfo2);
			info1.setTypeface(design.textFont);
			info1.setTextColor(design.infoColor);
			info2.setTypeface(design.textFont);
			info2.setTextColor(design.infoColor);

			TextView text = (TextView) layoutView.findViewById(R.id.fundMainSubTitle1);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundMainSubTitle2);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundMainYen1);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundMainYen2);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			layoutView.findViewById(R.id.fundMainCashButton).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundMainCashButton)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundMainDepoButton).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundMainDepoButton)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundMainDetails).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundMainDetails)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundMainItems).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundMainItems)).setTextColor(design.buttonColor);

			readData = sql.getFirstValue(db);
			StringTokenizer st = new StringTokenizer(readData, ",");
			st.nextToken();
			st.nextToken();
			cashSum = Integer.parseInt(st.nextToken());
			depoSum = Integer.parseInt(st.nextToken());
			info1.setText(moneyFormat.format(cashSum));
			info2.setText(moneyFormat.format(depoSum));
		}

		///////////////////////////////////////////////////////
		//詳細画面のレイアウト/////////////////////////////////
		///////////////////////////////////////////////////////
		else if (tag.equals("detail")) {
			changeFlag1 = true;
			beforeLayout = "detail";
			layoutView = inflater.inflate(R.layout.wallet_detail, null);
			title = (TextView) layoutView.findViewById(R.id.fundDetailTitle);

			layoutView.findViewById(R.id.fundDetailButton).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundDetailButton)).setTextColor(design.buttonColor);

			layoutView.findViewById(R.id.fundDetailDate).setOnClickListener(this);
			((TextView) layoutView.findViewById(R.id.fundDetailDate)).setText(dateStr);
			((TextView) layoutView.findViewById(R.id.fundDetailDate)).setTypeface(design.textFont);
			((TextView) layoutView.findViewById(R.id.fundDetailDate)).setTextColor(design.dateColor);

			TextView text = (TextView) layoutView.findViewById(R.id.fundDetailSubTitle1);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundDetailSubTitle2);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundDetailSubTitle3);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundDetailSubTitle4);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundDetailYen);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			selectedNumber1 = 0;
			selectedNumber2 = 0;

			setDetailSpinnerIvent((Spinner) layoutView.findViewById(R.id.fundDetailSpinner1));
			setDetailSpinnerIvent((Spinner) layoutView.findViewById(R.id.fundDetailSpinner2));
			setDetailSpinnerIvent((Spinner) layoutView.findViewById(R.id.fundDetailSpinner3));
		}

		/////////////////////////////////////////////////////////
		//内訳画面のレイアウト///////////////////////////////////
		/////////////////////////////////////////////////////////
		else if (tag.equals("item")) {
			changeFlag1 = true;
			beforeLayout = "item";
			layoutView = inflater.inflate(R.layout.wallet_item, null);
			title = (TextView) layoutView.findViewById(R.id.fundItemTitle);
			layoutView.findViewById(R.id.fundItemSearch).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundItemSearch)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundItemLeft).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundItemLeft)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundItemRight).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundItemRight)).setTextColor(design.buttonColor);

			layoutView.findViewById(R.id.fundItemDate).setOnClickListener(this);
			((TextView) layoutView.findViewById(R.id.fundItemDate)).setTypeface(design.textFont);
			((TextView) layoutView.findViewById(R.id.fundItemDate)).setTextColor(design.dateColor);

			TextView text = (TextView) layoutView.findViewById(R.id.fundItemSubTitle);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			setItemSpinnerIvent((Spinner) layoutView.findViewById(R.id.fundItemSpinner1));
			setItemSpinnerIvent((Spinner) layoutView.findViewById(R.id.fundItemSpinner2));
			setItemSpinnerIvent((Spinner) layoutView.findViewById(R.id.fundItemSpinner3));

			itemDate = (TextView) layoutView.findViewById(R.id.fundItemDate);
			itemDate.setText(dateStr);
			itemLinear = (LinearLayout) layoutView.findViewById(R.id.fundItemLinear);
			setItemData(itemLinear, new String[] { "date = ?" }, new String[] { itemDate.getText().toString() });
		}

		///////////////////////////////////////////////////////////
		//検索画面のレイアウト/////////////////////////////////////
		///////////////////////////////////////////////////////////
		else if (tag.equals("search")) {
			changeFlag2 = true;
			beforeLayout2 = "search";
			layoutView = inflater.inflate(R.layout.wallet_search, null);
			title = (TextView) layoutView.findViewById(R.id.fundSearchTitle);
			layoutView.findViewById(R.id.fundSearchButton).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundSearchButton)).setTextColor(design.buttonColor);
			searchLinear = (LinearLayout) layoutView.findViewById(R.id.fundSearchLinear);
		}

		////////////////////////////////////////////////////////////
		//カレンダー画面のレイアウト////////////////////////////////
		////////////////////////////////////////////////////////////
		else if (tag.equals("date")) {
			changeFlag2 = true;
			layoutView = inflater.inflate(R.layout.calendar, null);
			title = (TextView) layoutView.findViewById(R.id.calendarTitle);
			layoutView.findViewById(R.id.calendarLeft).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.calendarLeft)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.calendarRight).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.calendarRight)).setTextColor(design.buttonColor);

			calendarDate = (TextView) layoutView.findViewById(R.id.calendarDate);
			calendarDate.setText(calendarDateStr);
			calendarDate.setTypeface(design.textFont);
			calendarDate.setTextColor(design.dateColor);

			TextView text = (TextView) layoutView.findViewById(R.id.calendarSun);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.calendarMon);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.calendarTue);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.calendarWed);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.calendarThu);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.calendarFri);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.calendarSat);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			setCalendarButton();
		}

		//////////////////////////////////////////////////////////////
		//内訳詳細画面のレイアウト////////////////////////////////////
		//////////////////////////////////////////////////////////////
		else if (tag.equals("itemDetail")) {
			layoutView = inflater.inflate(R.layout.wallet_item_derail, null);
			title = (TextView) layoutView.findViewById(R.id.fundItemDetailTitle);
			itemDetailDate = (TextView) layoutView.findViewById(R.id.fundItemDetailDate);
			itemDetailDate.setOnClickListener(this);
			itemDetailDate.setTypeface(design.textFont);
			itemDetailDate.setTextColor(design.dateColor);

			layoutView.findViewById(R.id.fundItemDetailButton1).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundItemDetailButton1)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundItemDetailButton2).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundItemDetailButton2)).setTextColor(design.buttonColor);
			layoutView.findViewById(R.id.fundItemDetailButton3).setOnClickListener(this);
			((Button) layoutView.findViewById(R.id.fundItemDetailButton3)).setTextColor(design.buttonColor);

			TextView text = (TextView) layoutView.findViewById(R.id.fundItemDetailIO);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundItemDetailCD);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			text = (TextView) layoutView.findViewById(R.id.fundItemDetailCA);
			text.setTypeface(design.textFont);
			text.setTextColor(design.textColor);

			setItemDetailData();
		}

		setContentView(layoutView);

		layoutView.setBackgroundColor(design.bgColor);
		layoutView.setBackgroundDrawable(design.bgim);
		title.setTypeface(design.titleFont);
		title.setTextColor(design.titleColor);
	}

	//内訳詳細レイアウトのデータセット
	public void setItemDetailData() {
		StringTokenizer st = new StringTokenizer(readList.get(listNumber), ",");
		itemDetailDate.setText(st.nextToken());

		if (replaceFlag) {
			itemDetailDate.setText(dateStr);
			replaceFlag = false;
		}

		st.nextToken();
		st.nextToken();
		st.nextToken();

		((TextView) layoutView.findViewById(R.id.fundItemDetailIO)).setText(st.nextToken());
		((TextView) layoutView.findViewById(R.id.fundItemDetailCD)).setText(st.nextToken());
		((TextView) layoutView.findViewById(R.id.fundItemDetailCA)).setText(st.nextToken());

		((EditText) layoutView.findViewById(R.id.fundItemDetailComment)).setText(st.nextToken());
		((EditText) layoutView.findViewById(R.id.fundItemDetailInput)).setText(st.nextToken());
	}

	//クリック時の処理
	@Override
	public void onClick(View v) {
		String tag = (String) v.getTag();

		/////////////////////////////////////////////////////
		//mainレイアウトの処理
		/////////////////////////////////////////////////////
		if (tag.equals("cashRegister")) {
			EditText input1 = (EditText) layoutView.findViewById(R.id.fundMainInput1);
			int num = Integer.parseInt(input1.getText().toString());

			cashSum -= num;
			String inputData = sdf.format(new Date()) + "," + cashSum + "," + depoSum + "," +
					"支出" + "," + "現金" + "," + "通常" + "," + " " + "," + num + ",";
			sql.inputdateEntry(db, inputData);
			sql.updateEntry(db, sdf.format(new Date()), cashSum, depoSum);
			info1.setText(moneyFormat.format(cashSum));

		} else if (tag.equals("depoRegister")) {
			EditText input2 = (EditText) layoutView.findViewById(R.id.fundMainInput2);
			int num = Integer.parseInt(input2.getText().toString());

			cashSum += num;
			depoSum -= num;

			String inputData = sdf.format(new Date()) + "," + cashSum + "," + depoSum + "," +
					"支出" + "," + "預金" + "," + "引出" + "," + " " + "," + num + ",";
			sql.inputdateEntry(db, inputData);
			sql.updateEntry(db, sdf.format(new Date()), cashSum, depoSum);
			info1.setText(moneyFormat.format(cashSum));
			info2.setText(moneyFormat.format(depoSum));

		} else if (tag.equals("detail")) {
			dateStr = sdf.format(new Date());
			init(tag);

		} else if (tag.equals("item")) {
			dateStr = sdf.format(new Date());
			init(tag);

		}

		////////////////////////////////////////////////////
		//detailレイアウトの処理
		////////////////////////////////////////////////////
		else if (tag.equals("detailRegister")) {
			String inOrOut, cashOrDepo, category;
			EditText input = (EditText) layoutView.findViewById(R.id.fundDetailInput);
			EditText comEdit = (EditText) layoutView.findViewById(R.id.fundDetailCommentInput);
			String date = ((TextView) layoutView.findViewById(R.id.fundDetailDate)).getText().toString();

			Spinner spinner = (Spinner) layoutView.findViewById(R.id.fundDetailSpinner1);
			inOrOut = (String) spinner.getSelectedItem();

			spinner = (Spinner) layoutView.findViewById(R.id.fundDetailSpinner2);
			cashOrDepo = (String) spinner.getSelectedItem();

			spinner = (Spinner) layoutView.findViewById(R.id.fundDetailSpinner3);
			category = (String) spinner.getSelectedItem();

			if (inOrOut.equals("---") || cashOrDepo.equals("---") ||
					category.equals("---") || input.getText() == null) {
				showDialog(this, "入力エラー", "コメント以外は必ず入力してください", null);
				return;
			}

			int num = Integer.parseInt(input.getText().toString());

			String comment = comEdit.getText().toString();
			String inputData = date + ",";

			if (inOrOut.equals("収入")) {
				if (cashOrDepo.equals("現金")) {
					if (category.equals("通常")) {
						cashSum += num;
						inputData += cashSum + "," + depoSum + ",";

					} else if (category.equals("引出")) {
						cashSum += num;
						depoSum -= num;
						inputData += cashSum + "," + depoSum + ",";

					}

				} else if (cashOrDepo.equals("預金")) {
					if (category.equals("通常")) {
						depoSum += num;
						inputData += cashSum + "," + depoSum + ",";

					} else if (category.equals("預入")) {
						cashSum -= num;
						depoSum += num;
						inputData += cashSum + "," + depoSum + ",";

					}
				} else {
					return;
				}

			} else if (inOrOut.equals("支出")) {
				if (cashOrDepo.equals("現金")) {
					if (category.equals("通常")) {
						cashSum -= num;
						inputData += cashSum + "," + depoSum + ",";

					} else if (category.equals("預入")) {
						cashSum -= num;
						depoSum += num;
						inputData += cashSum + "," + depoSum + ",";

					}

				} else if (cashOrDepo.equals("預金")) {
					if (category.equals("通常")) {
						depoSum -= num;
						inputData += cashSum + "," + depoSum + ",";

					} else if (category.equals("引出")) {
						cashSum += num;
						depoSum -= num;
						inputData += cashSum + "," + depoSum + ",";

					}
				} else {
					return;
				}

			} else {
				return;
			}

			inputData += inOrOut + "," + cashOrDepo + "," + category + "," + comment + "," + num + ",";
			sql.inputdateEntry(db, inputData);
			sql.updateEntry(db, sdf.format(new Date()), cashSum, depoSum);
			init("main");

		} else if (tag.equals("detailDate")) {
			calendarDateStr = calendarFormat.format(new Date());
			init("date");
		}

		/////////////////////////////////////////////
		//itemレイアウトの処理
		/////////////////////////////////////////////
		else if (tag.equals("search")) {
			serchDialog();
			init(tag);

		} else if (tag.equals("left") || tag.equals("right")) {
			changeDate(tag);

		} else if (tag.equals("date")) {
			calendarDateStr = calendarFormat.format(new Date());
			init(tag);
		}

		///////////////////////////////////////////////
		//searchレイアウトの処理
		///////////////////////////////////////////////
		else if (tag.equals("reSearch")) {
			serchDialog();
		}

		////////////////////////////////////////////////
		//calendarレイアウトの処理
		////////////////////////////////////////////////
		else if (tag.equals("calendarLeft") || tag.equals("calendarRight")) {
			if (calendarDateStr == null)
				calendarDateStr = calendarFormat.format(new Date());

			int year = Integer.parseInt(calendarDateStr.substring(0, calendarDateStr.indexOf("/")));
			int month = Integer.parseInt(calendarDateStr.substring(calendarDateStr.indexOf("/") + 1));

			if (tag.equals("calendarLeft"))
				month--;
			else if (tag.equals("calendarRight"))
				month++;

			if (month == 0) {
				month = 12;
				year--;
			} else if (month == 13) {
				month = 1;
				year++;
			}

			calendarDateStr = year + "/" + month;
			calendarDate.setText(calendarDateStr);
			setCalendarButton();
		}

		/////////////////////////////////////////////////
		//内訳詳細レイアウトの処理
		/////////////////////////////////////////////////
		else if (tag.equals("itemDetailRevise")) {
			reviseData();

			if (!changeFlag3) {
				init(beforeLayout);
				changeFlag2 = false;

			} else {
				init(beforeLayout2);
				changeFlag3 = false;
			}

		} else if (tag.equals("itemDetailReturn")) {
			if (!changeFlag3) {
				init(beforeLayout);
				changeFlag2 = false;

			} else {
				init(beforeLayout2);
				changeFlag3 = false;
			}

		} else if (tag.equals("itemDetailDelete")) {
			showDialog(this, "削除しますか？", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == DialogInterface.BUTTON_POSITIVE) {
						deleteData();

						if (!changeFlag3) {
							init(beforeLayout);
							changeFlag2 = false;

						} else {
							init(beforeLayout2);
							changeFlag3 = false;
						}
					}
				}
			});

		} else if (tag.equals("itemDetailDate")) {
			replaceFlag = true;
			calendarDateStr = calendarFormat.format(new Date());
			init("date");
		}

		/////////////////////////////////////////////////
		//内訳の内容１つ１つの処理
		/////////////////////////////////////////////////
		else {
			if (tag.indexOf("readList") != -1) {
				listNumber = Integer.parseInt(tag.replaceAll("[^0-9]", ""));
				if (changeFlag2)
					changeFlag3 = true;
				else
					changeFlag2 = true;
				init("itemDetail");
			}
		}
	}

	private static final int textSize = 25;

	//内訳の表示
	public void setItemData(LinearLayout linear, String[] searchId, String[] searchValue) {
		if (linear == null)
			return;

		linear.removeAllViews();
		readList.clear();

		String inOrOut = null, cashOrDepo = null, category = null;
		if (linear == itemLinear) {
			Spinner spinner = (Spinner) layoutView.findViewById(R.id.fundItemSpinner1);
			inOrOut = (String) spinner.getSelectedItem();

			spinner = (Spinner) layoutView.findViewById(R.id.fundItemSpinner2);
			cashOrDepo = (String) spinner.getSelectedItem();

			spinner = (Spinner) layoutView.findViewById(R.id.fundItemSpinner3);
			category = (String) spinner.getSelectedItem();
		}
		String[] readData = sql.searchByDate(db, searchId, searchValue);
		if (linear == itemLinear)
			if (readData.length <= 1)
				return;
		RelativeLayout[] relative = new RelativeLayout[readData.length];
		TextView[] inOutText = new TextView[readData.length];
		TextView[] cashDepoText = new TextView[readData.length];
		TextView[] categoryText = new TextView[readData.length];
		TextView[] moneyText = new TextView[readData.length];
		TextView[] commentText = new TextView[readData.length];

		RelativeLayout.LayoutParams[] ioParam = new RelativeLayout.LayoutParams[readData.length];
		RelativeLayout.LayoutParams[] cdParam = new RelativeLayout.LayoutParams[readData.length];
		RelativeLayout.LayoutParams[] cateParam = new RelativeLayout.LayoutParams[readData.length];
		RelativeLayout.LayoutParams[] monParam = new RelativeLayout.LayoutParams[readData.length];
		RelativeLayout.LayoutParams[] comParam = new RelativeLayout.LayoutParams[readData.length];

		String str;
		StringTokenizer st;

		int setCounter = 0;
		for (int i = 0; i < readData.length; i++) {
			inOutText[i] = new TextView(this);
			cashDepoText[i] = new TextView(this);
			categoryText[i] = new TextView(this);
			moneyText[i] = new TextView(this);
			commentText[i] = new TextView(this);

			inOutText[i].setTextSize(textSize);
			cashDepoText[i].setTextSize(textSize);
			categoryText[i].setTextSize(textSize);
			moneyText[i].setTextSize(textSize);
			commentText[i].setTextSize(textSize);

			st = new StringTokenizer(readData[i], ",");

			st.nextToken();
			if (Integer.parseInt(st.nextToken()) <= 0)
				continue;
			st.nextToken();
			st.nextToken();

			str = st.nextToken();
			inOutText[i].setText(str.substring(0, 1) + "\n" + str.substring(1));

			if (str.equals("支出")) {
				inOutText[i].setTextColor(design.outputColor);
				moneyText[i].setTextColor(design.moneyColor2);
			} else if (str.equals("収入")) {
				inOutText[i].setTextColor(design.inputColor);
				moneyText[i].setTextColor(design.moneyColor1);
			}

			str = st.nextToken();
			cashDepoText[i].setText(str.substring(0, 1) + "\n" + str.substring(1));

			if (str.equals("現金"))
				cashDepoText[i].setTextColor(design.cashColor);
			else if (str.equals("預金"))
				cashDepoText[i].setTextColor(design.depoColor);

			categoryText[i].setText(st.nextToken());
			categoryText[i].setTextColor(design.categoryColor);

			commentText[i].setText(st.nextToken());
			commentText[i].setTextColor(design.commentColor);

			moneyText[i].setText(st.nextToken() + "円");

			if (linear == itemLinear) {
				if (!inOrOut.equals("---") || !cashOrDepo.equals("---") || !category.equals("---")) {
					if (!inOrOut.equals("---") &&
							!inOutText[i].getText().toString().
									equals(inOrOut.substring(0, 1) + "\n" + inOrOut.substring(1)))
						continue;

					if (!cashOrDepo.equals("---") &&
							!cashDepoText[i].getText().toString().
									equals(cashOrDepo.substring(0, 1) + "\n" + cashOrDepo.substring(1)))
						continue;

					if (!category.equals("---") &&
							!categoryText[i].getText().toString().equals(category))
						continue;
				}
			}

			ioParam[i] = new RelativeLayout.LayoutParams(WC, WC);
			cdParam[i] = new RelativeLayout.LayoutParams(WC, WC);
			cateParam[i] = new RelativeLayout.LayoutParams(WC, WC);
			monParam[i] = new RelativeLayout.LayoutParams(WC, WC);
			comParam[i] = new RelativeLayout.LayoutParams(WC, WC);

			relative[i] = new RelativeLayout(this);
			relative[i].setBackgroundColor(Color.DKGRAY);
			relative[i].setOnClickListener(this);
			relative[i].setTag("readList" + setCounter);
			setCounter++;
			readList.add(readData[i]);

			ioParam[i].addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			inOutText[i].setId(1);
			relative[i].addView(inOutText[i], ioParam[i]);

			cashDepoText[i].setId(2);
			cdParam[i].addRule(RelativeLayout.RIGHT_OF, inOutText[i].getId());
			relative[i].addView(cashDepoText[i], cdParam[i]);

			categoryText[i].setId(3);
			cateParam[i].addRule(RelativeLayout.RIGHT_OF, cashDepoText[i].getId());
			relative[i].addView(categoryText[i], cateParam[i]);

			moneyText[i].setId(4);
			monParam[i].addRule(RelativeLayout.RIGHT_OF, cashDepoText[i].getId());
			monParam[i].addRule(RelativeLayout.BELOW, categoryText[i].getId());
			relative[i].addView(moneyText[i], monParam[i]);

			commentText[i].setId(5);
			commentText[i].setGravity(Gravity.RIGHT);
			comParam[i].addRule(RelativeLayout.RIGHT_OF, moneyText[i].getId());
			comParam[i].addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			comParam[i].setMargins(20, 0, 0, 0);
			relative[i].addView(commentText[i], comParam[i]);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MP, WC);
			params.setMargins(0, 50, 0, 0);
			linear.addView(relative[i], params);
		}
	}

	//内訳での日付変更
	public void changeDate(String tag) {
		Calendar calendar;
		String nowDate = itemDate.getText().toString();
		int year = Integer.parseInt(nowDate.substring(0, nowDate.indexOf("/")));
		int month = Integer.parseInt(nowDate.substring(nowDate.indexOf("/") + 1,
				nowDate.lastIndexOf("/")));
		int day = Integer.parseInt(nowDate.substring(nowDate.lastIndexOf("/") + 1));

		if (tag.equals("left")) {
			day--;
			if (day == 0) {
				month--;
				if (month == 0) {
					month = 12;
					year--;
				}
				calendar = new GregorianCalendar(year, month - 1, 1);
				day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			}

		} else if (tag.equals("right")) {
			calendar = new GregorianCalendar(year, month - 1, 1);
			day++;
			if (day == calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1) {
				day = 1;
				month++;
				if (month == 13) {
					month = 1;
					year++;
				}
			}
		}

		dateStr = year + "/" + month + "/" + day;
		itemDate.setText(dateStr);
		setItemData(itemLinear, new String[] { "date = ?" }, new String[] { itemDate.getText().toString() });
	}

	//スピナーイベント追加
	public void setDetailSpinnerIvent(Spinner spinner) {
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				String tag = parent.getTag().toString();
				ArrayAdapter<String> adapter;

				if (tag.equals("spinner1")) {
					selectedNumber1 = position;

				} else if (tag.equals("spinner2")) {
					selectedNumber2 = position;

				} else if (tag.equals("spinner3")) {
					return;
				}

				if (selectedNumber1 != 0 && selectedNumber2 != 0) {
					if (selectedNumber1 == 1) {
						if (selectedNumber2 == 1) {
							adapter = new ArrayAdapter<String>(
									WalletManagementActivity.this,
									android.R.layout.simple_spinner_item,
									getResources().getStringArray(R.array.fundDetailSpinner3_sub1));
							adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
							((Spinner) layoutView.findViewById(R.id.fundDetailSpinner3)).setAdapter(adapter);
						} else {
							adapter = new ArrayAdapter<String>(
									WalletManagementActivity.this,
									android.R.layout.simple_spinner_item,
									getResources().getStringArray(R.array.fundDetailSpinner3_sub2));
							adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
							((Spinner) layoutView.findViewById(R.id.fundDetailSpinner3)).setAdapter(adapter);
						}

					} else {
						if (selectedNumber2 == 1) {
							adapter = new ArrayAdapter<String>(
									WalletManagementActivity.this,
									android.R.layout.simple_spinner_item,
									getResources().getStringArray(R.array.fundDetailSpinner3_sub2));
							adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
							((Spinner) layoutView.findViewById(R.id.fundDetailSpinner3)).setAdapter(adapter);
						} else {
							adapter = new ArrayAdapter<String>(
									WalletManagementActivity.this,
									android.R.layout.simple_spinner_item,
									getResources().getStringArray(R.array.fundDetailSpinner3_sub1));
							adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
							((Spinner) layoutView.findViewById(R.id.fundDetailSpinner3)).setAdapter(adapter);
						}
					}
				} else {
					adapter = new ArrayAdapter<String>(
							WalletManagementActivity.this,
							android.R.layout.simple_spinner_item,
							getResources().getStringArray(R.array.fundDetailSpinner3_sub3));
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					((Spinner) layoutView.findViewById(R.id.fundDetailSpinner3)).setAdapter(adapter);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	public void setItemSpinnerIvent(Spinner spinner) {
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setItemData(itemLinear, new String[] { "date = ?" }, new String[] { itemDate.getText().toString() });
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	//カレンダーレイアウトでの処理
	public void setCalendarButton() {
		Resources res = getResources();
		int viewId;
		int year = Integer.parseInt(calendarDateStr.substring(0, calendarDateStr.indexOf("/")));
		int month = Integer.parseInt(calendarDateStr.substring(calendarDateStr.indexOf("/") + 1));
		int dayOfWeekNum = getDayOfWeek(year, month);
		Calendar calendar = new GregorianCalendar(year, month - 1, 1);

		for (int i = 1; i < 43; i++) {
			viewId = res.getIdentifier("calendarButton" + i, "id", getPackageName());
			Button button = (Button) layoutView.findViewById(viewId);

			if (i - dayOfWeekNum <= 0 ||
					(i - dayOfWeekNum) >= calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1) {
				button.setText("");
				button.setTag("");
			} else {
				button.setText("" + (i - dayOfWeekNum));
				button.setTag(calendarDateStr + "/" + (i - dayOfWeekNum));
				if (sql.searchByDate(db, new String[] { "date = ?" },
						new String[] { calendarDateStr + "/" + (i - dayOfWeekNum) }).length > 1)
					button.setTextColor(Color.BLUE);
			}

			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String tag = v.getTag().toString();
					if (tag != "") {
						dateStr = tag;
						if (!replaceFlag) {
							init(beforeLayout);
							changeFlag2 = false;
						} else {
							init("itemDetail");
						}
					}
				}
			});
		}
	}

	//各月の１日が何曜日かによって数値を返す
	public int getDayOfWeek(int year, int month) {
		Calendar calendar = new GregorianCalendar(year, month - 1, 1);

		int dayOfWeekNum = 0;

		switch (calendar.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.SUNDAY:
			dayOfWeekNum = 0;
			break;
		case Calendar.MONDAY:
			dayOfWeekNum = 1;
			break;
		case Calendar.TUESDAY:
			dayOfWeekNum = 2;
			break;
		case Calendar.WEDNESDAY:
			dayOfWeekNum = 3;
			break;
		case Calendar.THURSDAY:
			dayOfWeekNum = 4;
			break;
		case Calendar.FRIDAY:
			dayOfWeekNum = 5;
			break;
		case Calendar.SATURDAY:
			dayOfWeekNum = 6;
			break;
		}

		return dayOfWeekNum;
	}

	//データ修正
	public void reviseData() {
		String[] searchId = new String[6];
		String[] searchValue = new String[6];
		String[] replaceData = new String[3];
		StringTokenizer st = new StringTokenizer(readList.get(listNumber), ",");

		searchId[0] = "date = ?";
		searchId[1] = "dayCount = ?";
		searchId[2] = "inOrOut like ?";
		searchId[3] = "cashOrDepo like ?";
		searchId[4] = "category like ?";
		searchId[5] = "money = ?";

		searchValue[0] = st.nextToken();
		searchValue[1] = st.nextToken();
		st.nextToken();
		st.nextToken();
		searchValue[2] = st.nextToken();
		searchValue[3] = st.nextToken();
		searchValue[4] = st.nextToken();
		st.nextToken();
		searchValue[5] = st.nextToken();

		replaceData[0] = itemDetailDate.getText().toString();
		replaceData[1] = ((EditText) layoutView.findViewById(R.id.fundItemDetailComment)).getText().toString();
		replaceData[2] = ((EditText) layoutView.findViewById(R.id.fundItemDetailInput)).getText().toString();

		if (replaceData[1].equals(""))
			replaceData[1] = " ";

		sql.updateEntry(db, searchId, searchValue, replaceData);

		int oldM = Integer.parseInt(searchValue[5]);
		int newM = Integer.parseInt(replaceData[2]);

		if (searchValue[2].equals("収入")) {
			if (searchValue[3].equals("現金")) {
				cashSum += (newM - oldM);

				if (searchValue[4].equals("引出")) {
					depoSum -= (newM - oldM);
				}

			} else if (searchValue[3].equals("預金")) {
				depoSum += (newM - oldM);

				if (searchValue[4].equals("預入")) {
					cashSum -= (newM - oldM);
				}
			}

		} else if (searchValue[2].equals("支出")) {
			if (searchValue[3].equals("現金")) {
				cashSum -= (newM - oldM);

				if (searchValue[4].equals("預入")) {
					depoSum += (newM - oldM);
				}

			} else if (searchValue[3].equals("預金")) {
				depoSum -= (newM - oldM);

				if (searchValue[4].equals("引出")) {
					cashSum += (newM - oldM);
				}
			}
		}

		sql.updateEntry(db, sdf.format(new Date()), cashSum, depoSum);
	}

	//データ削除
	public void deleteData() {
		String[] searchId = new String[6];
		String[] searchValue = new String[6];
		StringTokenizer st = new StringTokenizer(readList.get(listNumber), ",");

		searchId[0] = "date = ?";
		searchId[1] = "dayCount = ?";
		searchId[2] = "inOrOut like ?";
		searchId[3] = "cashOrDepo like ?";
		searchId[4] = "category like ?";
		searchId[5] = "money = ?";

		searchValue[0] = st.nextToken();
		searchValue[1] = st.nextToken();
		st.nextToken();
		st.nextToken();
		searchValue[2] = st.nextToken();
		searchValue[3] = st.nextToken();
		searchValue[4] = st.nextToken();
		st.nextToken();
		searchValue[5] = st.nextToken();

		sql.deleteEntry(db, searchId, searchValue);

		int oldM = Integer.parseInt(searchValue[5]);
		int newM = 0;

		if (searchValue[2].equals("収入")) {
			if (searchValue[3].equals("現金")) {
				cashSum += (newM - oldM);

				if (searchValue[4].equals("引出")) {
					depoSum -= (newM - oldM);
				}

			} else if (searchValue[3].equals("預金")) {
				depoSum += (newM - oldM);

				if (searchValue[4].equals("預入")) {
					cashSum -= (newM - oldM);
				}
			}

		} else if (searchValue[2].equals("支出")) {
			if (searchValue[3].equals("現金")) {
				cashSum -= (newM - oldM);

				if (searchValue[4].equals("預入")) {
					depoSum += (newM - oldM);
				}

			} else if (searchValue[3].equals("預金")) {
				depoSum -= (newM - oldM);

				if (searchValue[4].equals("引出")) {
					cashSum += (newM - oldM);
				}
			}
		}

		sql.updateEntry(db, sdf.format(new Date()), cashSum, depoSum);
	}
}