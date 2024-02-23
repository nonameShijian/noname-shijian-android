package com.noname.shijian;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.palette.graphics.Palette;

import com.alibaba.fastjson.JSON;
import com.noname.shijian.zip.ZipUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AbstractFileHeader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.sf.sevenzipjbinding.SevenZip;

import cn.hutool.core.util.CharsetUtil;

public class NonameImportActivity extends Activity {
	/** 储存app版本号 */
	public static long VERSION = 10000L;

	public static final String[] WAITING_MESSAGES = {
			"无名杀的创造者是水乎，也叫村长。",
			"最好不要在扩展文件里包含除英文字母和英文标点之外的任何符号，防止导入的时候麻烦。",
			"苏婆玛丽奥是无名杀的现任更新者。",
			"GPLv3协议提倡开源与共享，是无名杀代码的基础协议。",
			"原生的换肤功能不好用？装个千幻聆音扩展试试吧！可以语音图片一起换哦！",
			"在线更新扩展能避免更新时崩溃导致需要重装游戏的问题。",
			"萌新记着要看公告和教程哦，不要频繁提问，大佬很忙的。",
			"诗笺版无名杀的前身是玄武版，现在玄武版已经光荣退役了。",
			"诗笺的第二个字念“jiān”，是“信纸”的意思，不要念错了。",
			"十周年UI扩展，是短歌制作的优秀的无名杀美化扩展。",
			"玄武江湖扩展，是原创世界观故事的武侠扩展，有独特的内力值玩法。",
			"时空枢纽扩展，是原创世界观故事的奇幻扩展，讲述了发生在奇幻世界的冒险故事。",
			"导入很慢吗？不要急，好饭不怕晚。",
			"无名杀的代码是用JavaScript写成的。",
			"阳光包，是优秀的武将扩展，由大佬阳光微凉设计开发。",
			"千幻聆音扩展的换肤换音功能，是可以让其它扩展接入并自定义的。",
			"我知道你很急，但是你先别急。",
			"无名杀扩展内置的导入键并不好用，不建议使用。",
			"无名杀的外壳和本体代码是分开的，目前本体仓库在GitHub上由苏婆进行更新维护。",
			"代码混淆的扩展有一定的风险，请谨慎甄别发布者。",
			"不要退出，你也不想导入出问题吧？",
			"请不要拿无名杀去别的圈子招仇恨，这种行为并不会显得你很智慧。",
			"写扩展，最好学会手动编辑文件。内置的编辑器太坑啦！",
			"无名杀的所有扩展，都有遵守GPLv3协议的义务。",
			"写扩展不是为了竞争武将强度的，孙悟空可以三棒槌打死鲁智深，但那并没啥意思。",
			"如果碰到错误弹窗，请滑动截取完整的弹窗信息再去询问作者。不然他也看不明白什么情况。",
			"越开放的扩展，在传播上有更多的优势。",
			"熟练掌握万能导入法，防止一切崩溃问题。",
			"强中更有强中手，一山更比一山高。",
			"在官服，联机情况下，是不能使用扩展的。",
			"关于无名杀的种种问题，牢记别人帮你是情分，别人不帮你是本分。",
			"无名杀不排斥开发自己的版本，但是必须遵守GPL协议。",
			"广告位招租！不要money交个朋友！",
	};

	/** 是否取得权限 */
	// private boolean hasPermissions = true;

	/** 标题文字 */
	private TextView titleTextView;

	/** 展示文字 */
	private TextView messageTextView;

	/** cache/currentLoadFile.zip */
	private File cacheFile = null;

	/** zip实例 */
	private ZipFile zipFile = null;

	///** zip的编码 */
	// String encoding = null;

	/** 进度条 */
	private ProgressDialog dialog;

	private int currentWaiting = 0;

	private long lastWaitingTime = 0;

	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(@NonNull Message msg) {
			super.handleMessage(msg);
			int MSG_PROGRESS = 0x0001;
			if (msg.what == MSG_PROGRESS) {
				int progress = msg.arg1;
				dialog.setProgress(progress);
				dialog.setMessage(getWaitingMessage());
			}
		}
	};

	/** 解压有错误不能进入游戏 */
	private boolean hasError = false;

	/** 是否是解压的内置资源包 */
	private boolean isAssetZip = false;

	public static class ExtensionNameException extends Exception {
		public ExtensionNameException() {
		}

		public ExtensionNameException(String message) {
			super(message);
		}

		public ExtensionNameException(String message, Throwable cause) {
			super(message, cause);
		}

		public ExtensionNameException(Throwable cause) {
			super(cause);
		}

		public ExtensionNameException(String message, Throwable cause, boolean enableSuppression,
									  boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
	}

	/** app文件夹内是否有游戏主文件 */
	public boolean inited() {
		File appPath = getExternalFilesDir(null).getParentFile();
		File[] files = new File[] {
				new File(appPath, "game/update.js"),
				new File(appPath, "game/config.js"),
				new File(appPath, "game/package.js"),
				new File(appPath, "game/game.js"),
		};
		for (File file: files) {
			if (!file.exists()) {
				return false;
			}
		}
		return true;
	}

	private boolean hasConfigFile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_begin);
		titleTextView = findViewById(R.id.title);
		messageTextView = findViewById(R.id.messages);

		WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
		BitmapDrawable bitmapDrawable = (BitmapDrawable) wallpaperManager.getDrawable(); // 默认获取系统壁纸
		Bitmap bitMap = bitmapDrawable.getBitmap();  // 获取系统壁纸的Bitmap
		// Palette palette = new Palette.Builder(bitMap).generate(); // 同步
		Palette.from(bitMap).maximumColorCount(10).generate(new Palette.PaletteAsyncListener() {
			@Override
			public void onGenerated(Palette palette) {
				Palette.Swatch s = palette.getDominantSwatch();      //独特的一种
				Palette.Swatch s1 = palette.getVibrantSwatch();      //获取到充满活力的这种色调
				Palette.Swatch s2 = palette.getDarkVibrantSwatch();  //获取充满活力的黑
				Palette.Swatch s3 = palette.getLightVibrantSwatch(); //获取充满活力的亮
				Palette.Swatch s4 = palette.getMutedSwatch();        //获取柔和的色调
				Palette.Swatch s5 = palette.getDarkMutedSwatch();    //获取柔和的黑
				Palette.Swatch s6 = palette.getLightMutedSwatch();   //获取柔和的亮
				if (s6 != null) {
					titleTextView.setTextColor(s6.getRgb());
					messageTextView.setTextColor(s6.getRgb());
				}
				if (s5 != null) {
					titleTextView.setShadowLayer(10, 5, 5, s5.getRgb());
					messageTextView.setShadowLayer(10, 5, 5, s5.getRgb());
				}
			}
		});

		// updateText("Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
		ToastUtils.show(NonameImportActivity.this, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
		if(Build.VERSION.SDK_INT < 30) {
			// 要申请的权限列表
			ArrayList<String> permissions = new ArrayList<>();

			// 读取文件权限
			if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
				permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}

			// 写入文件权限
			if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}

			if (permissions.size() > 0) {
				requestPermissions(permissions.toArray(new String[permissions.size()]), 999);
			} else {
				afterHasPermissions();
			}
		} else {
			afterHasPermissions();
		}
	}

	@Override
	/** 权限请求回调 */
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 999) {
			for (int ret : grantResults) {
				if (ret != PackageManager.PERMISSION_GRANTED) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setCancelable(false);
					builder.setTitle("未授予权限，将退出程序");
					builder.setNegativeButton("知道了", (dialog, which) -> {
						updateText("未授予权限，将退出程序");
						finish();
					});
					return;
				}
			}
			afterHasPermissions();
		}
	}

	/** 成功申请后 */
	private void afterHasPermissions() {
		FinishImport.getAppVersion(this);
		updateText("APK版本: " + VERSION);
		if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
			loadUri(getIntent().getData());
		} else if (getSharedPreferences("nonameshijian", MODE_PRIVATE).getLong("version",10000) < VERSION) {
			updateText("检测到您是首次安装或是升级了app，将自动为您解压内置资源");
			fixCharSet = "utf-8";
			AlertDialog dialog = new AlertDialog.Builder(this)
					.setIcon(R.mipmap.ic_banner_foreground)
					.setTitle("请选择是否解压")
					.setMessage("检测到您是首次安装或是升级了app,是否解压内置资源？")
					.setNegativeButton("取消", (dialogInterface, i) -> {
						dialogInterface.dismiss();
						loadAssetExt();
					})
					.setPositiveButton("确定", (dialog1, which) -> {
						dialog1.dismiss();
						try {
							InputStream inputStream = getAssets().open("www/app/noname.zip");
							inputStream.close();
							loadAssetZip();
							ToastUtils.show(NonameImportActivity.this, "正在解压内置资源包");
						} catch (IOException e) {
							loadAssetExt();
						}
					}).create();
			dialog.show();
		} else if (getIntent() != null && getIntent().getExtras() != null && "true".equals(getIntent().getExtras().getString("unzip"))) {
			AlertDialog dialog = new AlertDialog.Builder(this)
					.setIcon(R.mipmap.ic_banner_foreground)
					.setTitle("请选择是否解压")
					.setMessage("是否解压内置资源？")
					.setNegativeButton("取消", (dialogInterface, i) -> {
						dialogInterface.dismiss();
						loadAssetExt();
					})
					.setPositiveButton("确定", (dialog1, which) -> {
						dialog1.dismiss();
						try {
							InputStream inputStream = getAssets().open("www/app/noname.zip");
							inputStream.close();
							loadAssetZip();
							ToastUtils.show(NonameImportActivity.this, "正在解压内置资源包");
						} catch (IOException e) {
							loadAssetExt();
						}
					}).create();
			dialog.show();
		} else {
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
			this.finish();
		}
	}

	/** 导入压缩文件 */
	private void loadUri(final Uri uri) {
		new Thread(){
			public void run(){
				//ToastUtils.show(NonameImportActivity.this, "正在加载压缩包文件");
				updateText("正在加载压缩包文件...");
				try {
					// 把文件写入cache/currentLoadFile.zip
					InputStream inputStream = getContentResolver().openInputStream(uri);
					cacheFile = new File(getExternalCacheDir(), "currentLoadFile.zip");
					Utils.inputStreamToFile(inputStream, cacheFile, 1024 * 1024 *100, new Utils.ByteCallback() {
						@Override
						public void onProgress(long bytes) {
							updateText("已读取"+(bytes/(1024*1024))+"MB，请稍候");
						}
					});
					zipFile = new ZipFile(cacheFile);

					if (!zipFile.isValidZipFile()) {
						throw new Exception("压缩文件不合法,可能被损坏。");
					}

					if (zipFile.getFileHeader("game/game.js") != null) {
						updateText("压缩包被识别成游戏主文件包");
						if (zipFile.getFileHeader("noname.config.txt") != null) {
							hasConfigFile = true;
						}
						importPackage();
						return;
					} else if (zipFile.getFileHeader("extension.js") != null || zipFile.getFileHeader("extension.ts") != null) {
						updateText("压缩包被识别成扩展包");
						if (!inited()) {
							updateText("检测到您的文件缺失不能进入游戏，所以暂时不能导入扩展。请先导入离线包/完整包，或者在游戏的初始界面下载文件(注: 当前更新源不稳定，不建议在游戏初始界面下载)");
							return;
						}
						importExtension();
						return;
					} else {
						// 判断是否是文件夹嵌套的扩展或主文件包
						List<FileHeader> list = zipFile.getFileHeaders();
						List<String> mainFiles = Arrays.asList(
								"game/update.js",
								"game/config.js",
								"game/package.js",
								"game/game.js"
						);
						boolean isMain = list.stream().anyMatch(fileHeader -> {
							String fileName = fileHeader.getFileName();
							return mainFiles.stream().anyMatch(fileName::endsWith);
						});
						boolean isExtension = list.stream().anyMatch(fileHeader -> {
							String fileName = fileHeader.getFileName();
							return (
									fileName.endsWith("/extension.js") &&
									!fileName.endsWith("/boss/extension.js") &&
									!fileName.endsWith("/cardpile/extension.js") &&
									!fileName.endsWith("/wuxing/extension.js") &&
									!fileName.endsWith("/coin/extension.js")
									) ||
									fileName.endsWith("/extension.ts");
						});
						// 是文件夹嵌套的主文件包
						if (isMain) {
							updateText("压缩包被识别成文件夹嵌套的主文件包");
							Object[] paths = list.stream()
									.filter(fileHeader -> {
										String fileName = fileHeader.getFileName();
										return fileName.endsWith("game/game.js");
									})
									.map(AbstractFileHeader::getFileName)
									.toArray();
							// 取最短的路径
							String path = "";
							int strLen = -1;
							for (Object p: paths) {
								String p1 = p.toString();
								if (p1.length() < strLen || strLen == -1) {
									strLen = p1.length();
									path = p1;
								}
							}
							String rootPath = path.substring(0, path.indexOf("game/game.js"));
							if (list.stream().filter(fileHeader -> (rootPath + "/noname.config.txt").equals(fileHeader.getFileName())).toArray().length > 0) {
								hasConfigFile = true;
							}
							// updateText("rootPath: " + rootPath);
							importPackage(rootPath);
							return;
						}
						else if (isExtension) {
							updateText("压缩包被识别成文件夹嵌套的扩展");
							if (!inited()) {
								updateText("检测到您的文件缺失不能进入游戏，所以暂时不能导入扩展。请先导入离线包/完整包，或者在游戏的初始界面下载文件(注: 当前更新源不稳定，不建议在游戏初始界面下载)");
								return;
							}
							Object[] paths = list.stream()
									.filter(fileHeader -> {
										String fileName = fileHeader.getFileName();
										return fileName.endsWith("/extension.js") || fileName.endsWith("/extension.ts");
									})
									.map(AbstractFileHeader::getFileName)
									.toArray();
							// 取最短的路径
							String path = "";
							int strLen = -1;
							for (Object p: paths) {
								String p1 = p.toString();
								if (p1.length() < strLen || strLen == -1) {
									strLen = p1.length();
									path = p1;
								}
							}
							String rootPath = path.substring(0, path.indexOf(path.endsWith("/extension.js") ? "extension.js" : "extension.ts"));
							// updateText("rootPath: " + rootPath);
							importExtension(rootPath);
							return;
						}
					}

					updateText("压缩包识别失败，请手动选择目录导入");
					// ToastUtils.show(NonameImportActivity.this.getApplicationContext(),  "压缩包识别失败，请手动选择目录导入");

					// 手动选择目录
					Intent ListViewIntent = new Intent(NonameImportActivity.this, ListViewActivity.class);
					ListViewIntent.putExtra("type", "folder");
					startActivityForResult(ListViewIntent, 2);

				} catch (Exception e) {
					ToastUtils.show(NonameImportActivity.this,  "文件解压出现异常，已停止解压\n" + e.getMessage());
					updateText("文件解压出现异常，已停止解压\n" + e.getMessage());
				}
			}
		}.start();
	}

	/** 导入asset里的zip文件*/
	private void loadAssetZip() {
		new Thread(){
			public void run(){
				updateText("正在加载内置资源压缩包");
				try {
					// 把文件写入cache/currentLoadFile.zip
					InputStream inputStream = getAssets().open("www/app/noname.zip");
					cacheFile = new File(getExternalCacheDir(), "currentLoadFile.zip");
					Utils.inputStreamToFile(inputStream, cacheFile);
					zipFile = new ZipFile(cacheFile);

					if (!zipFile.isValidZipFile()) {
						throw new Exception("压缩文件不合法,可能被损坏。");
					}

					if (zipFile.getFileHeader("game/game.js") != null) {
						updateText("压缩包被识别成游戏主文件包");
						isAssetZip = true;
						if (zipFile.getFileHeader("noname.config.txt") != null) {
							hasConfigFile = true;
						}
						importPackage();
						return;
					} else {
						// 判断是否是文件夹嵌套的扩展或主文件包
						List<FileHeader> list = zipFile.getFileHeaders();
						List<String> mainFiles = Arrays.asList(
								"game/update.js",
								"game/config.js",
								"game/package.js",
								"game/game.js"
						);
						boolean isMain = list.stream().anyMatch(fileHeader -> {
							String fileName = fileHeader.getFileName();
							return mainFiles.stream().anyMatch(fileName::endsWith);
						});
						boolean isExtension = list.stream().anyMatch(fileHeader -> {
							String fileName = fileHeader.getFileName();
							return fileName.endsWith("/extension.js") &&
									!fileName.endsWith("/boss/extension.js") &&
									!fileName.endsWith("/cardpile/extension.js") &&
									!fileName.endsWith("/wuxing/extension.js") &&
									!fileName.endsWith("/coin/extension.js");
						});
						// 是文件夹嵌套的主文件包
						if (isMain) {
							updateText("压缩包被识别成文件夹嵌套的主文件包");
							Object[] paths = list.stream()
									.filter(fileHeader -> {
										String fileName = fileHeader.getFileName();
										return fileName.endsWith("game/game.js");
									})
									.map(AbstractFileHeader::getFileName)
									.toArray();
							// 取最短的路径
							String path = "";
							int strLen = -1;
							for (Object p : paths) {
								String p1 = p.toString();
								if (p1.length() < strLen || strLen == -1) {
									strLen = p1.length();
									path = p1;
								}
							}
							String rootPath = path.substring(0, path.indexOf("game/game.js"));
							if (list.stream().filter(fileHeader -> (rootPath + "/noname.config.txt").equals(fileHeader.getFileName())).toArray().length > 0) {
								hasConfigFile = true;
							}
							// updateText("rootPath: " + rootPath);
							isAssetZip = true;
							importPackage(rootPath);
							return;
						}
					}
					updateText("压缩包识别失败，请手动选择目录导入");
					ToastUtils.show(NonameImportActivity.this,  "压缩包识别失败，请手动选择目录导入");

					// 手动选择目录
					Intent ListViewIntent = new Intent(NonameImportActivity.this, ListViewActivity.class);
					ListViewIntent.putExtra("type", "folder");
					startActivityForResult(ListViewIntent, 2);
				} catch (Exception e) {
					ToastUtils.show(NonameImportActivity.this,  "文件解压出现异常，已停止解压\n" + e.getMessage());
					updateText("文件解压出现异常，已停止解压\n" + e.getMessage());
				}
			}
		}.start();
	}

	private void loadAssetExt() {
		long oldVersion = getSharedPreferences("nonameshijian", MODE_PRIVATE).getLong("version",10000);
		if (oldVersion < 16000) {
			File data = getExternalFilesDir(null).getParentFile();
			File config = new File(data, "game/config.js");
			reWriteConfigFile(config);
		}
		// 储存版本号
		getSharedPreferences("nonameshijian", MODE_PRIVATE)
				.edit()
				.putLong("version",VERSION)
				.apply();

		afterFinishImportExtension();
	}

	private void importExtension() throws Exception {
		if (cacheFile.length() >= 50 * 1024 * 1024) {
			//ToastUtils.show(NonameImportActivity.this,  "这个文件比较大，请耐心等待。");
			updateText("这个文件比较大，请耐心等待。");
		}
		if (zipFile.isEncrypted()) {
			updateText("这个文件需要密码");
			setPassword(1, null);
		} else {
			importExtension2();
		}
	}

	private void importExtension(String rootPath) throws Exception {
		if (cacheFile.length() >= 50 * 1024 * 1024) {
			//ToastUtils.show(NonameImportActivity.this,  "这个文件比较大，请耐心等待。");
			updateText("这个文件比较大，请耐心等待。");
		}
		if (zipFile.isEncrypted()) {
			updateText("这个文件需要密码");
			setPassword(1, rootPath);
		} else {
			importExtension2(rootPath);
		}
	}

	private void importExtension2() throws Exception {
		File cacheDir = new File(getExternalCacheDir(), Utils.getRandomString(10));
		try { zipFile.extractFile("extension.js", cacheDir.getPath()); } catch (ZipException ignored) {}
		try { zipFile.extractFile("extension.ts", cacheDir.getPath()); } catch (ZipException ignored) {}
		try { zipFile.extractFile("info.json", cacheDir.getPath()); } catch (ZipException ignored) {}
		File cacheJs = new File(cacheDir, "extension.js");
		try {
			// ts的扩展名还是以info.json解析为准
			String extensionName = getExtensionName(cacheJs);
			updateText("扩展名解析为：" + extensionName);
			runOnUiThread(() -> {
				final EditText editText = new EditText(NonameImportActivity.this);
				editText.setText(extensionName);
				new AlertDialog.Builder(NonameImportActivity.this)
						.setTitle("请确认扩展名是否正确")
						.setView(editText)
						.setCancelable(false)
						.setPositiveButton("确定", (dialogInterface, i) -> {
							if (editText.getText().length() == 0) {
								ToastUtils.show(NonameImportActivity.this,  "请输入扩展名！");
							} else {
								final String extName = editText.getText().toString();
								updateText("正在解压到对应扩展文件夹。请耐心等待。");
								showProgressDialogAndExtractAll(getExtensionFile(extName).getPath(), cacheDir, extName);
							}
						}).create().show();
			});
		} catch (ExtensionNameException e) {
			runOnUiThread(() -> {
				final EditText editText = new EditText(NonameImportActivity.this);
				new AlertDialog.Builder(NonameImportActivity.this)
						.setTitle("扩展名解析失败，请手动输入扩展名（纯扩展名，不包含版本号等信息）")
						.setView(editText)
						.setCancelable(false)
						.setPositiveButton("确定", (dialogInterface, i) -> {
							if (editText.getText().length() == 0) {
								ToastUtils.show(NonameImportActivity.this,  "请输入扩展名！");
							} else {
								final String extensionName = editText.getText().toString();
								updateText("扩展名输入为：" + extensionName + "\n正在解压到对应扩展文件夹。请耐心等待。");
								showProgressDialogAndExtractAll(getExtensionFile(extensionName).getPath(), cacheDir, extensionName);
							}
						}).create().show();
			});
		}
	}

	private void importExtension2(String rootPath) throws Exception {
		// cache里创建一个随机字符串的文件夹
		String randomString = Utils.getRandomString(10);
		File cacheDir = new File(getExternalCacheDir(), randomString);
		// 把extension.js解压到随机字符串的文件夹
		try { zipFile.extractFile(rootPath + "extension.js", cacheDir.getPath()); } catch (ZipException ignored) {}
		try { zipFile.extractFile(rootPath + "extension.ts", cacheDir.getPath()); } catch (ZipException ignored) {}
		try { zipFile.extractFile(rootPath + "info.json", cacheDir.getPath()); } catch (ZipException ignored) {}
		File cacheJs = new File(cacheDir, rootPath + "extension.js");
		String [] split = rootPath.split("/");
		new Thread() {
			public void run() {
				try {
					String extensionNameFromDir = split[split.length - 1];
					String extensionNameFromJs = null;
					// ts的扩展名还是以info.json解析为准
					extensionNameFromJs = getExtensionName(cacheJs);
					updateText("从文件夹名解析扩展名为: " + extensionNameFromDir);
					updateText("从js/ts/json文件解析扩展名为: " + extensionNameFromJs);
					String extensionName;
					if (extensionNameFromDir.equals(extensionNameFromJs)) {
						extensionName = extensionNameFromDir;
					} else {
						updateText("解析结果不同，以js/ts/json文件的解析结果为准");
						extensionName = extensionNameFromJs;
					}
					updateText("扩展名解析为：" + extensionName);
					runOnUiThread(() -> {
						final EditText editText = new EditText(NonameImportActivity.this);
						new AlertDialog.Builder(NonameImportActivity.this)
								.setTitle("请确认扩展名是否正确")
								.setView(editText)
								.setCancelable(false)
								.setPositiveButton("确定", (dialogInterface, num) -> {
									if (editText.getText().length() == 0) {
										ToastUtils.show(NonameImportActivity.this,  "请输入扩展名！");
									} else {
										final String extName = editText.getText().toString();
										updateText("开始检测解压文件，此过程时间可能会较长，请耐心等待。");
										try {
											// 获取到extension的文件夹位置
											File extDir = getExtensionFile(extName);
											if (!extDir.exists()) {
												extDir.mkdir();
											}
											// updateText("rootPath: " + rootPath);
											// updateText("原文件数量: " + zipFile.getFileHeaders().size());
											// 移除除了rootPath外的文件
											List<String> filesToRemove = new ArrayList<>();
											Map<String, String> fileNamesMap = new HashMap<>();
											List<FileHeader> list = zipFile.getFileHeaders();
											for (FileHeader f: list) {
												String name = f.getFileName();
												// 如果rootPath是a/b时，name为a此判断依旧返回真
												// if (!name.startsWith(rootPath)) {
												if (!name.startsWith(rootPath) && rootPath.indexOf(name) != 0) {
													filesToRemove.add(name);
												} else if(!f.isDirectory()) {
													fileNamesMap.put(name, name.substring(rootPath.length()));
												}
											}
											// zip文件中删除多个文件和文件夹
											zipFile.removeFiles(filesToRemove);
											// 把rootPath中的文件移动到zip根目录
											zipFile.renameFiles(fileNamesMap);
											// 删除rootPath
											zipFile.removeFile(rootPath);
											// 循环删除
											if (rootPath.split("/").length > 1) {
												String[] split = rootPath.split("/");
												for (int i = split.length - 1; i > -1; i--) {
													String p = "";
													for (int j = 0; j <= i; j++) {
														p = p + split[j] + '/';
													}
													zipFile.removeFile(p);
												}
											}
											// 解压zip
											// updateText("filesToRemove: " + filesToRemove.size());
											// updateText("fileNamesMap: " + fileNamesMap.size());
											// zipFile.extractAll(extDir.getPath());
											showProgressDialogAndExtractAll(extDir.getPath(), cacheDir, extName);

										} catch (Exception e) {
											updateText("解压失败，已停止解压\n" + e);
										}
									}
								}).create().show();
					});
				} catch (Exception e) {
					updateText("解压失败，已停止解压\n" + e);
				}
			}
		}.start();
	}

	/** 延时1.5S进入游戏 */
	private void afterFinishImportExtension(String extname) {
		if (dialog != null) {
			// 关闭对话框
			dialog.dismiss();
		}

		if (hasError) return;

		PackageManager packageManager = this.getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
		intent.setPackage(null);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.putExtra("extensionImport", extname);
		FinishImport.ext = extname;

		// 导入完完整包/离线包后，储存版本号
		if (extname.equals("importPackage")) {
			getSharedPreferences("nonameshijian", MODE_PRIVATE)
					.edit()
					.putLong("version", VERSION)
					.apply();

			File data = getExternalFilesDir(null).getParentFile();
			File config = new File(data, "game/config.js");
			if (!config.exists()) {
				updateText("检测到您没有game/config.js，将为您从内置资源中复制一份");
				File result = Utils.assetToFile("www/game/config_example.js", this, "game/config.js");
				if (result == null) {
					updateText("内置资源game/config_example.js复制失败");
				}
			} else {
				reWriteConfigFile(config);
			}
		}

		File file = Utils.assetToFile("www/app/app-release.apk",this,"cache/app-release.apk");
		if (file == null || !isAssetZip) {
			Log.e("install", "file is null");
			updateText("正在为你启动无名杀。");
			Timer timer = new Timer();
			timer.schedule(new TimerTask(){
				public void run(){
					timer.cancel();
					startActivity(intent);
					finish();
				}
			}, 1500);
		} else {
			runOnUiThread(() -> {
				final TextView textView = new TextView(this);
				textView.setText("注: 此过程不消耗流量，且点击确定后将自动关闭本界面");
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("解压完内置资源后，是否覆盖安装没有资源的apk以节省资源？");
				builder.setView(textView);
				// builder.setIcon(R.mipmap.ic_launcher);
				builder.setPositiveButton("确定", (dialog, which) -> {
					installApk(file);
				});
				builder.setNegativeButton("取消", (dialog, which) -> {
					updateText("正在为你启动无名杀。");
					Timer timer = new Timer();
					timer.schedule(new TimerTask() {
						public void run() {
							timer.cancel();
							startActivity(intent);
							finish();
						}
					}, 1500);
				});
				builder.create().show();
			});
		}
	}

	private void reWriteConfigFile(File config) {
		try {
			updateText("检测到您有game/config.js，将为您添加内置扩展");
			Scanner scan = new Scanner(config);
			while (scan.hasNextLine()) {
				String line = scan.nextLine().trim();
				if (line.startsWith("extensions:")) {
					String extArray = line.substring(11, line.length() - 1).trim();
					if (extArray.contains("Settings")) break;
					// 修改文件，写入
					FileReader in = new FileReader(config);
					BufferedReader bufIn = new BufferedReader(in);
					// 内存流, 作为临时流
					CharArrayWriter tempStream = new CharArrayWriter();
					// 替换
					String lines;
					while ((lines = bufIn.readLine()) != null) {
						// 替换每行中, 符合条件的字符串
						if (lines.trim().startsWith("extensions:")) {
							if (extArray.equals("[]")) {
								lines = lines.replace(extArray, "['Settings']");
							} else {
								lines = lines.replace(extArray, "['Settings', " + extArray.substring(1));
							}
							// updateText("lines: " + lines);
						}
						// 将该行写入内存
						tempStream.write(lines);
						// 添加换行符
						tempStream.append(System.getProperty("line.separator"));
					}
					// 关闭 输入流
					bufIn.close();
					// 将内存中的流 写入 文件
					FileWriter out = new FileWriter(config);
					tempStream.writeTo(out);
					out.close();
					// 写入文件后退出读取文件的循环
					break;
				}
			}
			scan.close();
			updateText("内置扩展添加成功");
		} catch (Exception e) {
			updateText("内置扩展添加失败\n" + e.getMessage());
		}
	}

	/** 不延时进入游戏 */
	private void afterFinishImportExtension() {
		if (dialog != null) {
			// 关闭对话框
			dialog.dismiss();
		}

		if (hasError) return;

		File data = getExternalFilesDir(null).getParentFile();
		File config = new File(data, "game/config.js");
		if (!config.exists()) {
			updateText("检测到您没有game/config.js，将为您从内置资源中复制一份");
			File result = Utils.assetToFile("www/game/config_example.js", this, "game/config.js");
			if (result == null) {
				updateText("内置资源game/config_example.js复制失败");
			}
		} else {
			reWriteConfigFile(config);
		}

		File file = Utils.assetToFile("www/app/app-release.apk",this,"cache/app-release.apk");
		if (file == null || !isAssetZip) {
			Log.e("install", "file is null");
			updateText("正在为你启动无名杀");

			//Intent intent = new Intent(this, MainActivity.class);
			PackageManager packageManager = this.getPackageManager();
			Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
			intent.setPackage(null);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			finish();
		} else {
			runOnUiThread(() -> {
				final TextView textView = new TextView(this);
				textView.setText("注: 此过程不消耗流量，点击确定后将自动关闭本界面");
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("解压完内置资源后，是否覆盖安装没有资源的apk以节省资源？");
				builder.setView(textView);
				// builder.setIcon(R.mipmap.ic_launcher);
				builder.setPositiveButton("确定", (dialog, which) -> {
					installApk(file);
				});
				builder.setNegativeButton("取消", (dialog, which) -> {
					updateText("正在为你启动无名杀");

					//Intent intent = new Intent(this, MainActivity.class);
					PackageManager packageManager = this.getPackageManager();
					Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
					intent.setPackage(null);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					finish();
				});
				builder.create().show();
			});
		}
	}

	private File getExtensionFile(String extname) {
		File file = getExternalFilesDir(null).getParentFile();
		return new File(file, "extension/" + extname);
	}

	private String getExtensionName(File file) throws Exception {
		// new json file
		File jsonFile = new File(file.getParentFile(), "info.json");
		Log.e("getExtensionName", jsonFile.getAbsolutePath());
		if (jsonFile.exists() && jsonFile.canRead() && jsonFile.isFile()) {
			InputStream in = new BufferedInputStream(new FileInputStream(jsonFile));
			Scanner s = new Scanner(in).useDelimiter("\\A");
			String conf = s.hasNext() ? s.next() : "";
			// String conf = IOUtils.toString(in, StandardCharsets.UTF_8);
			String name = JSON.parseObject(conf).getString("name");
			if (name != null) {
				Log.e("getExtensionName", name);
				updateText("从info.json解析出的扩展名为: " + name);
				return name;
			}
		}
		// old
		if (!file.exists() && "extension.js".equals(file.getName())) {
			file = new File(file.getParentFile(), "extension.ts");
		}
		Scanner scanner = new Scanner(file);
		boolean appear = false;
		String s = "name:\"";
		String s2 = "name: \"";
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if (line.startsWith("/// <reference path=") || line.startsWith("///<reference path=")) {
				continue;
			}
			// 未格式化的扩展
			if (line.contains("game.import(")) {
				appear = true;
				int index = line.indexOf(s);
				if (index < 0) {
					continue;
				}
				String ret = line.substring(index + s.length(), line.indexOf('\"', index + s.length()));
				if (ret.length() != 0) {
					Log.e("getExtensionName", ret);
					updateText("从extension.js解析出的扩展名为: " + ret);
					return ret;
				}
			}
			if (appear && (line.startsWith(s) || line.startsWith(s2))) {
				String str = line.startsWith(s) ? s : s2;
				int length = str.length();
				int index = line.indexOf(str);
				String ret = line.substring(index + length, line.indexOf('\"', index + length));
				if (ret.length() != 0) {
					Log.e("getExtensionName", ret);
					updateText("从extension.js解析出的扩展名为: " + ret);
					return ret;
				}
			}
		}
		throw new ExtensionNameException("解析扩展名失败");
	}

	private void importPackage() throws Exception {
		if (cacheFile.length() >= 50 * 1024 * 1024) {
			//ToastUtils.show(NonameImportActivity.this,  "这个文件比较大，请耐心等待。");
			updateText("这个文件比较大，请耐心等待。");
		}
		if (zipFile.isEncrypted()) {
			updateText("这个文件需要密码");
			setPassword(2, null);
		} else {
			importPackage2();
		}
	}

	private void importPackage(String rootPath) throws Exception {
		if (cacheFile.length() >= 50 * 1024 * 1024) {
			//ToastUtils.show(NonameImportActivity.this,  "这个文件比较大，请耐心等待。");
			updateText("这个文件比较大，请耐心等待。");
		}
		if (zipFile.isEncrypted()) {
			updateText("这个文件需要密码");
			setPassword(2, rootPath);
		} else {
			importPackage2(rootPath);
		}
	}

	private void importPackage2() {
		File data = getExternalFilesDir(null).getParentFile();
		new Thread() {
			public void run() {
				try {
					showProgressDialogAndExtractAll(data.getPath(), null, "importPackage");
					// updateText("解压完成！");
				} catch (Exception e) {
					updateText("解压失败，已停止解压\n" + e.getMessage());
				}
			}
		}.start();
	}

	private void importPackage2(String rootPath) {
		File data = getExternalFilesDir(null).getParentFile();
		new Thread() {
			public void run() {
				try {
					// 移除除了rootPath外的文件
					List<String> filesToRemove = new ArrayList<>();
					Map<String, String> fileNamesMap = new HashMap<>();
					List<FileHeader> list = zipFile.getFileHeaders();
					for (FileHeader f: list) {
						String name = f.getFileName();
						if (!name.startsWith(rootPath) && rootPath.indexOf(name) != 0) {
							filesToRemove.add(name);
						} else if(!f.isDirectory()) {
							fileNamesMap.put(name, name.substring(rootPath.length()));
						}
					}
					// zip文件中删除多个文件和文件夹
					zipFile.removeFiles(filesToRemove);
					// 把rootPath中的文件移动到zip根目录
					zipFile.renameFiles(fileNamesMap);
					// 删除rootPath
					zipFile.removeFile(rootPath);
					// 循环删除
					if (rootPath.split("/").length > 1) {
						String[] split = rootPath.split("/");
						for (int i = split.length - 1; i > -1; i--) {
							StringBuilder p = new StringBuilder();
							for (int j = 0; j <= i; j++) {
								p.append(split[j]).append('/');
							}
							zipFile.removeFile(p.toString());
						}
					}
					// 解压zip
					// zipFile.extractAll(data.getPath());
					showProgressDialogAndExtractAll(data.getPath(), null, "importPackage");
				} catch (Exception e) {
					updateText("解压失败，已停止解压\n" + e.getMessage());
				}
			}
		}.start();
	}

	private char[] password;

	private void setPassword(int method, String rootPath) {
		runOnUiThread(() -> {
			final EditText editText = new EditText(NonameImportActivity.this);
			new AlertDialog.Builder(NonameImportActivity.this)
					.setTitle("请输入压缩包密码")
					.setView(editText)
					.setCancelable(false)
					.setIcon(R.mipmap.ic_launcher)
					.setPositiveButton("确定", (dialogInterface, i) -> {
						if (editText.getText().length() == 0) {
							ToastUtils.show(NonameImportActivity.this,  "请输入压缩包密码！");
							// alertDialog框消失后重新出现
							setPassword(method, rootPath);
						} else {
							char[] password = editText.getText().toString().toCharArray();
							this.password = password;
							zipFile.setPassword(password);
							try {
								if (method == 1) {
									if (rootPath != null) {
										importExtension2(rootPath);
									} else {
										importExtension2();
									}
								} else if (method == 2) {
									if (rootPath != null) {
										importPackage2(rootPath);
									} else {
										importPackage2();
									}
								} else if (method == 3) {
									showProgressDialogAndExtractAll(rootPath, null, null);
								}
							} catch (Exception e) {
								String message = e.getMessage();
								if (message != null) {
									if ("Wrong password!".equals(e.getMessage())) {
										updateText("密码错误！请重新输入密码！");
										ToastUtils.show(NonameImportActivity.this,  "密码错误！请重新输入密码！");
										setPassword(method, rootPath);
									} else {
										updateText("解压失败！\n" + e.getMessage());
									}
								} else {
									updateText("解压失败！");
								}
							}
						}
					}).create().show();
		});
	}

	private void updateText(final String msg){
		runOnUiThread(() -> {
			if (messageTextView == null) return;
			String newMsg = msg + "\n" + messageTextView.getText();
			if(newMsg.length() >= 1000){
				newMsg = newMsg.substring(0,999);
			}
			messageTextView.setText(newMsg);
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//判断code
		if (requestCode == 2 && resultCode == 3) {
			// 从data中取出数据
			String path = data.getStringExtra("path");
			try {
				path = path.replace(getPackageName(), getExternalFilesDir(null).getParentFile().getPath());
			} catch (Exception e) {
				updateText("手动选择目录失败，已停止解压");
				ToastUtils.show(NonameImportActivity.this,  "手动选择目录失败，已停止解压");
				afterFinishImportExtension();
				return;
			}
			// 解压到path
			String finalPath = path;
			try {
				if (zipFile.isEncrypted()) {
					updateText("这个文件需要密码");
					setPassword(3, path);
				} else {
					isAssetZip = true;
					showProgressDialogAndExtractAll(finalPath, null, null);
				}
			} catch (ZipException e) {
				updateText("解压失败，已停止解压\n" + e.getMessage());
			}
		} else if (requestCode == 2 && resultCode == 2) {
			finish();
		}
	}

	private String getCharset(ZipFile zipFile)throws ZipException{
		if(!TextUtils.isEmpty(fixCharSet)){
			updateText("使用选定的编码"+fixCharSet);
			return fixCharSet;
		}
		String[] charsets = new String[]{"utf-8","gbk","gb2312"};
		for(String c:charsets){
			updateText("正在检查文件编码是否为"+c);
			ZipFile z = new ZipFile(zipFile.getFile());
			z.setCharset(Charset.forName(c));
			if(!hasMessy(z,c)){
				updateText("编码确认为："+c);
				return c;
			}
		}
		updateText("无法确认文件编码");
		return null;
	}

	private HashMap<String,String> messyCodeMap = new HashMap<>();

	private String fixCharSet = null;

	private boolean hasMessy(ZipFile zipFile,String charset) throws ZipException{
		for (FileHeader fh : zipFile.getFileHeaders()) {
			if (isMessyCode(fh.getFileName())) {
				updateText("发现疑似乱码文件："+fh.getFileName());
				messyCodeMap.put(charset,fh.getFileName());
				return true;
			}
		}
		return false;
	}

	private void chooseCharsetAndExtractAll(String filePath,File cacheDir,String extName){
		if(messyCodeMap.size() == 0){
			extractAll(filePath,cacheDir,extName);
			return;
		}
		List<CharSequence> list = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		for(Map.Entry<String,String> entry:messyCodeMap.entrySet()){
			list.add("["+entry.getKey()+"编码]:"+entry.getValue());
			keys.add(entry.getKey());
		}
		CharSequence[] items = list.toArray(new CharSequence[messyCodeMap.size()]);
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle("请选择以下不为乱码的一项");
		fixCharSet = keys.get(0);
		alertBuilder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				fixCharSet = keys.get(i);
			}
		});
		alertBuilder.setIcon(R.mipmap.ic_launcher);
		alertBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
				new Thread(){
					@Override
					public void run() {
						extractAll(filePath,cacheDir,extName);
					}
				}.start();
			}
		});
		alertBuilder.create().show();
	}

	// 解压文件
	private void extractAll(String filePath, File cacheDir, String extName) {
		//zipFile = new ZipFile(zipFile.getFile());
		//zipFile.setPassword(this.password);
		try {
			String charset = getCharset(zipFile);
			if(charset == null){
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						chooseCharsetAndExtractAll(filePath,cacheDir,extName);
					}
				});
				return;
			}
			updateText("Charset is:"+charset);
			zipFile = new ZipFile(zipFile.getFile());
			zipFile.setPassword(this.password);
			zipFile.setCharset(Charset.forName(charset));
			//clearMessy();
			//List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			int size = zipFile.getFileHeaders().size();
			final Handler handler = new Handler(Looper.getMainLooper());
			handler.post(() -> {
				dialog.setTitle("正在解压");
				dialog.setMax(100);
				dialog.setProgress(0);
				dialog.setMessage(getWaitingMessage());
				dialog.show();
			});
			updateText("开始解压zip(共" + size + "个文件)\n若其中有文件名乱码将会自动识别，会增加解压时间，请耐心等待");
			try {
				String passwordStr = password == null?"":new String(password);
				ZipUtil.extractAll(zipFile.getFile().getPath(), new File(filePath), charset,passwordStr, new ZipUtil.Callback() {
					private long total = -1;
					@Override
					public void setTotal(long total) {
						this.total = total;
					}

					@Override
					public void onError(String filePath, Exception e) {
						updateText(filePath+": "+e.getMessage());
					}

					@Override
					public void setCompleted(long completed) {
						if(total > 0){
							double b = completed/(double)total;
							int m = (int) Math.floor(b*100);
							handler.post(()->{
								dialog.setTitle("正在解压");
								dialog.setMax(100);
								dialog.setProgress(m);
								dialog.setMessage(getWaitingMessage());
							});
						}
					}
				});
			}catch (Throwable e){
				Log.e("NonameImportActivity","解压失败",e);
				updateText("解压失败："+e.getMessage());
			}

			// 关闭对话框
			if (dialog != null) dialog.dismiss();
			updateText("解压完成！");
			if (hasConfigFile) {
				File configFile = new File(filePath, "noname.config.txt");
				if (configFile.exists() && configFile.isFile()) {
					try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = br.readLine()) != null) {
							sb.append(line);
							sb.append(System.lineSeparator());
						}
						getSharedPreferences("nonameshijian", MODE_PRIVATE)
								.edit()
								.putString("config", sb.toString())
								.apply();
						updateText("读取配置文件成功，启动后将自动导入");
						configFile.delete();
					} catch (IOException e) {
						updateText("读取配置文件异常: " + e.getMessage());
					}
				}
			} else updateText("该压缩包没有配置文件");
			// 清除缓存
			clearCache(cacheDir);
			// 进入无名杀
			if (extName != null) {
				afterFinishImportExtension(extName);
			} else {
				afterFinishImportExtension();
			}
		} catch (ZipException e) {
			Log.e("ZipException", e.getMessage());
			updateText("解压遇到错误，已停止解压：" + e.getMessage());
			hasError = true;
		}
	}

	/** 解析乱码文件名 */
	public String getFileName(FileHeader fileHeader) {
		String name = fileHeader.getFileName();
		/*
		try {
			if (Charset.forName("gbk").newEncoder().canEncode(name)) {
				return new String(name.getBytes("Cp437"), CharsetUtil.CHARSET_GBK.name());
			} else if (Charset.forName("gb2312").newEncoder().canEncode(name)) {
				return new String(name.getBytes("Cp437"), Charset.forName("gb2312"));
			} else {
				return new String(name.getBytes("Cp437"), CharsetUtil.CHARSET_UTF_8.name());
			}
		}catch (Exception e){
			return name;
		}*/

		boolean imc = isMessyCode(name);
		Log.e("name", name);
		Log.e("isMessyCode", String.valueOf(imc));
		String name_utf8 = name;
		String name_gbk = name;
		try {
			name_utf8 = new String(name.getBytes("Cp437"), CharsetUtil.CHARSET_UTF_8.name());
			name_gbk = new String(name.getBytes("Cp437"), CharsetUtil.CHARSET_GBK.name());
			Log.e("name-utf8", name_utf8);
			Log.e("name-gbk", name_gbk);
			Log.e("name", "——————————");
		} catch (Exception e) {
			Log.e("getFileName", e.getMessage());
		}
		if (!imc) {
			return name;
		}
		if(!isMessyCode(name_utf8)){
			updateText("use utf8 "+name+" "+name_utf8);
			return name_utf8;
		}else if(!isMessyCode(name_gbk)){
			updateText("use gbk "+name+" "+name_gbk);
			return name_gbk;
		}
		//updateText("not compact with:"+name);
		// 目前压缩包主要是两种来源WINdows和Linux
		return name;
		/*
		if (fileHeader.isFileNameUTF8Encoded()) {
			return name_utf8;
		} else {
			return name_gbk;
		}*/
	}

	/** 对FileHeader指定编码 */
	public String getFileName(FileHeader fileHeader, String charset) {
		String name = fileHeader.getFileName();
		String name_utf8 = name;
		String name_gbk = name;
		try {
			name_utf8 = new String(name.getBytes("Cp437"), CharsetUtil.CHARSET_UTF_8.name());
			name_gbk = new String(name.getBytes("Cp437"), CharsetUtil.CHARSET_GBK.name());
		} catch (Exception e) {
			Log.e("getFileName2", e.getMessage());
		}

		if ("utf-8".equals(charset)) {
			return name_utf8;
		} else if ("gbk".equals(charset)) {
			return name_gbk;
		} else {
			return name;
		}
	}

	// 显示解压进度条并解压文件
	private void showProgressDialogAndExtractAll(String filePath, File cacheDir, String extName) {
		runOnUiThread(() -> {
			dialog = new ProgressDialog(NonameImportActivity.this);
			dialog.setTitle("正在解压");
			dialog.setCancelable(false);
			dialog.setIcon(R.mipmap.ic_launcher);
			// 水平进度条
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

			Runnable runnable = () -> extractAll(filePath, cacheDir, extName);
			(new Thread(runnable)).start();
		});
	}

	// 递归删除非空文件夹
	private void removeDir(File file){
		//获取该文件下所有子文件和子文件夹
		File[] files = file.listFiles();

		//循环遍历数组中的所有子文件和文件夹
		if (files != null){
			//判断是否是文件，如果是，就删除
			for (File file1 : files) {
				if (file1.isFile()) {
					file1.delete();
				}
				//在循环中，判断遍历出的是否是文件夹
				if (file1.isDirectory()){
					// 如果是文件夹,就递归删除里面的文件
					removeDir(file1);
					// 删除该文件夹里所有文件后,当前文件夹就为空了,那么就可以删除该文件夹了
					file1.delete();
				}
			}
		}
		//删除完里面的文件夹后，当前文件夹也删除
		file.delete();
	}

	private String getWaitingMessage(){
		if(currentWaiting % WAITING_MESSAGES.length == 0){
			for(int i=0;i<WAITING_MESSAGES.length;i++){
				String a = WAITING_MESSAGES[i];
				int random = (int)(Math.random() * WAITING_MESSAGES.length);
				String b = WAITING_MESSAGES[random];
				WAITING_MESSAGES[i] = b;
				WAITING_MESSAGES[random] = a;
			}
			currentWaiting += 1;
		}
		String ret = WAITING_MESSAGES[currentWaiting % WAITING_MESSAGES.length];
		long ct = System.currentTimeMillis();
		if(ct - lastWaitingTime >= 3500){
			lastWaitingTime = ct;
			currentWaiting += 1;
		}
		return "小提示："+ret;
	}

	// 删除缓存
	private void clearCache(File cacheDir) {
		if (cacheDir != null) {
			updateText("正在清除缓存");
			try {
				removeDir(cacheDir);
				updateText("清除缓存成功！");
			} catch (Exception e) {
				updateText("清除缓存失败！" + e.getMessage());
			}
		}
		// 删除cache/currentLoadFile.zip文件
		try {
			cacheFile.delete();
		} catch (Exception e) {
			updateText("删除cache/currentLoadFile.zip失败: " + e.getMessage());
		}
	}

	// 安装解压完懒人包后，是否覆盖安装apk以节省资源
	private void installApk(File file) {
		if (file == null) {
			Log.e("install", "file is null");
			return;
		}

		try {
			//这里有文件流的读写，需要处理一下异常
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			Uri uri;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
				//如果SDK版本 =24，即：Build.VERSION.SDK_INT  = 24
				String packageName = getApplicationContext().getPackageName();
				String authority = new StringBuilder(packageName).append(".fileProvider").toString();
				uri = FileProvider.getUriForFile(this, authority, file);
				intent.setDataAndType(uri, "application/vnd.android.package-archive");
			} else{
				uri = Uri.fromFile(file);
				intent.setDataAndType(uri, "application/vnd.android.package-archive");
			}
			startActivity(intent);
			finish();
		} catch (Exception e) {
			Log.e("install", Objects.requireNonNull(e.getMessage()));
		}
	}

	private static Pattern messyFilterPattern = Pattern.compile("\\s*|\t*|\r*|\n*");

	/**
	 * 判断字符串是否包含乱码
	 * @param strName  需要判断的字符串
	 * @return 字符串包含乱码则返回true, 字符串不包含乱码则返回false
	 */
	public static boolean isMessyCode(String strName) {
		Pattern p = messyFilterPattern;
		Matcher m = p.matcher(strName);
		String after = m.replaceAll("");
		String temp = after.replaceAll("\\p{P}", "");
		char[] ch = temp.trim().toCharArray();
		float chLength = 0 ;
		float count = 0;
		for (int i = 0; i < ch.length; i++) {
			char c = ch[i];
			if (!isCertainlyNotMessyCode(c)) {
				if (!isChinese(c)) {
					count = count + 1;
				}
				chLength++;
			}
		}
		float result = count / chLength ;
		return result > 0.4;
	}

	private static boolean isCertainlyNotMessyCode(char c){
		if(Character.isLetterOrDigit(c))return true;
		return "!@#$%^&*()-_+/`~\\|[]{};:\",.<>/".contains(c+"");
	}

	/**
	 * 判断字符是否为中文
	 * @param c 字符
	 * @return 字符是中文返回 true, 否则返回false
	 */
	private static boolean isChinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}