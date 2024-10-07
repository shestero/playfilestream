package helpers

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import scala.language.implicitConversions

package object streamsugar:

  val parallel: Int = 4

  extension [Out, M](source: Source[Option[Out], M])
    def flattenOp: Source[Out, M] =
      source.collect(identity[Option[Out]].unlift)

  extension [Out, M](source: Source[Out, M])
    def flatMapOp[Out2](f: Out => Option[Out2]): Source[Out2, M] =
      source.map(f).flattenOp // or: collect(identity[Option[P]].unlift)

    def flatMap[Out2](f: Out => Source[Out2, M]): Source[Out2, M] =
      source.flatMapMerge(parallel, f)

    def withFilter(p: Out => Boolean): Source[Out, M] =
      source.filter(p)

    def dropLast: Source[Out, M] =
      source.sliding(2).collect { case Seq(first, _) => first }

    def groupMapReduce[K, V](maxSubstreams: Int)(k: Out => K)(f: Out => V)(op: (V, V) => V): Source[(K, V), M] =
      source
        .groupBy(maxSubstreams, k)
        .map: r =>
          k(r) -> f(r)
        .reduce:
          case ((k1, v1), (k2, v2)) =>
            assert(k1 == k2)
            k1 -> op(v1, v2)
        .mergeSubstreams

  extension [Out, M](source: Source[Source[Out, M], M])
    def flatten: Source[Out, M] =
      source.flatMapConcat(identity)

  extension [Out](op: Option[Out])
    implicit def toSource: Source[Out, NotUsed] = Source(op.toSeq)

