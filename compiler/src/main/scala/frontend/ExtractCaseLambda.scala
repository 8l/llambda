package io.llambda.compiler.frontend
import io.llambda

import llambda.compiler._
import llambda.compiler.{valuetype => vt}

import llambda.compiler.valuetype.Implicits._

object ExtractCaseLambda {
  def apply(
      located : SourceLocated, 
      typed : Boolean,
      clauseData : List[sst.ScopedDatum]
  )(debugContext : debug.SourceContext, libraryLoader : LibraryLoader, frontendConfig : FrontendConfig) : et.CaseLambda = {
    val locatedClauses = clauseData map {
      case clauseDatum @ sst.ScopedProperList(sst.ScopedListOrDatum(fixedArgData, restArgDatum) :: definition) =>
        val lambdaExpr = ExtractLambda(
          located=clauseDatum,
          typed=typed,
          operandList=fixedArgData,
          operandTerminator=restArgDatum,
          definition=definition
        )(debugContext, libraryLoader, frontendConfig).assignLocationFrom(clauseDatum)

        (clauseDatum, lambdaExpr)

      case otherDatum =>
        throw new BadSpecialFormException(otherDatum, "Invalid (case-lambda) clause")
    }

    val locatedSignatures = locatedClauses map { case (located, lambdaExpr) =>
      (located, lambdaExpr.schemeType)
    }

    ValidateCaseLambdaClauses(locatedSignatures)
    et.CaseLambda(locatedClauses.map(_._2))
  }
}
