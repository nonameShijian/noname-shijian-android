package com.noname.shijian.chain;

public abstract class TaskNode<T> {

    protected boolean done = false;

    private boolean failed = false;

    protected TaskChain<T> chain;

    protected int index = 0;

    public abstract void handle(T param);

    public abstract float progress();

    public abstract float weight();

    public void updateProgress(){
        if(chain!=null){
            chain.updateProgress();
        }
    }

    public final void done(T param){
        if(chain!=null){
            chain.onTaskDone(this,param);
        }
        done = true;
    }

    public boolean isDone() {
        return done;
    }
}
