package com.y59song.Utilities.Pool;

/**
 * Created by y59song on 28/05/14.
 */
public interface IPool<T> {
  T get();

  void release(T t);

  void shutdown();

  /**
   * Created by y59song on 28/05/14.
   */
  public static interface Validator<T> {
    public boolean isValid(T t);
    public void invalidate(T t);
  }
}
