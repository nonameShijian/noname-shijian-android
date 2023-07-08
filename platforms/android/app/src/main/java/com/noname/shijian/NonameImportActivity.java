package com.noname.shijian;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

import cn.hutool.core.util.CharsetUtil;

public class NonameImportActivity extends Activity {
	/** 储存app版本号 */
	public static long VERSION = 10000L;

	public static final String[] WAITING_MESSAGES = {
			"无名杀的创造者是水乎，也叫村长。",
			"苏婆玛丽奥是无名杀的现任更新者。",
			"GPLv3协议提倡开源与共享，是无名杀代码的基础协议。",
			"原生的换肤功能不好用？装个千幻聆音扩展试试吧！可以语音图片一起换哦！",
			"在线更新扩展能避免更新时崩溃导致需要重装游戏的问题。",
			"萌新记着要看公告和教程哦，不要频繁提问，大佬很忙的。",
			"诗笺版无名杀的前身是玄武版，现在已经光荣退役了。",
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
			"广告位招租！不要money交个朋友！",
	};

	/** 是否取得权限 */
	// private boolean hasPermissions = true;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_begin);
		messageTextView = findViewById(R.id.messages);
		// updateText("Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
		ToastUtils.show(NonameImportActivity.this, "Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
		if(Build.VERSION.SDK_INT < 30) {
			/** 要申请的权限列表 */
			ArrayList<String> permissions = new ArrayList<>();

			/** 读取文件权限 */
			if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
				permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}

			/** 写入文件权限 */
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
			try {
				InputStream inputStream = getAssets().open("www/app/noname.zip");
				inputStream.close();
				loadAssetZip();
				ToastUtils.show(NonameImportActivity.this, "正在解压内置资源包");
			} catch (IOException e) {
				loadAssetExt();
				ToastUtils.show(NonameImportActivity.this, "正在更新SJ Settings扩展");
			}
		} else if (getIntent() != null && getIntent().getExtras() != null && "true".equals(getIntent().getExtras().getString("unzip"))) {
			try {
				InputStream inputStream = getAssets().open("www/app/noname.zip");
				inputStream.close();
				loadAssetZip();
				ToastUtils.show(NonameImportActivity.this, "正在解压内置资源包");
			} catch (IOException e) {
				loadAssetExt();
				ToastUtils.show(NonameImportActivity.this, "正在更新SJ Settings扩展");
			}
		} else {
			// ToastUtils.show(NonameImportActivity.this, "未通过无名杀打开zip");
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
						importPackage();
						return;
					} else if (zipFile.getFileHeader("extension.js") != null) {
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
							for (Object p: paths) {
								String p1 = p.toString();
								if (p1.length() < strLen || strLen == -1) {
									strLen = p1.length();
									path = p1;
								}
							}
							String rootPath = path.substring(0, path.indexOf("game/game.js"));
							// updateText("rootPath: " + rootPath);
							importPackage(rootPath);
							return;
						} else if (isExtension) {
							updateText("压缩包被识别成文件夹嵌套的扩展");
							if (!inited()) {
								updateText("检测到您的文件缺失不能进入游戏，所以暂时不能导入扩展。请先导入离线包/完整包，或者在游戏的初始界面下载文件(注: 当前更新源不稳定，不建议在游戏初始界面下载)");
								return;
							}
							Object[] paths = list.stream()
								.filter(fileHeader -> {
									String fileName = fileHeader.getFileName();
									return fileName.endsWith("/extension.js");
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
							String rootPath = path.substring(0, path.indexOf("extension.js"));
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
		String[] strings = new String[] {
				"extension.js",
				"extension.css",
		};
		for (String s : strings) {
			File result = Utils.assetToFile("www/SJSettings/" + s,this,"extension/SJ Settings/" + s);
			if (result == null) {
				updateText(s + "添加失败");
			}
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
		zipFile.extractFile("extension.js", cacheDir.getPath());
		File cacheJs = new File(cacheDir, "extension.js");
		try {
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
		zipFile.extractFile(rootPath + "extension.js", cacheDir.getPath());
		File cacheJs = new File(cacheDir, rootPath + "extension.js");
		String [] split = rootPath.split("/");
		new Thread() {
			public void run() {
				try {
					String extensionNameFromDir = split[split.length - 1];
					String extensionNameFromJs = null;
					extensionNameFromJs = getExtensionName(cacheJs);
					updateText("从文件夹名解析扩展名为: " + extensionNameFromDir);
					updateText("从js文件解析扩展名为: " + extensionNameFromJs);
					String extensionName;
					if (extensionNameFromDir.equals(extensionNameFromJs)) {
						extensionName = extensionNameFromDir;
					} else {
						updateText("解析结果不同，以文件夹的解析结果为准");
						extensionName = extensionNameFromDir;
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

		// 导入完完整包/离线包后，检查是否有内置扩展。如果没有就从apk目录里复制一个过去
		// 另，储存版本号
		if (extname.equals("importPackage")) {
			getSharedPreferences("nonameshijian", MODE_PRIVATE)
					.edit()
					.putLong("version",VERSION)
					.apply();
			File data = getExternalFilesDir(null).getParentFile();
			File extJs = new File(data, "extension/SJ Settings/extension.js");
			if (!extJs.exists()) {
				updateText("检测到您没有内置扩展，将为你自动添加'SJ Settings'扩展");
				File dir = extJs.getParentFile();
				if (!dir.exists()) {
					dir.mkdirs();
				}
				String[] strings = new String[] {
						"extension.js",
						"extension.css",
				};
				for (String s : strings) {
					File result =  Utils.assetToFile("www/SJSettings/" + s,this,"extension/SJ Settings/" + s);
					if (result == null) {
						updateText(s + "添加失败");
						return;
					}
				}
				/* File success =  Utils.assetToFile("www/SJ Settings/",this,"extension/SJ Settings/");
				if (success == null) {
					updateText("添加失败");
					return;
				}*/
				File file = new File(data, "extension/SJ Settings/extension.js");
				if (!file.exists()) {
					updateText("内置扩展添加失败");
				} else {
					// 修改game/config.js
					File config = new File(data, "game/config.js");
					if (!config.exists()) {
						updateText("检测到您没有game/config.js，将为您从内置资源中复制一份");
						File result =  Utils.assetToFile("www/game/config.js",this,"game/config.js");
						if (result == null) {
							updateText("内置资源game/config.js复制失败");
						}
					} else {
						try {
							updateText("检测到您有game/config.js，将为您添加内置扩展");
							Scanner scan = new Scanner(config);
							while (scan.hasNextLine()) {
								String line = scan.nextLine().trim();
								if (line.startsWith("extensions:")) {
									String extArray = line.substring(11, line.length() - 1).trim();
									// updateText("extArray: " + extArray);
									if (extArray.contains("SJ Settings")) break;
									// 修改文件，写入
									FileReader in = new FileReader(config);
									BufferedReader bufIn = new BufferedReader(in);
									// 内存流, 作为临时流
									CharArrayWriter tempStream = new CharArrayWriter();
									// 替换
									String lines;
									while ( (lines = bufIn.readLine()) != null) {
										// 替换每行中, 符合条件的字符串
										if (lines.trim().startsWith("extensions:")) {
											if (extArray.equals("[]")) {
												lines = lines.replace(extArray, "['SJ Settings']");
											} else {
												lines = lines.replace(extArray, "['SJ Settings', " + extArray.substring(1));
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
				}
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
					timer.schedule(new TimerTask(){
						public void run(){
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

	/** 不延时进入游戏 */
	private void afterFinishImportExtension() {
		if (dialog != null) {
			// 关闭对话框
			dialog.dismiss();
		}

		if (hasError) return;

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
					return ret;
				}
			}
			if (appear && (line.startsWith(s) || line.startsWith(s2))) {
				String str = line.startsWith(s) ? s : s2;
				int length = str.length();
				int index = line.indexOf(str);
				String ret = line.substring(index + length, line.indexOf('\"', index + length));
				if (ret.length() != 0) {
					return ret;
				}
			}
		}
		throw new ExtensionNameException("解析扩展名失败。");
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
			if(newMsg.length() >= 5000){
				newMsg = newMsg.substring(0,4999);
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

	private void clearMessy(){
		updateText("正在清除文件乱码");
		HashMap<String,String> renames = new HashMap<>();
		try {
			List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			int total = fileHeaders.size();
			new Handler(Looper.getMainLooper()).post(() -> {
				dialog.setTitle("正在扫描乱码文件");
				dialog.setMax(total);
				dialog.setProgress(0);
				dialog.setMessage(getWaitingMessage());
				dialog.show();
			});
			int p = 0;
			for (FileHeader fileHeader : fileHeaders) {
				String extractedFile = getFileName(fileHeader);
				if(!fileHeader.getFileName().equals(extractedFile)){
					renames.put(fileHeader.getFileName(),extractedFile);
				}
				p++;
				final int p2 = p;
				new Handler(Looper.getMainLooper()).post(() -> {
					dialog.setProgress(p2);
					dialog.setMessage(getWaitingMessage());
					dialog.show();
				});
			}
			updateText("检测到"+renames.size()+"个文件乱码，正在恢复，恢复时间较长，请耐心等待");
			zipFile.setRunInThread(true);
			new Handler(Looper.getMainLooper()).post(() -> {
				dialog.setTitle("正在修复乱码文件");
				dialog.setMax(1000);
				dialog.setProgress(0);
				dialog.setMessage(getWaitingMessage());
				dialog.show();
			});
			zipFile.renameFiles(renames);
			ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
			while (progressMonitor.getState() == ProgressMonitor.State.BUSY){
				Thread.sleep(100);
				int value = (int)((progressMonitor.getWorkCompleted()/(double)progressMonitor.getTotalWork())*1000);
				new Handler(Looper.getMainLooper()).post(() -> {
					dialog.setProgress(value);
					dialog.setMessage(getWaitingMessage());
				});
			}
			updateText("乱码修复完成！");
			zipFile.setRunInThread(false);
		}catch (Throwable e){

		}
	}

	// 解压文件
	private void extractAll(String filePath, File cacheDir, String extName) {
		zipFile.setCharset(StandardCharsets.UTF_8);
		updateText("Charset: UTF_8");
		try {
			List<FileHeader> fileHeaders = zipFile.getFileHeaders();
			clearMessy();
			int size = fileHeaders.size();
			new Handler(Looper.getMainLooper()).post(() -> {
				dialog.setTitle("正在解压");
				dialog.setMax(size);
				dialog.setProgress(0);
				dialog.setMessage(getWaitingMessage());
				dialog.show();
			});
			updateText("开始解压zip(共" + size + "个文件)\n若其中有文件名乱码将会自动识别，会增加解压时间，请耐心等待");
			for (int i = 0; i < size; i++) {
				FileHeader v = fileHeaders.get(i);
				if (v.isDirectory()) continue;
				// 解决乱码后文件路径
				String extractedFile = v.getFileName();
				// 但是原本的没变，需要改名
				/*
				if (!v.getFileName().equals(extractedFile)) {
					Log.e("renameFile", v.getFileName() + " to " + extractedFile);
					zipFile.renameFile(v, extractedFile);
				}*/
				try {
					zipFile.extractFile(v, filePath, extractedFile);
				} catch (ZipException e) {
					String message = e.getMessage().contains("Wrong password!") ? "压缩包密码错误，请重新解压" : e.getMessage();

					Log.e("解压失败", "filePath: " + filePath);
					Log.e("解压失败", extractedFile + "(" + (i + 1) + "/" + size + ")");
					Log.e("解压失败", message);
					Log.e("解压失败", "——————————");
					updateText("解压失败: " + extractedFile + "(" + (i + 1) + "/" + size + "): " + message);

					if (e.getMessage().contains("Wrong password!")) {
						hasError = true;
						dialog.dismiss();
						clearCache(cacheDir);
						return;
					}

					// 尝试换个编码，先换成utf8
					String oldName = v.getFileName();
					updateText("尝试更换编码解压: utf-8");
					extractedFile = getFileName(v, "utf-8");
					Log.e("renameFile", oldName + " to " + extractedFile);
					zipFile.renameFile(v, extractedFile);
					try {
						if (isMessyCode(extractedFile)) throw new ZipException("文件名为乱码: " + extractedFile);
						zipFile.extractFile(v, filePath, extractedFile);
						updateText("解压" + extractedFile + "成功");
						Log.e("utf-8解压成功", "——————————");
					} catch (ZipException err) {
						Log.e("utf-8解压失败", e.getMessage());
						Log.e("utf-8解压失败", "——————————");
						updateText("解压失败: " + e.getMessage());
						updateText("尝试更换编码解压: gbk");
						zipFile.renameFile(v, oldName);
						extractedFile = getFileName(v, "gbk");
						Log.e("renameFile", oldName + " to " + extractedFile);
						zipFile.renameFile(v, extractedFile);
						try {
							if (isMessyCode(extractedFile)) throw new ZipException("文件名为乱码: " + extractedFile);
							zipFile.extractFile(v, filePath, extractedFile);
							updateText("解压" + extractedFile + "成功");
							Log.e("gbk解压成功", "——————————");
						} catch (ZipException error) {
							Log.e("gbk解压失败", e.getMessage());
							Log.e("gbk解压失败", "——————————");
							updateText("解压失败: " + e.getMessage());
							hasError = true;
							dialog.dismiss();
							clearCache(cacheDir);
							return;
						}
					}
				}
				// updateText("解压成功 ：" + extractedFile + "(" + (i + 1) + "/" + size + ")");
				Message msg = handler.obtainMessage(0x0001);
				msg.arg1 = i;
				handler.sendMessage(msg);
			}
			// 关闭对话框
			if (dialog != null) dialog.dismiss();
			updateText("解压完成！");
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
		// 目前压缩包主要是两种来源WINdows和Linux
		if (fileHeader.isFileNameUTF8Encoded()) {
			return name_utf8;
		} else {
			return name_gbk;
		}
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

	/**
	 * 判断字符串是否包含乱码
	 * @param strText  需要判断的字符串
	 * @return 字符串包含乱码则返回true, 字符串不包含乱码则返回false
	 */
	private static boolean isMessyCode(String strText) {
		Pattern p = Pattern.compile("\\s*|\t*|\r*|\n*");
		Matcher m = p.matcher(strText);
		String after = m.replaceAll("");
		String temp = after.replaceAll("\\p{P}", "");
		char[] ch = temp.trim().toCharArray();
		float chLength = 0 ;
		float count = 0;
		for (int i = 0; i < ch.length; i++) {
			char c = ch[i];
			if (!Character.isLetterOrDigit(c)) {
				if (!isChinese(c)) {
					count = count + 1;
				}
				chLength++;
			}
		}
		float result = count / chLength ;
		if (result > 0.4) {
			return true;
		} else {
			return false;
		}
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