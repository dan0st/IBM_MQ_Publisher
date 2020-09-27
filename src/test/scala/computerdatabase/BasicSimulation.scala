package computerdatabase


import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.jms.JmsConstants
import com.ibm.msg.client.wmq.common.CommonConstants
import io.gatling.core.scenario.Simulation
import io.gatling.jms.Predef._
import io.gatling.core.Predef._


import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  val ff = JmsFactoryFactory.getInstance(JmsConstants.WMQ_PROVIDER)
  val cf = ff.createConnectionFactory()

  cf.setStringProperty(CommonConstants.WMQ_HOST_NAME, "localhost")
  cf.setIntProperty(CommonConstants.WMQ_PORT, 1414)
  cf.setStringProperty(CommonConstants.WMQ_CHANNEL, "SYSTEM.DEF.SVRCONN");
  cf.setIntProperty(CommonConstants.WMQ_CONNECTION_MODE, CommonConstants.WMQ_CM_CLIENT)
  cf.setStringProperty(CommonConstants.WMQ_QUEUE_MANAGER, "TestIBM")
  cf.setStringProperty(CommonConstants.WMQ_APPLICATIONNAME, "JmsPutGet (JMS)")

//  cf.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, true)
//              cf.setStringProperty(JmsConstants.USERID, "");
//              cf.setStringProperty(JmsConstants.PASSWORD, " ")
//    cf.setStringProperty(CommonConstants.WMQ_CHANNEL, "SYSTEM.DEF.SVRCONN");


  val jmsProtocolWithJndiConnectionFactory = jms
    .connectionFactory(cf)
    .listenerThreadCount(1)


// JNDI коннект

//    val jmsProtocolWithJndiConnectionFactory = jms
//      .connectionFactory(
//        jmsJndiConnectionFactory
//          .connectionFactoryName("TEST.MQ")
//          .url("file:/C:/Temp/binding/") //path to directory containing .bindingsfile
//          .contextFactory("com.sun.jndi.fscontext.RefFSContextFactory")
//      )


  val filesFeeder = csv("D:\\files.csv").queue.circular

  val scn1 =  scenario("JMS DSL test").feed(filesFeeder)
    .repeat(1)(
    exec(session => session.set("Disc", "D"))
    .exec { session =>
        println("Print Log Login - " + session ("Disc").as[String])
    session
  })
    .during(30 seconds) (
      pace(10 seconds)
        .exec(jms("req reply testing ${testFeder}")
          .send
          .queue("TestJMS")
          .objectMessage(ElFileBody("${Disc}:\\${testFeder}"))
        ).exec { session =>
          println("main")
          session
        }
    )
    .repeat(1){
      exec{ session =>
        println("logout " )
        session
      }

      }


  val scn2 =  scenario("JMS DSL test 2").feed(filesFeeder)
    .exec(session => session.set("Disc", "D"))
    .exec { session =>
      println("Print Log - " + session ("Disc").as[String])
      session
    }
    .exec( jms( "req reply testing")
//    .send
        .requestReply
      .queue("TestJMS")
//      .queue("Response")
      .replyQueue("Response")

      .textMessage(ElFileBody("${Disc}:\\${testFeder}")
      )
        .property("Time",System.currentTimeMillis())



    )

    setUp(
//      scn1.inject(atOnceUsers(1)).protocols(jmsProtocolWithJndiConnectionFactory),
      scn2.inject(constantUsersPerSec(0.2) during(1 minute)).protocols(jmsProtocolWithJndiConnectionFactory)

    )

}


