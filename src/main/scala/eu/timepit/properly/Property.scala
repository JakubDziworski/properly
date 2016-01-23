package eu.timepit.properly

import scalaz.{ Free, Monad }
import scalaz.concurrent.Task
import scalaz.effect.IO

object Property {

  // constructors

  def clear(key: String): Property[Unit] =
    Free.liftFC(PropertyOp.Clear(key))

  def get(key: String): Property[Option[String]] =
    Free.liftFC(PropertyOp.Get(key))

  def set(key: String, value: String): Property[Unit] =
    Free.liftFC(PropertyOp.Set(key, value))

  // derived operations

  def getOrElse(key: String, defaultValue: String): Property[String] =
    get(key).map(_.getOrElse(defaultValue))

  def getOrSet(key: String, value: String): Property[String] =
    get(key).flatMap {
      case Some(v) => Free.point(v)
      case None => set(key, value).map(_ => value)
    }

  // syntax

  implicit class PropertySyntax[A](val self: Property[A]) extends AnyVal {
    def runIO: IO[A] =
      Free.runFC(self)(PropertyOp.propertyOpToIO)

    def runTask: Task[A] =
      Task.delay(runIO.unsafePerformIO())

    def runMock(props: Map[String, String]): (Map[String, String], A) = {
      val state = Free.runFC(self)(PropertyOp.propertyOpToMockState)
      state(props)
    }
  }

  // instances

  implicit val propertyMonad: Monad[Property] =
    Free.freeMonad[PropertyOp.FreeFunctor]
}
