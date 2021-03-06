package TradeService

import Messages._
import akka.cluster._

import com.typesafe.config.ConfigFactory
import akka.cluster.ClusterEvent.MemberUp
import akka.actor.{ Actor, ActorRef, ActorSystem, Props, RootActorPath }

class TradeService extends Actor {
	println(self.path)
  val cluster = Cluster(context.system)

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive = {
    case Trade(ticker, task) =>
      println("I'm a trade node with trade" + ticker + " : " + task)
    
    case MemberUp(member) =>
      if(member.hasRole("Controller")){
        context.actorSelection(RootActorPath(member.address) / "user" / "Controller") !
          TradeServiceRegistration
      }
  }

}

object TradeService {
  def initiate(port: Int) = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.load().getConfig("TradeService"))

    val system = ActorSystem("ClusterSystem", config)

    val TradeService = system.actorOf(Props[TradeService], name = "TradeService")
  }
}


object TradeApp extends App{
	TradeService.initiate(2560)
	
	
}
