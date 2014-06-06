package com.y59song.Utilities.Pool;

import java.util.concurrent.TimeUnit;

/**
 * Created by y59song on 28/05/14.
 */
public interface BlockingPool<T> extends IPool<T> {
  T get();
  T get(long time, TimeUnit unit) throws InterruptedException;
}
