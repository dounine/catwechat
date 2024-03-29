package test.com.dounine.douyinpay

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import com.dounine.catwechat.model.models.OrderModel
import com.dounine.catwechat.model.types.service.{PayPlatform, PayStatus}
import com.dounine.catwechat.service.OrderService
import com.dounine.catwechat.store.{EnumMappers, OrderTable}
import com.dounine.catwechat.tools.akka.db.DataSource
import com.dounine.catwechat.tools.json.JsonParse
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base64
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.crypto.{Cipher, KeyGenerator}
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.Await
import scala.concurrent.duration._

class Base64Test
    extends ScalaTestWithActorTestKit(
      ConfigFactory
        .parseString(s"""
           |akka.remote.artery.canonical.port = 25520
           |akka.persistence.journal.leveldb.dir = "/tmp/journal_${classOf[
          Base64Test
        ].getSimpleName}"
           |akka.persistence.snapshot-store.local.dir = "/tmp/snapshot_${classOf[
          Base64Test
        ].getSimpleName}"
           |""".stripMargin)
        .withFallback(
          ConfigFactory.parseResources("application-test.conf")
        )
        .resolve()
    )
    with Matchers
    with AnyWordSpecLike
    with LogCapturing
    with EnumMappers
    with MockitoSugar
    with JsonParse {
  val orderService = new OrderService()
  "base64 test" should {

    "aes" in {
//      val kg= KeyGenerator.getInstance("AES")
//      kg.init(128)
//      val secretKey=kg.generateKey()
//      val key=new SecretKeySpec(secretKey.getEncoded(),"AES")

      val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
      val plainTextBytes = """{"ai":"test-accountId","name":"用户姓名","idNum":"371321199012310912"}""".getBytes("UTF-8")
//      cipher.init(Cipher.ENCRYPT_MODE, "2836e95fcd10e04b0069bb1ee659955b")


    }

    "encode64 and decode64" ignore {
      val file = FileIO.fromPath(Paths.get("/tmp/wechat.png"))
      file
        .map(f => Base64.encodeBase64String(f.toList.toArray))
        .map(Base64.decodeBase64)
        .map(f => ByteString.fromArray(f))
        .runWith(FileIO.toPath(Paths.get("/tmp/wechat1.png")))
    }

    "sort time" ignore {
      val time = LocalDateTime.now()
      val list = Array(
        (1, time.plusDays(2)),
        (2, time.minusDays(1)),
        (3, time.plusSeconds(10)),
        (5, time.minusYears(1)),
        (4, time.plusDays(1))
      )
      info(list.minBy(_._2).toString())
    }

    "merge source" in {
      Source(1 to 10)
        .flatMapMerge(
          3,id => {
            if(id==2){
             Source.single(id)
               .delay(1.seconds)
            }else Source.single(id)
          }
        )
        .runForeach(id => {
          info(id.toString)
        })

      TimeUnit.SECONDS.sleep(2)
    }

    "stream" ignore {

      val cc = Source(1 to 3)
        .filter(_ > 3)
        .take(1)
        .runWith(Sink.head)

      info(cc.futureValue.toString)

    }
  }
}
