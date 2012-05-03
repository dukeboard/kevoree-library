package org.kevoree.library.voldemortChannels.demo;

import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 26/04/12
 * Time: 15:51
 */
@Library(name = "JavaSE", names = {"Android"})
@Provides(value = {
        @ProvidedPort(name = "msg", type = PortType.MESSAGE)
})
@ComponentType
public class Cmoyens extends AbstractComponentType  {

    private static final Logger logger = LoggerFactory.getLogger(Cmoyens.class);
    private FrameMoyens frame = null;

    @Start
    public void start() throws Exception {
        frame = new FrameMoyens(getNodeName());
    }

    @Stop
    public void stop() {
        frame.dispose();
        frame = null;
    }


    @Update
    public void update() {

    }


    @Port(name = "msg")
    public void msg(Object msg) {
        String rowData[][]  = (String[][]) msg;
        logger.debug("update  = "+rowData[0][1]);
        frame.update(rowData);
    }


}
