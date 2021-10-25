package service.database.converters

import doobie.{Read, Write}
import shapeless.Generic

object DoobieGenericConverters {

  class To[A] {
    def genericRead[B](implicit gen: Generic.Aux[A, B], t: Read[B]): Read[A] =
      Read.generic[A, B]
  }

  object To { def apply[A] = new To[A] }

  class From[A] {
    def genericWrite[B](implicit gen: Generic.Aux[A, B], t: Write[B]): Write[A] =
      Write.generic[A, B]
  }

  object From { def apply[A] = new From[A] }

}
