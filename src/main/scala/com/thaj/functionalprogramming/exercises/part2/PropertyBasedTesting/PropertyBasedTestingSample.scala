package com.thaj.functionalprogramming.example.exercises.part2

import com.thaj.functionalprogramming.example.exercises.PureStatefulAPI.RNG
import com.thaj.functionalprogramming.example.exercises.PureStatefulAPIGeneric.State

/**
 *  Although a library for testing has a very different purpose than a library for parallel computations,
 *  we’ll discover that these libraries
 *  have a lot of surprisingly similar combinators. This similarity is something we’ll return to in part 3.
 */
object PropertyBasedTestingSample {
  /*
    {{{
       // A generator of list of integers between 0 and 100
        val intList: Gen[List[Int]] = Gen.listOf(Gen.choose(0, 100))
        val prop = forAll(intList)(ns => ns.reverse.reverse == ns) &&
         forAll(intList)(ns => ns.headOption == ns.reverse.lastOption)
        val failingProp = forAll(intList)(ns = ns.reverse == ns
      )

      > prop.check
      > failingProp.check
    }}}

    The function forAll creates a property by combining a generator of type Gen[A] with some
    predicate of type A => Boolean. The property asserts that all values produced by the generator
    should satisfy the predicate. Like generators, properties can also have a rich API.
    In this simple example we’ve used && to combine two properties.
    The resulting property will hold only if neither property can be falsified by any of the generated test cases.
    Together, the two properties form a partial specification of the correct behavior of the reverse method.[1]



    Exercise 8.1
    To get used to thinking about testing in this way, come up with properties that specify the implementation
    of a sum: List[Int] => Int function. You don’t have to write your properties down as
    executable ScalaCheck code—an informal description is fine. Here are some ideas to get you started:

    -> List of all zeroes => zero
    -> reverse the list and sum => sum of list
    -> list of same number => number * size of list
    -> list of all 1s => size of list
    -> list of n numbers and negative of all numbers => zero


    Exercise 8.2
    What properties specify a function that finds the maximum of a List[Int]?
    max of list of zeros => zero
    max of reverse of a list => max of list
    max of list with Int.MaxValue => Int.MaxValue
   */

  /** A few nice properties of property based checking
    * API Generators and Properties
    *
    * Test Case minimisation: if a property fails, it tries to find out the smallest list of values, for which
    * the property was failing. This illuminates debugging.
    *
    * Exhaustive test case generation: If your domain Gen[A] is well defined, it runs test for all the values
    * in the domain and test, instead of taking a sample of values and run the test that can result in proving
    * a theorem by the absence of an evidence to the contrary.
    *
    * ScalaCheck is one property-based testing library
    * val intList = Gen.listOf(Gen.choose(0,100))
    * val prop = forAll(intList)(ns => ns.reverse.reverse == ns) &&
    * forAll(intList )(ns => ns.headOption == ns.reverse.lastOption)
    */

  // Initial API set up

  def listOf[A](gen:Gen[A]): Gen[List[A]] = ???

  // bringing in size constrains

  /**
   * This would certainly be a useful combinator, but not having to explicitly specify sizes is powerful as well.
   * It means that whatever function runs the tests has the freedom to choose test case sizes, which opens up the
   * possibility of doing the test case minimization we mentioned earlier. If the sizes are always fixed and specified by
   * the programmer, the test runner won’t
   * have this flexibility. Keep this concern in mind as we get further along in our design.
   */
  def listOfN[A](n: Int, gen: Gen[A]): Gen[List[A]] = ???

  def forAll[A](a: Gen[A])(f: A => Boolean): Prop = ???

}


trait LegacyProp {
  def check: Boolean
  /**
   * Exercise 8.3
   */
  def &&(p: LegacyProp): LegacyProp = new LegacyProp {
    def check = this.check && p.check
  }
}


// Design refinement
trait PropRefinedV1 {
  import PropRefinedV1._
  def check: Either[(FailedCase, SuccessCount), SuccessCount]
}
object PropRefinedV1 {
  type FailedCase = String
  type SuccessCount = Int
}


// Consider gen as something dealing with random number generator, that in turn deals with state transitions.
// Lets wrap State transition with respect to RNG in Gen case class

// Gen[RNG, Gen[A](State (rng => (genA, rng2))) : Gen[Gen[A]]
case class Gen[A](sample: State[RNG, A]) {
  def map [B](a: A => B): Gen[B] = Gen(this.sample.map(a))
  // Exercise 8.6
  def flatMap[B](a: A => Gen[B]): Gen[B] = Gen.join(this.map(a))
  def listOfN(size: Gen[Int]): Gen[List[A]] = size.flatMap(n => Gen.sequence(List.fill(n)(this)))
  def map2[B, C](a: Gen[B])(f: (A, B) => C): Gen[C] = this.flatMap( t => a.map(aa => f(t, aa)))

}



object Gen {
  //Exercise 8.4
  //Implement Gen.choose using this representation of Gen.
  // It should generate integers in the range start to stopExclusive. Feel free to use functions you’ve already written.

  import State._

  def choose(start: Int, stopExclusive: Int): Gen[Int] =
    Gen(nonNegativeInt.map(n => start + n % (stopExclusive-start)))

  // Exercise 8.5
  def unit[A](a: A): Gen[A] = Gen(State.unit(a))
  def boolean: Gen[Boolean] = Gen(State.boolean)
  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] = Gen(State.sequence(List.fill(n)(g.sample)))

  /**
   * As we discussed in chapter 7, we’re interested in understanding what operations are
   * primitive and what operations are derived, and in finding a small yet expressive set of primitives.
   * A good way to explore what is expressible with a given set of primitives is to pick some
   * concrete examples you’d like to express, and see if you can assemble the functionality you want.
   * As you do so, look for patterns, try factoring out these patterns into combinators, and refine your set of primitives.
   * We encourage you to stop reading here and simply play with the primitives and combinators we’ve written so far.
   * If you want some concrete examples to inspire you, here are some ideas:
   * If we can generate a single Int in some range, do we need a new primitive to generate an (Int,Int) pair in some range?
   * Can we produce a Gen[Option[A]] from a Gen[A]? What about a Gen[A] from a Gen[Option[A]]?
   * Can we generate strings somehow using our existing primitives?
   *
   * // Refer to map, flatMap, join etc
   */

  def join[B](a: Gen[Gen[B]]): Gen[B] = {
    Gen(State(rng => {
      val k: (Gen[B], RNG) = a.sample.run(rng)
      val y: (B, RNG) = k._1.sample.run(k._2)
      y
    }))
  }

  def sequence[A](a: List[Gen[A]] ): Gen[List[A]] =
    a.foldRight(unit(Nil: List[A]))((c, d) => c.map2(d)(_ :: _))


  // Sample ones
  def converToGenOptionA[A](a: Gen[A]): Gen[Option[A]] = a.map(Option(_))

  //Tuple in some range
  def GenForTuple(start: Int, end: Int): Gen[(Int, Int)] = Gen.choose(start, end).map2(Gen.choose(start, end))((_, _))

  //generate string - May be wrong, but just for pedagogical purpose
  def generateString = Gen.choose(1, 100).map(_.toString)

  // Suppose we’d like a Gen[(String,String)] that generates pairs where the second string
  // contains only characters from the first. Or that we had a Gen[Int] that chooses an integer
  // between 0 and 11, and we’d like to make a Gen[List[Double]] that then generates
  // lists of whatever length is chosen. In both of these cases there’s a dependency—we

  def getIntegerBetween0and11(n: Int): Gen[List[Double]] = sequence(List.fill(n)(Gen.choose(0, 11).map(_.toDouble)))

  // This might be wrong
  def getStringPairWithSecondStringBeingSubStringOfFirst: Gen[(String, String)] = {
    Gen.choose(0, 100).map(_.toString).map2(Gen.choose(0, 100).map(_.toString)) { (a, b) =>
      if(a.contains(b))(a, b) else (a, a)
    }
  }

  //Exercise 8.7
  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] =
    boolean.flatMap(b => if (b) g1 else g2)

  // Exercise 8.8
  // Implement weighted, a version of union that accepts a weight for each
  // Gen and generates values from each Gen with probability proportional to its weight.
  // Referred the answer here
  def weighted[A](g1: (Gen[A],Double), g2: (Gen[A],Double)): Gen[A] = {
    val g1Threshold = g1._2.abs / (g1._2.abs + g2._2.abs)

    Gen(State.double).flatMap(d => if (d < g1Threshold) g1._1 else g2._1)
  }
}


import Prop._
case class Prop(run: TestCases => Result)

// Prop Refined - Final Version
object Prop {
  type FailedCase = String
  type SuccessCount = Int
  type TestCases = Int

  /**
   *
   * type Result = Either[(FailedCase, SuccessCount), SuccessCount]
   * case class Prop(run: TestCases => Result)
   * Also, we’re recording the number of successful tests on both sides of that Either.
   * But when a property passes, it’s implied that the number of passed tests will be equal to the argument to run.
   * So the caller of run learns nothing new by being told the success count.
   * Since we don’t currently need any information in the Right case of that Either, we can turn it into an Option:
   * type Result = Option[(FailedCase, SuccessCount)]
   * case class Prop(run: TestCases => Result)
   * This seems a little weird, since None will mean that all tests succeeded
   * and the property passed and Some will indicate a failure. Until now, we’ve only
   * used the None case of Option to indicate failure. But in this case we’re using it
   * to represent the absence of a failure.
   * That’s a perfectly legitimate use for Option, but its intent isn’t very clear.
   * So let’s make a new data type, equivalent to Option[(FailedCase, SuccessCount)], that shows our intent very clearly.
   */
  sealed trait Result {
    def isFalsified: Boolean
  }

  case object Passed extends Result {
    def isFalsified: Boolean = false
  }

  case class Falsified(failedCase: FailedCase, successCount: SuccessCount) {
    def isFalsisfied: Boolean = true
  }

  import com.thaj.functionalprogramming.example.exercises.Stream
  // Basically generates a stream of A by intuitively passing the changed state after each computation of stream.
  def randomStream[A](g: Gen[A])(rng: RNG): Stream[A] = {
    Stream.unfold(rng)(rng => Some(g.sample.run(rng)))
  }
}





