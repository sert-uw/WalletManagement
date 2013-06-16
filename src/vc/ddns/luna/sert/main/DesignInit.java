package vc.ddns.luna.sert.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

public class DesignInit implements OnItemSelectedListener{
	private WalletManagementActivity context;

	public Typeface titleFont;
	public Typeface textFont;

	public String[] readData; //ファイルから読み込んだデータ

	//initファイルに書かれている順番
	public String titleFontPath;//タイトル用フォント
	public String textFontPath; //テキスト用フォント
	public BitmapDrawable bgim; //背景

	public int bgColor; //背景色
	public int titleColor; //タイトルカラー
	public int textColor; //テキストカラー
	public int infoColor; //タイトルの金額カラー
	public int buttonColor; //ボタンの文字色
	public int inputColor; //収入の文字色
	public int outputColor; //支出の文字色
	public int cashColor; //現金の文字色
	public int depoColor; //預金の文字色
	public int categoryColor; //カテゴリの文字色
	public int moneyColor1; //内訳の金額カラー１
	public int moneyColor2; //内訳の金額カラー２
	public int commentColor; //コメントカラー
	public int dateColor; //日付カラー

	public String[] path = new String[3];
	public int[] spinnerNumber = new int[14];

	//コンストラクタ
	public DesignInit(WalletManagementActivity context) {
		this.context = context;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					context.openFileInput("init.txt"), "Shift-JIS"));

			readData = new String[20];
			for (int i = 0; (readData[i] = br.readLine()) != null; i++) {
			}

			fontInit();
			imageInit();
			colorInit();

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//フォントの初期化
	public void fontInit(){
		//タイトルフォント設定
		if (readData[0].equals("default")) {
			path[1] = "デフォルト";
			titleFont = Typeface.createFromAsset(context.getAssets(), "font/AlexBrush.ttf");

		}else {
			try{
				titleFont = Typeface.createFromFile(readData[0]);
			}catch (Exception e){
				titleFont = Typeface.createFromAsset(context.getAssets(), "font/AlexBrush.ttf");
			}
		}

		//テキストフォント設定
		if (readData[1].equals("default")) {
			path[2] = "デフォルト";
			textFont = Typeface.DEFAULT;

		}else {
			try{
				textFont = Typeface.createFromFile(readData[1]);
			}catch (Exception e){
				textFont = Typeface.DEFAULT;
			}
		}
	}

	//画像の初期化
	public void imageInit(){
		try{
			//背景画像設定
			if (readData[2].equals("default")) {
				path[0] = "デフォルト";
				bgim = new BitmapDrawable(BitmapFactory.decodeResource(
						context.getResources(), R.drawable.back));
			} else {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;

				Bitmap bmp = BitmapFactory.decodeStream(
						context.getContentResolver().openInputStream(Uri.parse(readData[2])),
						null, options);

				//端末の解像度を取得する
				DisplayMetrics metrics = new DisplayMetrics();
				context.getWindowManager().getDefaultDisplay().getMetrics(metrics);

				int display_x = metrics.widthPixels;
				int display_y = metrics.heightPixels;

				int scaleW = options.outWidth / display_x + 1;
				int scaleH =options.outHeight / display_y + 1;
				int scale = Math.max(scaleW, scaleH);

				options.inJustDecodeBounds = false;
				options.inSampleSize = scale;

				bmp = BitmapFactory.decodeStream(
						context.getContentResolver().openInputStream(Uri.parse(readData[2])),
						null, options);

				Cursor query = MediaStore.Images.Media.query(
						context.getContentResolver(), Uri.parse(readData[2]),
						new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
						null, null);
				query.moveToFirst();
				System.out.println(query.getInt(0));

				Matrix mat = new Matrix();
				mat.postRotate(query.getInt(0));

				if(bmp == null)
					throw new IOException();

				bmp = Bitmap.createBitmap(bmp, 0, 0,
						bmp.getWidth(), bmp.getHeight(), mat, true);

				//背景画像の大きさを決定する
				Bitmap reSize = Bitmap.createBitmap(
						display_x, display_y,
						Config.ARGB_8888);

				Canvas canvas = new Canvas(reSize);
				canvas.drawColor(Color.WHITE);

				double aspectX = display_x / (double)bmp.getWidth();
				double aspectY = display_y / (double)bmp.getHeight();

				double aspect;
				int w=0, h=0;
				int dx=0, dy=0;

				if(aspectX >= aspectY){
					w = reSize.getWidth();
					h = (int)(bmp.getHeight() * aspectX);
					dy = (h - reSize.getHeight())/2;
				}else {
					w = (int)(bmp.getWidth() * aspectY);
					h = reSize.getHeight();
					dx = (w - reSize.getWidth())/2;
				}

				Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
				Rect dst = new Rect(-dx, -dy, w-dx, h-dy);

				canvas.drawBitmap(bmp, src, dst, null);

				bgim = new BitmapDrawable(reSize);

			}
		}catch(IOException e){
			bgim = new BitmapDrawable(BitmapFactory.decodeResource(
					context.getResources(), R.drawable.back));
			System.out.println(e);
			readData[2] = "default";
			writeData();
		}
	}

	//色の初期化
	public void colorInit() {
		//背景色設定
		if (readData[3].equals("default")) {
			bgColor = makeColor("ブラック", 0);
		} else {
			bgColor = makeColor(readData[3], 0);
		}

		//タイトルカラー設定
		if (readData[4].equals("default")) {
			titleColor = makeColor("ホワイト", 1);
		} else {
			titleColor = makeColor(readData[4], 1);
		}

		//テキストカラー設定
		if (readData[5].equals("default")) {
			textColor = makeColor("ホワイト", 2);
		} else {
			textColor = makeColor(readData[5], 2);
		}

		//タイトルの金額カラー設定
		if (readData[6].equals("default")) {
			infoColor = makeColor("イエロー", 3);
		} else {
			infoColor = makeColor(readData[6], 3);
		}

		//ボタンの文字色設定
		if (readData[7].equals("default")) {
			buttonColor = makeColor("ブラック", 4);
		} else {
			buttonColor = makeColor(readData[7], 4);
		}

		//収入の文字色設定
		if (readData[8].equals("default")) {
			inputColor = makeColor("ブルー", 5);
		} else {
			inputColor = makeColor(readData[8], 5);
		}

		//支出の文字色設定
		if (readData[9].equals("default")) {
			outputColor = makeColor("レッド", 6);
		} else {
			outputColor = makeColor(readData[9], 6);
		}

		//現金の文字色設定
		if (readData[10].equals("default")) {
			cashColor = makeColor("イエロー", 7);
		} else {
			cashColor = makeColor(readData[10], 7);
		}

		//預金の文字色設定
		if (readData[11].equals("default")) {
			depoColor = makeColor("グリーン", 8);
		} else {
			depoColor = makeColor(readData[11], 8);
		}

		//カテゴリの文字色設定
		if (readData[12].equals("default")) {
			categoryColor = makeColor("ホワイト", 9);
		} else {
			categoryColor = makeColor(readData[12], 9);
		}

		//内訳の金額カラー１設定
		if (readData[13].equals("default")) {
			moneyColor1 = makeColor("ブルー", 10);
		} else {
			moneyColor1 = makeColor(readData[13], 10);
		}

		//内訳の金額カラー２設定
		if (readData[14].equals("default")) {
			moneyColor2 = makeColor("レッド", 11);
		} else {
			moneyColor2 = makeColor(readData[14], 11);
		}

		//コメントカラー設定
		if (readData[15].equals("default")) {
			commentColor = makeColor("ホワイト", 12);
		} else {
			commentColor = makeColor(readData[15], 12);
		}

		//日付カラー設定
		if (readData[16].equals("default")) {
			dateColor = makeColor("ライトブルー", 13);
		} else {
			dateColor = makeColor(readData[16], 13);
		}
	}

	//色の生成
	public int makeColor(String cName, int index) {
		int color = 0;

		if (cName.equals("ブラック")) {
			color = Color.rgb(0, 0, 0);
			spinnerNumber[index] = 0;

		} else if (cName.equals("ホワイト")) {
			color = Color.rgb(255, 255, 255);
			spinnerNumber[index] = 1;

		} else if (cName.equals("グレイ")) {
			color = Color.rgb(128, 128, 128);
			spinnerNumber[index] = 2;

		} else if (cName.equals("シルバー")) {
			color = Color.rgb(192, 192, 192);
			spinnerNumber[index] = 3;

		} else if (cName.equals("レッド")) {
			color = Color.rgb(255, 0, 0);
			spinnerNumber[index] = 4;

		} else if (cName.equals("ダークレッド")) {
			color = Color.rgb(139, 0, 0);
			spinnerNumber[index] = 5;

		} else if (cName.equals("オレンジレッド")) {
			color = Color.rgb(255, 69, 0);
			spinnerNumber[index] = 6;

		} else if (cName.equals("ブルー")) {
			color = Color.rgb(0, 0, 255);
			spinnerNumber[index] = 7;

		} else if (cName.equals("ネイビー")) {
			color = Color.rgb(0, 0, 128);
			spinnerNumber[index] = 8;

		} else if (cName.equals("ライトブルー")) {
			color = Color.rgb(173, 216, 230);
			spinnerNumber[index] = 9;

		} else if (cName.equals("パープル")) {
			color = Color.rgb(128, 0, 128);
			spinnerNumber[index] = 10;

		} else if (cName.equals("インディゴ")) {
			color = Color.rgb(75, 0, 130);
			spinnerNumber[index] = 11;

		} else if (cName.equals("マゼンタ")) {
			color = Color.rgb(255, 0, 255);
			spinnerNumber[index] = 12;

		} else if (cName.equals("ピンク")) {
			color = Color.rgb(255, 192, 203);
			spinnerNumber[index] = 13;

		} else if (cName.equals("グリーン")) {
			color = Color.rgb(0, 128, 0);
			spinnerNumber[index] = 14;

		} else if (cName.equals("ダークグリーン")) {
			color = Color.rgb(0, 100, 0);
			spinnerNumber[index] = 15;

		} else if (cName.equals("ライトグリーン")) {
			color = Color.rgb(144, 238, 144);
			spinnerNumber[index] = 16;

		} else if (cName.equals("イエロー")) {
			color = Color.rgb(255, 255, 0);
			spinnerNumber[index] = 17;

		} else if (cName.equals("オレンジ")) {
			color = Color.rgb(255, 165, 0);
			spinnerNumber[index] = 18;

		} else if (cName.equals("カーキ")) {
			color = Color.rgb(240, 230, 140);
			spinnerNumber[index] = 19;

		} else if (cName.equals("ブラウン")) {
			color = Color.rgb(165, 42, 42);
			spinnerNumber[index] = 20;

		} else if (cName.equals("チョコレート")) {
			color = Color.rgb(210, 105, 30);
			spinnerNumber[index] = 21;

		} else if (cName.equals("ベージュ")) {
			color = Color.rgb(245, 245, 220);
			spinnerNumber[index] = 22;
		}

		return color;
	}

	//データ上書き
	public void writeData(){
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(
							"init.txt", context.MODE_PRIVATE), "Shift-JIS"));
			for (int i = 0; i < 17; i++) {
				bw.write(readData[i]);
				bw.newLine();

			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//スピナーイベント
	@Override
	public void onItemSelected(AdapterView<?> parent, View view,
			int position, long id) {
		int tag = Integer.parseInt(parent.getTag().toString().replaceAll("[^0-9]", ""));
		readData[tag - 1] = parent.getSelectedItem().toString();
		colorInit();

		writeData();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}
}
