package com.way.downloadlibrary.net.http;

import android.text.TextUtils;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public final class CookieHolder {

    private static final CookieHolder instance = new CookieHolder();

    private final Object lock = new Object();

    private Map<String, String> cookieValues;

    private CookieHolder() {
        cookieValues = new HashMap<>();
    }

    public static CookieHolder getInstance() {
        return instance;
    }

    public void resolveCookie(HttpURLConnection connection, String[] cookieKeys) {
        synchronized (lock) {
            for (int i = 1; ; i++) {
                String headFieldKey = connection.getHeaderFieldKey(i);
                if (TextUtils.isEmpty(headFieldKey)) {
                    break;
                }

                if ("set-cookie".equalsIgnoreCase(headFieldKey)) {
                    String headField = connection.getHeaderField(i);
                    String[] values = headField.split(";");
                    for (String value : values) {
                        String key = value.split("=")[0];
                        for (String cookieKey : cookieKeys) {
                            if (cookieKey != null && cookieKey.equalsIgnoreCase(key)) {
                                if (cookieValues.containsKey(key)) {
                                    cookieValues.remove(key);
                                }
                                cookieValues.put(key, value);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private String getCookie() {
        StringBuilder builder = new StringBuilder();

        int index = 0;
        for (String value : cookieValues.values()) {
            if (index > 0) {
                builder.append(";");
                builder.append(value);
            } else {
                builder.append(value);
            }
            index++;
        }

        return builder.toString();
    }

    public void setCookie(HttpURLConnection connection) {
        String cookie = getCookie();
        if (!TextUtils.isEmpty(cookie)) {
            connection.addRequestProperty("Cookie", cookie);
        }
    }

}
