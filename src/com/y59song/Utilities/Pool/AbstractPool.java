package com.y59song.Utilities.Pool;

/**
 * Created by y59song on 28/05/14.
 */
public abstract class AbstractPool<T> implements IPool<T> {

  @Override
  public final void release(T t) {
    if(isValid(t)) returnToPool(t);
    else handleInvalidReturn(t);
  }

  protected abstract void handleInvalidReturn(T t);
  protected abstract void returnToPool(T t);
  protected abstract boolean isValid(T t);
}
