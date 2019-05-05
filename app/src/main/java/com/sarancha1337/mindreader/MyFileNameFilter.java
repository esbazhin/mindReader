package com.sarancha1337.mindreader;

import java.io.File;
import java.io.FilenameFilter;

class MyFileNameFilter implements FilenameFilter {

    private String ext;

    MyFileNameFilter(String ext){
        this.ext = ext.toLowerCase();
    }
    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(ext);
    }
}