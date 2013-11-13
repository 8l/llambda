package llambda.optimize

import llambda.et

object FlattenSelfExecutingLambdas {
  def apply(expr : et.Expression) : et.Expression = expr match {
    // Can't handle rest args for now - probably not worth it
    case et.Apply(et.Lambda(fixedArgs, None, body), operands) if fixedArgs.length == operands.length =>
      // Assign our operands to new storage locations
      // This is required to keep the semantics of the lambda receiving copies
      // of its arguments
      val bindingsExprs = fixedArgs.zip(operands) map { case (arg, operand) =>
        et.Bind(List(arg -> operand))
      }

      // Replace the whole lambda with a (begin)
      et.Begin(bindingsExprs :+ body.map(apply))

    case _ =>
      expr.map(apply)
  }
}