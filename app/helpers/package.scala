import scala.util.chaining.*

package object helpers {
  inline def scala2unapply[T <: Product](t: T)
                                        (using m: scala.deriving.Mirror.ProductOf[T]): Option[m.MirroredElemTypes] =
    Option(Tuple.fromProductTyped(t))

  // andThen для двуарной ф-ции
  extension [A1, A2, R](f: (A1, A2) => R)
    def andThen[RR](g: R => RR): (A1, A2) => RR =
      f.tupled.andThen(g) pipe Function.untupled

  extension [A, B](pair: (A, A => B))
    def swapApply: B = pair.swap.pipe(_ apply _)
  extension [A](a: A)
    def swapApply[B](f: A => B): B = f(a)


  // для универсальной обработки объектов где ключи простые или Option
  extension [T](k: Option[T] | T)
    inline def toOp: Option[T] =
      k match
        case o: Option[?] => o.asInstanceOf[Option[T]]
        case t => Option(t.asInstanceOf[T]) // None if null

  extension [T](t: T)
    def pipeOp(f: Option[T => T]): T =
      f.fold(t)(_.apply(t))
    def pipeIf(condition: Boolean)(f: T => T): T =
      pipeOp(Option.when(condition)(f))
    def pipeIf(condition: T => Boolean)(f: T => T): T =
      pipeIf(condition(t))(f)

    def tapPartial(f: PartialFunction[T, Unit]): T =
      t.tap(f.lift)

}
