package com.noname.shijian;

public class FileView {
    private final String name;
    private final String type;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public FileView(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
