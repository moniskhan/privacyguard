package com.y59song.Utilities.Pool;

/**
 * Created by y59song on 28/05/14.
 */
public interface ObjectFactory<T> {
  public abstract T createNew();
}
