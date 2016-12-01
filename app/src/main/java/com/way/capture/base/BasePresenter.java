package com.way.capture.base;


public abstract class BasePresenter<T> {
    protected T mView;

    public abstract void unSubscribe();
}
