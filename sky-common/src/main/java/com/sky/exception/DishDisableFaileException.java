package com.sky.exception;

/**
 * 菜品停售失败异常
 */
public class DishDisableFaileException extends BaseException {
  public DishDisableFaileException(){}

  public DishDisableFaileException(String msg){
    super(msg);
  }
}
