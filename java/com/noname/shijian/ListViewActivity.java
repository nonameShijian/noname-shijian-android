package com.noname.shijian;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListViewActivity extends Activity {
    // app在data里的位置
    private File appPath;
    // 目前的地址
    private File path;
    // 指定目录下所有文件
    private String[] data;
    // ListView需要的
    private FileViewAdapter adapter;
    // ArrayList
    private final List<FileView> FileViewList = new ArrayList<>();
    // ListView
    private ListView listView;
    // 标题
    private TextView title;
    // 确定按钮
    private Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);
        title = (TextView) findViewById(R.id.title);
        button = (Button) findViewById(R.id.button);
        // 初始路径为app根目录
        appPath = path = getExternalFilesDir(null).getParentFile();
        // 该文件夹下所有文件/文件夹
        data = path.list();
        // 排序
        Arrays.sort(data, String::compareToIgnoreCase);
        for (String fileName: data) {
            FileViewList.add(new FileView(fileName, (new File(path, fileName).isDirectory()) ? "folder" : "file"));
        }
        adapter = new FileViewAdapter(ListViewActivity.this, R.layout.activity_listview_item, FileViewList);
        listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        // 点击事件
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // 获取实例
            FileView fv = FileViewList.get(position);
            // 根据文件类型做出不同反映
            Bundle extras = getIntent().getExtras();
            switch (fv.getType()) {
                case "file":
                    if (extras != null) {
                        String type = getIntent().getExtras().getString("type");
                        if ("folder".equals(type)) return;
                    }
                    // 创建退出对话框
                    AlertDialog isExit = new AlertDialog.Builder(this).create();
                    // 设置对话框标题
                    isExit.setTitle("系统提示");
                    // 设置对话框消息
                    isExit.setMessage("确定选择" + fv.getName() + "吗");
                    // 添加选择按钮并注册监听
                    isExit.setButton(Dialog.BUTTON_POSITIVE,"确定", (dialog, which) -> {
                        // "确认"按钮退出程序
                        if (which == AlertDialog.BUTTON_POSITIVE) {
                            Intent data = new Intent();
                            String path = title.getText().toString();
                            data.putExtra("path", path + "/" + fv.getName());
                            String type = getIntent().getStringExtra("type");
                            if (type != null) {
                                data.putExtra("type", type);
                            }
                            String readFile = getIntent().getStringExtra("readFile");
                            if (readFile != null) {
                                data.putExtra("readFile", readFile);
                            }
                            //设置结果
                            setResult(3, data);
                            finish();
                        }
                    });
                    isExit.setButton(Dialog.BUTTON_NEGATIVE, "取消", (dialog, which) -> {});
                    // 显示对话框
                    isExit.show();
                    break;
                case "folder":
                    if (!fv.getName().equals("..")) {
                        path = new File(path, fv.getName());
                    } else {
                        path = path.getParentFile();
                    }
                    data = path.list();
                    Arrays.sort(data, String::compareToIgnoreCase);
                    FileViewList.clear();
                    // title.setText(path.getAbsolutePath().substring(33));
                    title.setText(getPackageName() + path.getAbsolutePath().replace(appPath.getAbsolutePath(), ""));
                    if (!path.equals(appPath)) FileViewList.add(new FileView("..", "folder"));
                    for (String fileName: data) {
                        FileViewList.add(new FileView(fileName, (new File(path, fileName).isDirectory()) ? "folder" : "file"));
                    }
                    adapter = new FileViewAdapter(ListViewActivity.this, R.layout.activity_listview_item, FileViewList);
                    listView.setAdapter(adapter);
                    break;
            }
        });

        // 确定选择目录/文件
        button.setOnClickListener((View v) -> {
            Intent data = new Intent();
            String path = title.getText().toString();
            data.putExtra("path", path);
            String type = getIntent().getStringExtra("type");
            if (type != null) {
                data.putExtra("type", type);
            }
            //设置结果
            setResult(3, data);
            //关闭当前界面
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        if (appPath.equals(path)) {
            // 创建退出对话框
            AlertDialog isExit = new AlertDialog.Builder(this).create();
            // 设置对话框标题
            isExit.setTitle("系统提示");
            // 设置对话框消息
            isExit.setMessage("确定要退出此页面吗");
            // 添加选择按钮并注册监听
            isExit.setButton(Dialog.BUTTON_POSITIVE, "确定", listener);
            isExit.setButton(Dialog.BUTTON_NEGATIVE, "取消", listener);
            // 显示对话框
            isExit.show();
        } else {
            // 清空
            FileViewList.clear();
            // 返回上一页
            path = path.getParentFile();
            // 修改标题
            title.setText(getPackageName() + path.getAbsolutePath().replace(appPath.getAbsolutePath(), ""));
            // 该文件夹下所有文件/文件夹
            data = path.list();
            // 排序
            Arrays.sort(data, String::compareToIgnoreCase);
            for (String fileName: data) {
                FileViewList.add(new FileView(fileName, (new File(path, fileName).isDirectory()) ? "folder" : "file"));
            }
            adapter = new FileViewAdapter(ListViewActivity.this, R.layout.activity_listview_item, FileViewList);
            listView = (ListView) findViewById(R.id.list_view);
            listView.setAdapter(adapter);

            // 点击事件
            listView.setOnItemClickListener((parent, view, position, id) -> {
                // 获取实例
                FileView fv = FileViewList.get(position);
                // 根据文件类型做出不同反映
                Bundle extras = getIntent().getExtras();
                switch (fv.getType()) {
                    case "file":
                        if (extras != null) {
                            String type = getIntent().getExtras().getString("type");
                            if ("folder".equals(type)) return;
                        }
                        // 创建退出对话框
                        AlertDialog isExit = new AlertDialog.Builder(this).create();
                        // 设置对话框标题
                        isExit.setTitle("系统提示");
                        // 设置对话框消息
                        isExit.setMessage("确定选择" + fv.getName() + "吗");
                        // 添加选择按钮并注册监听
                        isExit.setButton(Dialog.BUTTON_POSITIVE,"确定", (dialog, which) -> {
                            // "确认"按钮退出程序
                            if (which == AlertDialog.BUTTON_POSITIVE) {
                                Intent data = new Intent();
                                String path = title.getText().toString();
                                data.putExtra("path", path + "/" + fv.getName());
                                String type = getIntent().getStringExtra("type");
                                if (type != null) {
                                    data.putExtra("type", type);
                                }
                                String readFile = getIntent().getStringExtra("readFile");
                                if (readFile != null) {
                                    data.putExtra("readFile", readFile);
                                }
                                //设置结果
                                setResult(3, data);
                                finish();
                            }
                        });
                        isExit.setButton(Dialog.BUTTON_NEGATIVE, "取消", (dialog, which) -> {});
                        // 显示对话框
                        isExit.show();
                        break;
                    case "folder":
                        if (!fv.getName().equals("..")) {
                            path = new File(path, fv.getName());
                        } else {
                            path = path.getParentFile();
                        }
                        data = path.list();
                        Arrays.sort(data, String::compareToIgnoreCase);
                        FileViewList.clear();
                        title.setText(getPackageName() + path.getAbsolutePath().replace(appPath.getAbsolutePath(), ""));
                        if (!path.equals(appPath)) FileViewList.add(new FileView("..", "folder"));
                        for (String fileName: data) {
                            FileViewList.add(new FileView(fileName, (new File(path, fileName).isDirectory()) ? "folder" : "file"));
                        }
                        adapter = new FileViewAdapter(ListViewActivity.this, R.layout.activity_listview_item, FileViewList);
                        listView.setAdapter(adapter);
                        break;
                }
            });

            // 确定选择目录/文件
            button.setOnClickListener((View v) -> {
                Intent data = new Intent();
                String path = title.getText().toString();
                data.putExtra("path", path);
                String type = getIntent().getStringExtra("type");
                if (type != null) {
                    data.putExtra("type", type);
                }
                //设置结果
                setResult(3, data);
                //关闭当前界面
                finish();
            });
        }
        // super.onBackPressed();
    }

    // 监听对话框里面的button点击事件
    DialogInterface.OnClickListener listener = (dialog, which) -> {
        // "确认"按钮退出程序
        if (which == AlertDialog.BUTTON_POSITIVE) {
            Intent data = new Intent();
            String type = getIntent().getStringExtra("type");
            if (type != null) {
                data.putExtra("type", type);
            }
            String readFile = getIntent().getStringExtra("readFile");
            if (readFile != null) {
                data.putExtra("readFile", readFile);
            }
            // 设置结果
            setResult(2, data);
            finish();
        }
    };
}
