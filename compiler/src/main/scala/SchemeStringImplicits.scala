package io.llambda.compiler
import io.llambda

object SchemeStringImplicits {
  import StringContext._

  class MultipleDatumError extends Exception("Multiple datums found")

  implicit class SchemeStringHelper(val sc : StringContext) extends AnyVal {
    def scm(args : Any*) : List[ast.Datum] = 
      SchemeParser.parseStringAsData(sc.raw(args : _*))

    def datum(args : Any*) : ast.Datum = scm(args : _*) match {
      case singleDatum :: Nil => singleDatum
      case _ => throw new MultipleDatumError
    }
  }
}
