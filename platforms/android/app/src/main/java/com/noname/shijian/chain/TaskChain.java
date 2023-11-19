package com.noname.shijian.chain;

import java.util.ArrayList;

public abstract class TaskChain<T> extends TaskNode<T>{

    private ArrayList<TaskNode<T>> taskNodes = new ArrayList<>();

    @Override
    public void handle(T param) {
        if(taskNodes.size() == 0){
            done(param);
            return;
        }
        TaskNode<T> node = taskNodes.get(0);
        node.handle(param);
    }

    public TaskChain<T> addNode(TaskNode<T> node){
        node.chain = this;
        node.index = taskNodes.size();
        taskNodes.add(node);
        return this;
    }

    @Override
    public float progress() {
        float weightSum = 0;
        float weight = 0;
        for(TaskNode<T> node:taskNodes){
            weightSum += node.weight();
            weight += node.weight() * node.progress();
        }
        return weight/weightSum;
    }

    @Override
    public float weight() {
        return 1;
    }

    public void onTaskDone(TaskNode<T> node,T param){
        int nextIndex = node.index+1;
        if(nextIndex<taskNodes.size()){
            taskNodes.get(nextIndex).handle(param);
        }else{
            done(param);
        }
    }
}
