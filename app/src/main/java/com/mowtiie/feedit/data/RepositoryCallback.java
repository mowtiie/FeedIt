package com.mowtiie.feedit.data;

public interface RepositoryCallback<T> {
    void onComplete(T result);
    void onError(Exception e);
}