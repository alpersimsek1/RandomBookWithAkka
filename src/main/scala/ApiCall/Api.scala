package ApiCall

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.apache.spark.sql.{SaveMode, SparkSession}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

import scala.concurrent.duration._
import spray.json._

object Api {

  implicit val spark: SparkSession = SparkSession
    .builder()
    .appName("ApiCall")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  case class Product(bar: String, id: String, name: String,
                     brand: String, proid: String, `type`: String,
                     url: String)

  def main(args: Array[String]): Unit = {

    val unwantedChars = List("İ","Ş","Ç","Ö","Ü","."," ")
    val path = args(0)
    val arr = {
      spark
        .read
        .option("header", "true")
        .option("delimiter", "|")
        .csv(path)
        .as[(String, String)]
        .filter{
          xx =>
            !unwantedChars.exists{
              char => xx._2.contains(char)
            }
        }
    }

    arr.drop()

    arr.mapPartitions {
      partition =>
        implicit val system: ActorSystem = ActorSystem()
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        var proMap = new ListBuffer[Product]()
        partition.toList.foreach {
          data =>
            val responseFuture: Future[HttpResponse] = Http()
              .singleRequest(HttpRequest(uri = s"somecApi=${data._2}"))

            Await.result(responseFuture, 20 seconds)

            responseFuture
              .onComplete {
                case Success(res) => {

                  val bs: Future[ByteString] = res.entity.toStrict(5 seconds).map {
                    _.data
                  }
                  val s: Future[String] = bs.map(_.utf8String)
                  s.onComplete {
                    case Success(s) => {
                      val json = s.parseJson
                      json.asJsObject.getFields("data").foreach {
                        a =>
                          val js = a.asJsObject()
                          val productCount = js.getFields("someField").head.toString
                          if (productCount == "1") {
                            val pros = js.getFields("anotherfield").head.toString
                            val sub = pros.substring(1, pros.length - 1).parseJson.asJsObject()
                            val desiredFields = Seq("name", "is", "type", "secondname", "url")
                            val references = sub.getFields(desiredFields: _*).toList.map(_.toString)
                            val pro = Product(data._2, data._1,
                              references(3), references(0),
                              references(1), references(2),
                              references(4))
                            proMap += pro
                            println(pro)
                          }
                      }
                    }
                  }
                }
                case Failure(_) => proMap += Product("", "", "", "", "", "", "")
              }
        }
        proMap.toList.toIterator
    }.repartition(1)
      .write
      .mode(SaveMode.Overwrite)
      .option("header","true")
      .option("delimiter","\u2561")
      .csv("matched")

  }
}
