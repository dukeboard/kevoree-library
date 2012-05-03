package org.kevoree.library.voldemortChannels

import actors.DaemonActor
import voldemort.server.{VoldemortServer, VoldemortConfig}
import voldemort.cluster.{Node, Cluster}


/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 18/04/12
 * Time: 09:59
 */

class KServer(config: VoldemortConfig, nameCluster: String, nodes: java.util.List[Node]) extends  DaemonActor {

  private var cluster: Cluster = null
  private var server: VoldemortServer = null

  def act() {
    cluster = new Cluster(nameCluster,nodes)
    server = new VoldemortServer(config, cluster)
    server.start()
  }


  def stop(){
    if(server != null){
      try {
        server.stop()
      }  catch {
        case _ => // ignore
      }
    }

  }

}