package com.y59song.Utilities.RealPool;

import com.y59song.Forwader.AbsForwarder;
import com.y59song.Utilities.Pool.IPool.*;

/**
 * Created by y59song on 28/05/14.
 */
public class ForwarderValidator implements Validator<AbsForwarder> {
  @Override
  public boolean isValid(AbsForwarder forwarder) {
    return forwarder == null || forwarder.isClosed();
  }

  @Override
  public void invalidate(AbsForwarder forwarder) {
    forwarder.close();
  }
}
