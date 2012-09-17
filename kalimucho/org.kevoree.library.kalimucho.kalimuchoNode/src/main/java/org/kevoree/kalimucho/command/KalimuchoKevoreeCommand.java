package org.kevoree.kalimucho.command;

import org.kevoree.api.PrimitiveCommand;
import org.kevoree.kalimucho.KalimuchoNode;
import org.kevoreeAdaptation.AdaptationPrimitive;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/06/12
 * Time: 19:33
 */
public abstract class KalimuchoKevoreeCommand implements PrimitiveCommand {

    protected AdaptationPrimitive adaptationPrimitive = null;

    public void setAdaptationPrimitive(AdaptationPrimitive ap) {
        adaptationPrimitive = ap;
    }

    protected KalimuchoNode origin = null;

    public void setOrigin(KalimuchoNode o) {
        this.origin = o;
    }

}
