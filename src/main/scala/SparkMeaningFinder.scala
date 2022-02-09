import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import java.net._
import java.io._
import scala.collection.mutable.ArrayBuffer
import play.api.libs.json._

object SparkMeaningFinder {
  def meaningFinder(str:String):Option[String]={
    try {
      val url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + str)
      val in = new BufferedReader(new InputStreamReader(url.openStream))
      val buffer = new ArrayBuffer[String]()
      var inputLine = in.readLine
      while (inputLine != null) {
        if (!inputLine.trim.equals("")) {
          buffer += inputLine.trim
        }
        inputLine = in.readLine
      }
      in.close()
      try {
        val res = Json.parse(buffer(0).stripPrefix("[").stripSuffix("]").trim)
         Some((((res \ "meanings").get(0) \ "definitions").get(0) \ "definition").get.toString().stripPrefix("\"").stripSuffix("\"").trim)
      }
      catch {
        case e:Exception => {
          val res = Json.parse(buffer(0).stripPrefix("[").stripSuffix("]").trim.split(",\\{\"word\"")(0))
          Some((((res \ "meanings").get(0) \ "definitions").get(0) \ "definition").get.toString().stripPrefix("\"").stripSuffix("\"").trim)
        }
      }
    }
    catch {
      case e:FileNotFoundException => None
    }
  }
  case class WordMeaning(word:String,meaning:String)
  def main(args:Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[*]").setAppName("FirstSparkDemo")
    val sc = new SparkContext(conf)
    val rdd = sc.textFile("Words.txt", 4)
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._
    val df = rdd.map(word => WordMeaning(word, meaningFinder(word).getOrElse(""))).toDF()
    df.write.csv("output")
  }
}
