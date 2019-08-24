package WebServer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import org.apache.spark.sql.SparkSession

import scala.io.StdIn
import Helper.Util._

import scala.concurrent.ExecutionContextExecutor

object Server {

    implicit val spark: SparkSession = SparkSession
      .builder()
      .appName("alayzxer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    def main(args: Array[String]) {

      implicit val system: ActorSystem = ActorSystem("my-system")
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext: ExecutionContextExecutor = system.dispatcher

      val arr = spark
        .read
        .option("header","true")
        .option("delimiter","|")
        .csv("book_barcodes.csv")
        .as[(String, String)]
        .collect()

      val route =
        path("randomBook") {
          get {
            val randomBook = pickRandom(arr)
            val name = randomBook._1
            val barcode = randomBook._2
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"$name  $barcode"))
          }
        } ~ path("love") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say Love-http</h1>"))
          }
        }

      val bindingFuture = Http().bindAndHandle(route, "10.10.51.204", 8080)

      println(s"Server online at http://10.10.51.204:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }

}
