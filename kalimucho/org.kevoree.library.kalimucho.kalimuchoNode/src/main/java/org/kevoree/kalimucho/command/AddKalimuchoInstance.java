package org.kevoree.kalimucho.command;

import org.kevoree.Instance;
import platform.supervisor.CommandAnalyser;
import platform.supervisor.CommandSyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/06/12
 * Time: 19:17
 */
public class AddKalimuchoInstance extends KalimuchoKevoreeCommand {
    @Override
    public boolean execute() {
        try {
            Instance inst = (Instance) adaptationPrimitive.getRef();
            origin.getPlateform().getSupervisor().creerComposant(inst.getName(),inst.getTypeDefinition().getFactoryBean(),new CommandAnalyser(inst.getTypeDefinition().getFactoryBean()+" [null] [null]"));
            return true;
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void undo() {
    }
}
