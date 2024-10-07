package helpers

/**
 * Entity with name: String
 */

trait Name:
  val name: String

object Name:
  extension[N <: Name](sn: Seq[Name])
    def names: Seq[String] = sn.map(_.name)

