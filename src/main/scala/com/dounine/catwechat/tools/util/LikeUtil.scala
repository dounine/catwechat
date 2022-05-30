package com.dounine.catwechat.tools.util

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object LikeUtil {

  def module(vec: Vector[Double]) = {
    math.sqrt(vec.map(math.pow(_, 2)).sum)
  }
  def innerProduct(v1: Vector[Double], v2: Vector[Double]) = {
    val listBuffer = ListBuffer[Double]()
    for (i <- v1.indices; j <- 0 to v2.length; if i == j) {
      if (i == j) listBuffer.append(v1(i) * v2(j))
    }
    listBuffer.sum
  }

  def cosvec(v1: Vector[Double], v2: Vector[Double]) = {
    val cos = innerProduct(v1, v2) / (module(v1) * module(v2))
    if (cos <= 1) cos else 1.0
  }

  def textCosine(str1: String, str2: String) = {
    val set = mutable.Set[Char]()
    // 不进行分词
    str1.foreach(set += _)
    str2.foreach(set += _)
    val ints1: Vector[Double] = set.toList.sorted
      .map(ch => {
        str1.count(s => s == ch).toDouble
      })
      .toVector
    val ints2: Vector[Double] = set.toList.sorted
      .map(ch => {
        str2.count(s => s == ch).toDouble
      })
      .toVector
    cosvec(ints1, ints2)
  }

//  def main(args: Array[String]): Unit = {
//    println(LikeUtil.textCosine("那种猫粮猫咪吃了，有没有其他影响","有没有猫粮推荐"))
//  }

}
