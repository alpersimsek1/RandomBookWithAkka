package Helper

object Util {

  def pickRandom(arr: Array[(String, String)]): (String, String) = {
    val random = scala.util.Random.nextInt(arr.length-1)

    arr(random)
  }
}
