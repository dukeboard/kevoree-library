package org.kevoree.kalimucho;

import org.kevoree.ComponentInstance;
import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.TypeDefinition;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.kalimucho.command.AddKalimuchoDeployUnit;
import org.kevoree.kalimucho.command.AddKalimuchoInstance;
import org.kevoree.kalimucho.command.KalimuchoKevoreeCommand;
import org.kevoree.kalimucho.command.KalimuchoNoopCommand;
import org.kevoree.kompare.JavaSePrimitive$;
import org.kevoreeAdaptation.AdaptationPrimitive;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/06/12
 * Time: 19:12
 */
public class CommandMapper {

    private KalimuchoNode superNode = null;
    private HashMap<String, KalimuchoKevoreeCommand> kaliCmds = new HashMap<String, KalimuchoKevoreeCommand>();

    public CommandMapper(KalimuchoNode sup) {
        superNode = sup;
        //Init Kalimucho mapper
        kaliCmds.put(JavaSePrimitive$.MODULE$.AddDeployUnit(),new AddKalimuchoDeployUnit());
        kaliCmds.put(JavaSePrimitive$.MODULE$.AddInstance(),new AddKalimuchoInstance());
        kaliCmds.put(JavaSePrimitive$.MODULE$.AddType(),new KalimuchoNoopCommand());
        kaliCmds.put(JavaSePrimitive$.MODULE$.UpdateDictionaryInstance(),new KalimuchoNoopCommand());
        kaliCmds.put(JavaSePrimitive$.MODULE$.StartInstance(),new KalimuchoNoopCommand());

    }

    public PrimitiveCommand getPrimitive(AdaptationPrimitive adaptationPrimitive) {
        if (isKalimuchoManaged(adaptationPrimitive.getRef()) && kaliCmds.containsKey(adaptationPrimitive.getPrimitiveType().getName())) {
            KalimuchoKevoreeCommand cmd = kaliCmds.get(adaptationPrimitive.getPrimitiveType().getName());
            cmd.setAdaptationPrimitive(adaptationPrimitive);
            cmd.setOrigin(superNode);
            return cmd;
        } else {
            return superNode.getSuperPrimitive(adaptationPrimitive);
        }
    }

    /* Ugly method to determine if component is Kalimucho nature or Kevoree
     * Due to inheritance with JavaSENode
      * */
    private boolean isKalimuchoManaged(Object obj) {
        if (obj instanceof org.kevoree.MBinding) {
            ComponentInstance cinstance = ((ComponentInstance) ((org.kevoree.MBinding) obj).getPort().eContainer());
            for (DeployUnit du : cinstance.getTypeDefinition().getDeployUnitsForJ()) {
                if (du.getTargetNodeType().isDefined() && du.getTargetNodeType().get().getName().equals(KalimuchoNode.class.getSimpleName())) {
                    return true;
                }
            }
        }
        if (obj instanceof org.kevoree.Instance) {
            org.kevoree.Instance kinstance = (Instance) obj;
            for (DeployUnit du : kinstance.getTypeDefinition().getDeployUnitsForJ()) {
                if (du.getTargetNodeType().isDefined() && du.getTargetNodeType().get().getName().equals(KalimuchoNode.class.getSimpleName())) {
                    return true;
                }
            }
        }
        if(obj instanceof DeployUnit){
            DeployUnit kdu = (DeployUnit) obj;
            if (kdu.getTargetNodeType().isDefined() && kdu.getTargetNodeType().get().getName().equals(KalimuchoNode.class.getSimpleName())) {
                 return true;
            }
        }
        if(obj instanceof TypeDefinition){
            TypeDefinition kTD = (TypeDefinition) obj;
            for(DeployUnit kdu : kTD.getDeployUnitsForJ()){
                if (kdu.getTargetNodeType().isDefined() && kdu.getTargetNodeType().get().getName().equals(KalimuchoNode.class.getSimpleName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
