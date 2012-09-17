package org.kevoree.kalimucho.command;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 15/06/12
 * Time: 14:43
 */
public class KalimuchoNoopCommand extends KalimuchoKevoreeCommand {
    @Override
    public boolean execute() {
        return true;
    }

    @Override
    public void undo() {
    }
}
