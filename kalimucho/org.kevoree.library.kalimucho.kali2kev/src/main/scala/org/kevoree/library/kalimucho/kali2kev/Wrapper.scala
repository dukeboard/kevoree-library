package org.kevoree.library.kalimucho.kali2kev

import io.Source
import java.io.File
import scala.collection.JavaConversions._
import org.kevoree.KevoreeFactory
import org.kevoree.framework.KevoreeXmiHelper

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 15/06/12
 * Time: 00:33
 */

object Wrapper extends App with ComponentWrapper {

  println("Proto Kevoree Kalimucho Wrapper")
  val f = new File(".."+File.separator+"depotComposants"+File.separator+"PC")

  //TODO NOT HARD CODED PATH ;-)

  var collectedJar = new scala.collection.mutable.HashSet[File]()
  f.listFiles().foreach{ lf =>
     if(lf.getName.endsWith(".jar")){
       collectedJar.add(lf)
     }
  }

  import org.clapper.classutil.ClassFinder
  val classpath = collectedJar.toList
  val finder = ClassFinder(classpath)

  val classes = finder.getClasses().filter(_.superClassName == "model.osagaia.BCModel")
  val kevModel = KevoreeXmiHelper.loadStream(this.getClass.getClassLoader.getResourceAsStream("KEV-INF/lib.kev"))//LOAD BASE MODEL
  classes.foreach(c => {
    getOrAdd(c.location,c,kevModel)
  })

  KevoreeXmiHelper.save(f.getAbsolutePath+File.separator+"lib.kev",kevModel)


  //println(KevoreeXmiHelper.saveToString(kevModel,true))

}
