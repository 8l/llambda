package io.llambda.compiler.planner.intermediatevalue
import io.llambda

import llambda.compiler.{PolymorphicSignature, StorageLocation, ContextLocated}
import llambda.compiler.{valuetype => vt}
import llambda.compiler.planner.{step => ps}
import llambda.compiler.planner._
import llambda.compiler.et

/** Represents a user-provided procedure with a Scheme language definitio */
class KnownSchemeProc(
    polySignature : PolymorphicSignature,
    plannedSymbol : String,
    selfTempOpt : Option[ps.TempValue],
    val parentState : PlannerState,
    val closure : LambdaClosure,
    val lambdaExpr : et.Lambda,
    val recursiveSelfLoc : Option[StorageLocation],
    reportNameOpt : Option[String] = None)
extends KnownUserProc(polySignature, plannedSymbol, selfTempOpt, reportNameOpt) {
  // Override this to ensure we have vt.ProcedureType
  // This is required for KnownCaseLambdaProc to collect its type from its clauses
  override val schemeType : vt.ProcedureType = polySignature.toSchemeProcedureType

  override def locationOpt : Option[ContextLocated] =
    Some(lambdaExpr)

  override def withReportName(newReportName : String) : KnownSchemeProc = {
    new KnownSchemeProc(
      polySignature,
      plannedSymbol,
      selfTempOpt,
      parentState,
      closure,
      lambdaExpr,
      recursiveSelfLoc,
      Some(newReportName)
    )
  }

  override def withSelfTemp(selfTemp : ps.TempValue) : KnownSchemeProc =
    new KnownSchemeProc(
      polySignature,
      plannedSymbol,
      Some(selfTemp),
      parentState,
      closure,
      lambdaExpr,
      recursiveSelfLoc,
      reportNameOpt
    )
}

