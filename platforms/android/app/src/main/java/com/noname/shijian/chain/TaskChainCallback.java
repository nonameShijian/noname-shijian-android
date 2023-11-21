package com.noname.shijian.chain;

public interface TaskChainCallback<T> {

    void onTaskDone(TaskNode<T> task);

    void onTaskFailed(TaskNode<T> task);

}
