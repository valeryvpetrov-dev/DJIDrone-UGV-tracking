package ru.kpfu.itis.robotics.dji_activetrack.util;

import android.content.Context;

/**
 * Created by valera071998@gmail.com on 10.05.2018.
 */
public class StringUtil {

    private Context context;

    public StringUtil(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void addLine(StringBuffer builder, String key, Object value){
        if (builder != null) {
            builder.append((key != null && key.trim().length() > 0) ? key + " : " : "");
            builder.append((value != null) ? value : "");
            builder.append("\n");
        }
    }

    public String getStringByResId(int resId) {
        return context.getString(resId);
    }
}