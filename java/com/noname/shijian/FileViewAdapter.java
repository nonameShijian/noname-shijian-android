package com.noname.shijian;

import android.annotation.SuppressLint;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FileViewAdapter extends ArrayAdapter {
    private final int resourceId;

    public FileViewAdapter(Context context, int textViewResourceId, List<FileView> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前项的实例
        FileView fileView = (FileView) getItem(position);
        //实例化一个对象
        @SuppressLint("ViewHolder") View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
        //获取该布局内的图片视图
        ImageView image = (ImageView) view.findViewById(R.id.fileView_image);
        //获取该布局内的文本视图
        TextView name = (TextView) view.findViewById(R.id.fileView_name);
        //为图片视图设置图片资源
        image.setImageResource(fileView.getType().equals("file") ? R.mipmap.ic_file : R.mipmap.ic_folder);
        //为文本视图设置文本内容
        name.setText(fileView.getName());
        return view;
    }
}
