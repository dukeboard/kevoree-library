package org.kevoree.library.voldemortChannels

import voldemort.cluster.Node
import voldemort.client.{ClientConfig, SocketStoreClientFactory, StoreClient, StoreClientFactory}
import voldemort.cluster.failuredetector.FailureDetectorListener

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 18/04/12
 * Time: 11:13
 */

class KClient(currentNode :Node,nodes: java.util.List[Node])   {
  val loadbalance : Boolean = false
  val flb = new RoundRobinLoadBalancerFactory()
  var currentFactory: StoreClientFactory = null

  var FailureDetectorListener = new FailureDetectorListener{
    def nodeUnavailable(p1: Node) {
      println("nodeUnavailable "+p1)
      currentFactory = null
    }

    def nodeAvailable(p1: Node) {
      println("nodeAvailable "+p1)

    }
  }

  def lb()
  {
    if(currentFactory == null){
      import scala.collection.JavaConversions._
      val lb : LoadBalancer =   flb.newLoadBalancer(nodes.toSet)
      val node =    lb.nextNode.get
      node match  {
        case classOf: Node => {
          try
          {
            currentFactory = new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls("tcp://" + node.getHost + ":" + node.getSocketPort))
            currentFactory.getFailureDetector.addFailureDetectorListener(FailureDetectorListener)
          } catch
            {
              case e : Exception => {
                currentFactory.close()
                currentFactory = null
              }
            }
        }
        case _ =>    {
          currentFactory = null
        }
      }
    }
  }

  def local()
  {
    if(currentFactory == null)
    {
      try
      {
        currentFactory = new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls("tcp://" + currentNode.getHost + ":" + currentNode.getSocketPort))
        currentFactory.getFailureDetector.addFailureDetectorListener(FailureDetectorListener)
      } catch
        {
          case e : Exception => {
            currentFactory.close()
            currentFactory = null
          }
        }

    }

  }

  def getStore(storeName : String) : StoreClient[_, _] = {
    do
    {
      if(loadbalance){
        lb()
      } else
      {
        local()
      }

    } while(currentFactory == null)
    currentFactory.getStoreClient(storeName)
  }





}