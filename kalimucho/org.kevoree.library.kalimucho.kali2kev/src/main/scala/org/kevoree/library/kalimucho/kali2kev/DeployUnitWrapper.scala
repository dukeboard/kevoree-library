package org.kevoree.library.kalimucho.kali2kev

import java.io.File
import org.kevoree.{KevoreeFactory, ContainerRoot, DeployUnit,NodeType}


/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 15/06/12
 * Time: 11:25
 */

trait DeployUnitWrapper {

  def getJarNameFromFile(cont : File) : String = {
    cont.getName
  }


  def getOrAdd(cont: File, model: ContainerRoot): DeployUnit = {
    model.getDeployUnits.find(du => du.getUnitName == getJarNameFromFile(cont)) match {
      case Some(pDU) => pDU
      case None => {
        val du = KevoreeFactory.createDeployUnit
        du.setUnitName(getJarNameFromFile(cont))

        du.setTargetNodeType(getKalimuchoNodeType(model))
        model.addDeployUnits(du)
        du
      }
    }
  }

  def getKalimuchoNodeType(model : ContainerRoot) : Option[NodeType] = {
    model.getTypeDefinitions.find(td => td.getName == "KalimuchoNode") match {
      case Some(pNT)=> Some(pNT.asInstanceOf[NodeType])
      case None => None
    }
  }

}
