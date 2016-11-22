package com.thaj.functionalprogramming.example.typeclass.functor

import simulacrum._
// F[_] is the type constructor, that given a type to F[_] it
// gives you a proper type value.

// Functor makes sense to only type constructors, and not to actual types

// Many problems can be solved by bringing in the concept of Functor that
// defines map in it. We can define many derived operations using this map
// as given in the below example.
// @ typeclass gives us a few operators (ops) through which function(a, b) can be called as a.function(b)
@typeclass trait Functor[F[_]]{ self =>
  def map[A, B](fa: F[A])(f: A => B): F[B]

  def lift[A, B](f: A => B): F[A] => F[B] = fa => map(fa)(f)

  def as[A, B](fa: F[A])(b: => B): F[B] = map(fa)(_ => b)

  def void[A](fa: F[A]): F[Unit] = as(fa)(())

  def compose[G[_]](implicit F: Functor[G]): Functor[Lambda[X => F[G[X]]]] = new Functor[Lambda[X => F[G[X]]]] {
    def map[A, B] (fga: F[G[A]])(f: A => B): F[G[B]] =
      self.map(fga)(ga => F.map(ga)(f))
  }
}

trait FunctorLaws {
  def identity[F[_], A](fa: F[A])(implicit F: Functor[F]) =
    F.map(fa)(a => a) == fa
  def composition[F[_], A, B, C](fa: F[A], f: A => B, g: B => C) (implicit F: Functor[F]) =
    F.map(F.map(fa)(f))(g) == F.map(fa) (f andThen g)
}


object Functor {
  type L[A] = Function1[Int, A]
  // Doesnt make much sense there is already map defined for the type List in Scala
  // This is just for pedagogical purpose
  implicit val listFunctor: Functor[List] = new Functor[List] {
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
  }

  // We will be reusing the map of Option
  implicit val optionFunctor: Functor[Option] = new Functor[Option] {
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
  }

  // X => ? is a type constructor for a function... something which you want as Function1[A, ?]
  // Lambda[SomeVariableType => (X => SomeVariableType)] ==== X => ?
  implicit def function1Functor[X]: Functor[Lambda[SomeVariableType => (X => SomeVariableType)]] =
    new Functor[X => ?] {
    def map[A, B](fa: X => A)(g: A => B): X => B = fa andThen g
  }
}