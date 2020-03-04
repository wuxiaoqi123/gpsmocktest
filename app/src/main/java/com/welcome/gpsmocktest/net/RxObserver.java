package com.welcome.gpsmocktest.net;


import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class RxObserver<T> implements Observer<T> {

    @Override
    public void onSubscribe(Disposable d) {
    }

    @Override
    public void onNext(T t) {
        onSuccess(t);
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        onFail("-1", e.getMessage());
    }

    @Override
    public void onComplete() {
    }

    public abstract void onFail(String errCode, String errMsg);

    public abstract void onSuccess(T t);
}
