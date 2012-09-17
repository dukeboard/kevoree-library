package org.kevoree.kalimucho;

import network.platform.NetworkPlatformMessage;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.Library;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.defaultNodeTypes.JavaSENode;
import org.kevoreeAdaptation.AdaptationModel;
import org.kevoreeAdaptation.AdaptationPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import platform.ClassManager.ClassLoaderFromJarFile;
import platform.ClassManager.KalimuchoClassLoader;
import platform.ClassManager.LoadedClass;
import platform.Platform;
import platform.supervisor.CommandAnalyser;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/06/12
 * Time: 12:01
 */
@Library(name = "Kalimucho")
@NodeType
public class KalimuchoNode extends JavaSENode {

    protected Platform plateform = null;
    private Logger logger = LoggerFactory.getLogger(KalimuchoNode.class);
    private CommandMapper mapper = null;

    public Platform getPlateform(){
        return plateform;
    }

    @Start
    @Override
    public void startNode() {
        super.startNode();
        mapper = new CommandMapper(this);
        plateform = new  Platform();
        logger.info("Kalimucho Kevoree Node Started "+getNodeName());
    }


    @Stop
    @Override
    public void stopNode() {
        plateform.stop();
        plateform = null;
        mapper = null;
        super.stopNode();
    }

    @Override
    public AdaptationModel kompare(ContainerRoot current, ContainerRoot target) {
        return super.kompare(current, target);
    }

    @Override
    public PrimitiveCommand getPrimitive(AdaptationPrimitive adaptationPrimitive) {
        return mapper.getPrimitive(adaptationPrimitive);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PrimitiveCommand getSuperPrimitive(AdaptationPrimitive adaptationPrimitive){
        return super.getPrimitive(adaptationPrimitive);
    }

}
