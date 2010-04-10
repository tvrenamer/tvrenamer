package com.google.code.tvrenamer.model.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple wrapper around the JDK logging framework to make it easier to work
 * with
 * 
 * @author Dave Harris
 * 
 */
public class TVRenamerLogger {

  private Logger logger;

  /** Private default constructor */
  private TVRenamerLogger() {
  }

  /**
   * Create a JDK {@link Logger} for the class
   * 
   * @param clazz
   *          the class to create a logger for
   * @return a {@link Logger} for the given class
   */
  @SuppressWarnings("unchecked")
  public TVRenamerLogger(Class clazz) {
    logger = Logger.getLogger(clazz.getCanonicalName());
  }

  public void trace(String message) {
    logger.log(Level.FINER, message);
  }

  public void debug(String message) {
    logger.log(Level.FINE, message);
  }

  public void info(String message) {
    logger.log(Level.INFO, message);
  }

  public void warn(String message) {
    logger.log(Level.WARNING, message);
  }

  public void warn(String message, Throwable th) {
    logger.log(Level.WARNING, message, th);
  }

  public void error(String message) {
    logger.log(Level.SEVERE, message);
  }

  public void error(String message, Throwable th) {
    logger.log(Level.SEVERE, message, th);
  }

}
