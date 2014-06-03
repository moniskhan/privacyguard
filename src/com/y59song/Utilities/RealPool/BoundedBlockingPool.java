package com.y59song.Utilities.RealPool;

import com.y59song.Utilities.Pool.AbstractPool;
import com.y59song.Utilities.Pool.BlockingPool;
import com.y59song.Utilities.Pool.ObjectFactory;

import java.util.concurrent.*;

/**
 * Created by y59song on 28/05/14.
 */
public final class BoundedBlockingPool<T>
  extends AbstractPool<T>
  implements BlockingPool<T> {
  private int size;
  private BlockingQueue objects;
  private Validator validator;
  private ObjectFactory objectFactory;
  private ExecutorService executor = Executors.newCachedThreadPool();

  private volatile boolean shutdownCalled;

  public BoundedBlockingPool(int size, Validator validator, ObjectFactory objectFactory) {
    super();
    this.objectFactory = objectFactory;
    this.size = size;
    this.validator = validator;
    objects = new LinkedBlockingQueue(size);
    initializeObjects();
    shutdownCalled = false;
  }

  public T get(long timeout, TimeUnit unit) {
    if(!shutdownCalled) {
      T t = null;
      try {
        synchronized(objects) {
          t = (T) objects.poll(timeout, unit);
        }
      } catch(InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      return t;
    }
    throw new IllegalStateException("Pool is already shutdown");
  }

  public T get() {
    if(!shutdownCalled) {
      T t = null;
      try {
        synchronized(objects) {
          t = (T) objects.take();
        }
      } catch(InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      return t;
    }
    throw new IllegalStateException("Pool is already shutdown");
  }

  public void shutdown() {
    shutdownCalled = true;
    executor.shutdownNow();
    clearResources();
  }

  private void clearResources() {
    for(Object t : objects) validator.invalidate(t);
  }

  @Override
  protected void returnToPool(T t) {
    if(validator.isValid(t))
      executor.submit(new ObjectReturner(objects, t));
  }

  @Override
  protected void handleInvalidReturn(T t) { }

  @Override
  protected boolean isValid(T t) {
    return validator.isValid(t);
  }

  private void initializeObjects() {
    for(int i = 0; i < size; i ++)
      objects.add(objectFactory.createNew());
  }

  private class ObjectReturner<E> implements Callable<E> {
    private BlockingQueue queue;
    private E e;
    public ObjectReturner(BlockingQueue queue, E e) {
      this.queue = queue;
      this.e = e;
    }
    @Override
    public E call() throws Exception {
      while(true) {
        try {
          synchronized(queue) {
            queue.put(e);
          }
          break;
        } catch(InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
      return null;
    }
  }
}

