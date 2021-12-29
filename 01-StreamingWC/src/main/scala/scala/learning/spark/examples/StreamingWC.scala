package scala.learning.spark.examples

import java.util.logging.Logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object StreamingWC {
  @transient lazy val logger: Logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .master("local[3]")
      .appName("Streaming Word Count")
      .config("spark.streaming.stopGracefullyOnShutdown", "true")
      .config("spark.sql.shuffle.partitions", 3)
      .getOrCreate()

    val linesDF = spark.readStream
                    .format("socket")
                    .option("host", "localhost")
                    .option("port", "9999")
                    .load()

//    linesDF.printSchema()

    val wordsDF = linesDF.select(expr("explode(split(value,' ')) as word"))
    val countsDF = wordsDF.groupBy("word").count()

    val wordCountQuery = countsDF.writeStream
                          .format("console")
                          .outputMode("complete") // complete/update/insert
                          .option("checkpointLocation", "chk-point-dir")
                          .start()

    logger.info("Listening to localhost:9999")
    wordCountQuery.awaitTermination()
  }

}
