package jp.co.moneyforward.autotest.framework.action;

import com.github.dakusui.actionunit.core.Action;

public class LeafActCall<T, R> extends ActCall<T, R> {
  
  private final LeafAct<T, R> act;
  
  public LeafActCall(String outputFieldName, LeafAct<T, R> act, String inputFieldName) {
    super(inputFieldName, outputFieldName);
    this.act = act;
  }
  
  public LeafAct<T, R> act() {
    return this.act;
  }
  
  @Override
  public Action toAction(ActionComposer actionComposer) {
    return actionComposer.create(this);
  }
}
