package io.llambda.compiler.planner.reportproc
import io.llambda

import llambda.compiler.{celltype => ct}
import llambda.compiler.{valuetype => vt}
import llambda.compiler.ContextLocated
import llambda.compiler.planner.{step => ps}
import llambda.compiler.planner.{intermediatevalue => iv}
import llambda.compiler.planner._

object EquivalenceProcPlanner extends ReportProcPlanner {
  private def allSubtypes(rootType : ct.CellType) : Set[ct.CellType] =
    rootType.directSubtypes ++ rootType.directSubtypes.flatMap(allSubtypes)

  private lazy val preconstructedTypes =
    allSubtypes(ct.DatumCell).collect {
      case precons : ct.PreconstructedCellType =>
        vt.SchemeTypeAtom(precons)
    } : Set[vt.NonUnionSchemeType]

  // These can be tested for (equals?) with a simple pointer compare
  private lazy val ptrCompareEqualsTypes = (preconstructedTypes ++ Set(
    vt.ErrorObjectType,
    vt.PortType
  )) : Set[vt.NonUnionSchemeType]

  // These can be tested for (eqv?) with a simple pointer compare
  private lazy val ptrCompareEqvTypes = (ptrCompareEqualsTypes ++ Set(
    vt.PairType,
    vt.VectorType,
    vt.BytevectorType
  )) : Set[vt.NonUnionSchemeType]

  private def directCompareAsType(state : PlannerState)(valueType : vt.ValueType, val1 : iv.IntermediateValue, val2 : iv.IntermediateValue)(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : Option[PlanResult] = {
    val val1Temp = val1.toTempValue(valueType)
    val val2Temp = val2.toTempValue(valueType)

    val predicateTemp = ps.Temp(vt.Predicate)

    // Do a direct integer compare
    plan.steps += ps.IntegerCompare(predicateTemp, ps.CompareCond.Equal, None, val1Temp, val2Temp)

    Some(PlanResult(
      state=state,
      value=new iv.NativePredicateValue(predicateTemp)
    ))
  }
  
  private def planEquivalenceProc(state : PlannerState)(ptrCompareTypes : Set[vt.NonUnionSchemeType], val1 : iv.IntermediateValue, val2 : iv.IntermediateValue)(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : Option[PlanResult] = {
    val ptrCompareUnionType = vt.UnionType(ptrCompareTypes)

    if (val1.schemeType.satisfiesType(val2.schemeType) == Some(false)) {
      // Types are completely disjoint - they can't be equivalent
      Some(PlanResult(
        state=state,
        value=new iv.ConstantBooleanValue(false)
      ))
    }
    else if ((val1.schemeType.satisfiesType(ptrCompareUnionType) == Some(true)) ||
             (val2.schemeType.satisfiesType(ptrCompareUnionType) == Some(true))) {
      // We can fast path this?
      // If the pssible types for either value consists entirely of fast path types
      directCompareAsType(state)(vt.AnySchemeType, val1, val2)

    }
    else if (val1.hasDefiniteType(vt.ExactIntegerType) && 
             val2.hasDefiniteType(vt.ExactIntegerType)) {
      directCompareAsType(state)(vt.Int64, val1, val2)
    }
    else if (val1.hasDefiniteType(vt.CharacterType) && 
             val2.hasDefiniteType(vt.CharacterType)) {
      directCompareAsType(state)(vt.UnicodeChar, val1, val2)
    }
    else {
      None
    }
  }

  def apply(state : PlannerState)(reportName : String, operands : List[(ContextLocated, iv.IntermediateValue)])(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : Option[PlanResult] = (reportName, operands) match {
    case (_, List((_, val1), (_, val2))) if List("eqv?", "eq?").contains(reportName) =>
      planEquivalenceProc(state)(ptrCompareEqvTypes, val1, val2)
    
    case ("equals?", List((_, val1), (_, val2))) =>
      planEquivalenceProc(state)(ptrCompareEqualsTypes, val1, val2)

    case _ =>
      None
  }
}
