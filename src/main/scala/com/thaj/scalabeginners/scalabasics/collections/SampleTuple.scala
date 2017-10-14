package com.scalabasics.collections
/**
 * basic concept of tuple in scala
 */
object SampleTuple {
  def main(args: Array[String]) {
    val t = (4, 3, 2, 1)
    val sum = t._1 + t._2 + t._3 + t._4
    println("Sum of Elements in this tuple is " + sum)
  }

}