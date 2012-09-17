package org.kevoree.library.kalimucho.kali2kev

import org.clapper.classutil.ClassInfo
import java.io.File
import org.kevoree.{TypeDefinition, KevoreeFactory, ComponentType, ContainerRoot}

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 15/06/12
 * Time: 11:23
 */

trait ComponentWrapper extends DeployUnitWrapper {

  def getNameFromClassInfo(ci: ClassInfo): String = {
    ci.name.substring(ci.name.lastIndexOf(".")+1)
  }

  def getOrAdd(origin : File,ci: ClassInfo, model: ContainerRoot): TypeDefinition = {

    model.getTypeDefinitions.find(td => td.getName == getNameFromClassInfo(ci)) match {
      case Some(pTD) => pTD
      case None => {
        val newCT = KevoreeFactory.createComponentType
        newCT.setName(getNameFromClassInfo(ci))
        newCT.setFactoryBean(ci.name)
        model.addTypeDefinitions(newCT)
        newCT.addDeployUnits(getOrAdd(origin,model))
        newCT
      }
    }
  }

}
