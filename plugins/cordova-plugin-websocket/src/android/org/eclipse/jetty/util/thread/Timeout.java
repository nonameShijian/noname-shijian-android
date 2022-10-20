//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util.thread;


/* ------------------------------------------------------------ */
/** Timeout queue.
 * This class implements a timeout queue for timers that are at least as likely to be cancelled as they are to expire.
 * Unlike the util timeout class, the duration of the timeouts is shared by all scheduled tasks and if the duration
 * is changed, this affects all scheduled tasks.
 * <p>
 * The nested class Task should be extended by users of this class to obtain call back notification of
 * expires.
 */
public class Timeout
{
    private Object _lock;
    private long _duration;
    private volatile long _now=System.currentTimeMillis();
    private Task _head=new Task();

    /* ------------------------------------------------------------ */
    public Timeout(Object lock)
    {
        _lock=lock;
        _head._timeout=this;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param duration The duration to set.
     */
    public void setDuration(long duration)
    {
        _duration = duration;
    }

    /* ------------------------------------------------------------ */
    public long getNow()
    {
        return _now;
    }

    /* ------------------------------------------------------------ */
    public void setNow(long now)
    {
        _now=now;
    }

    /* ------------------------------------------------------------ */
    /** Get an expired tasks.
     * This is called instead of {@link #tick()} to obtain the next
     * expired Task, but without calling it's {@link Task#expire()} or
     * {@link Task#expired()} methods.
     *
     * @return the next expired task or null.
     */
    public Task expired()
    {
        synchronized (_lock)
        {
            long _expiry = _now-_duration;

            if (_head._next!=_head)
            {
                Task task = _head._next;
                if (task._timestamp>_expiry)
                    return null;

                task.unlink();
                task._expired=true;
                return task;
            }
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public void cancelAll()
    {
        synchronized (_lock)
        {
            _head._next=_head._prev=_head;
        }
    }

    /* ------------------------------------------------------------ */
    public long getTimeToNext()
    {
        synchronized (_lock)
        {
            if (_head._next==_head)
                return -1;
            long to_next = _duration+_head._next._timestamp-_now;
            return to_next<0?0:to_next;
        }
    }

    /* ------------------------------------------------------------ */
    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(super.toString());

        Task task = _head._next;
        while (task!=_head)
        {
            buf.append("-->");
            buf.append(task);
            task=task._next;
        }

        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Task.
     * The base class for scheduled timeouts.  This class should be
     * extended to implement the expire() method, which is called if the
     * timeout expires.
     *
     *
     *
     */
    public static class Task
    {
        Task _next;
        Task _prev;
        Timeout _timeout;
        long _delay;
        long _timestamp=0;
        boolean _expired=false;

        /* ------------------------------------------------------------ */
        protected Task()
        {
            _next=_prev=this;
        }

        /* ------------------------------------------------------------ */
        private void unlink()
        {
            _next._prev=_prev;
            _prev._next=_next;
            _next=_prev=this;
            _expired=false;
        }
    }
}
