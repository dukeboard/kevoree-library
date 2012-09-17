package org.kevoree.kalimucho.command;

import org.kevoree.DeployUnit;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.kalimucho.KalimuchoNode;
import platform.ClassManager.ClassLoaderFromJarFile;
import platform.ClassManager.KalimuchoClassLoader;
import platform.ClassManager.LoadedClass;

import java.io.File;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/06/12
 * Time: 12:06
 */
public class AddKalimuchoDeployUnit extends KalimuchoKevoreeCommand {

    public static String defaultRepo = ".." + File.separator + "depotComposants" + File.separator + "PC" + File.separator;

    @Override
    public boolean execute() {
        DeployUnit kdu = (DeployUnit) adaptationPrimitive.getRef();
        File jarFile = new File(defaultRepo + kdu.getUnitName());
        if (jarFile.exists()) {
            KalimuchoClassLoader.addClassLoader(new ClassLoaderFromJarFile(KalimuchoNode.class.getClassLoader(), jarFile.getAbsolutePath()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void undo() {
    }
}
