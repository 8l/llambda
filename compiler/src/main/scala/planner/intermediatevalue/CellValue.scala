package io.llambda.compiler.planner.intermediatevalue
import io.llambda

import llambda.compiler.{celltype => ct}
import llambda.compiler.{valuetype => vt}
import llambda.compiler.planner.{step => ps}
import llambda.compiler.planner.typecheck
import llambda.compiler.planner.{PlanWriter, InvokableProcedure, BoxedValue}
import llambda.compiler.InternalCompilerErrorException
import llambda.compiler.RuntimeErrorMessage

class CellValue(val schemeType : vt.SchemeType, val boxedValue : BoxedValue) extends IntermediateValue {
  lazy val typeDescription = s"cell of type ${schemeType}"

  override def toTruthyPredicate()(implicit plan : PlanWriter) : ps.TempValue = {
    // Find out if we're false
    val isFalseResult = typecheck.PlanTypeCheck(boxedValue, schemeType, vt.ConstantBooleanType(false))
    val isFalsePred = isFalseResult.toNativePred()

    // Invert the result
    val constantZeroPred = ps.Temp(vt.Predicate)
    plan.steps += ps.CreateNativeInteger(constantZeroPred, 0, vt.Predicate.bits) 

    val truthyPred = ps.Temp(vt.Predicate)
    plan.steps += ps.IntegerCompare(truthyPred, ps.CompareCond.Equal, None, isFalsePred, constantZeroPred)
    truthyPred
  }
  
  def toBoxedValue()(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : BoxedValue =
    boxedValue
  
  def toInvokableProcedure()(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : Option[InvokableProcedure] =  {
    if (vt.SatisfiesType(vt.ProcedureType, schemeType) == Some(false)) {
      None
    }
    else {
      // Cast to a procedure
      val boxedProcTemp = toTempValue(vt.ProcedureType)

      Some(new InvokableProcedureCell(boxedProcTemp))
    }
  }

  def toNativeTempValue(nativeType : vt.NativeType, errorMessageOpt : Option[RuntimeErrorMessage])(implicit plan : PlanWriter, worldPtr : ps.WorldPtrValue) : ps.TempValue = nativeType match {
    case vt.UnicodeChar =>
      val boxedChar = toTempValue(vt.CharType)
      val unboxedTemp = ps.Temp(vt.UnicodeChar)
      plan.steps += ps.UnboxChar(unboxedTemp, boxedChar)

      unboxedTemp
      
    case intType : vt.IntType =>
      val boxedExactInt = toTempValue(vt.ExactIntegerType)
      val unboxedTemp = ps.Temp(vt.Int64)
      plan.steps += ps.UnboxExactInteger(unboxedTemp, boxedExactInt)

      if (intType.bits == 64) {
        // Correct width
        unboxedTemp
      }
      else {
        val convTemp = ps.Temp(intType)

        // Convert to the right width
        plan.steps += ps.ConvertNativeInteger(convTemp, unboxedTemp, intType.bits, intType.signed) 
        convTemp
      }

    case fpType : vt.FpType =>
      if (vt.SatisfiesType(vt.FlonumType, schemeType) == Some(false)) {
        // Not possible
        impossibleConversion(s"Unable to convert non-flonum ${typeDescription} to ${fpType.schemeName}")
      }

      // Unbox as flonum
      val boxedFlonum = toTempValue(vt.FlonumType)
      val unboxedTemp = ps.Temp(vt.Double)
      plan.steps += ps.UnboxFlonum(unboxedTemp, boxedFlonum)

      if (fpType == vt.Double) {
        // No conversion needed
        unboxedTemp
      }
      else {
        val convTemp = ps.Temp(fpType)

        plan.steps += ps.ConvertNativeFloat(convTemp, unboxedTemp, fpType)
        convTemp
      }

    case vt.Predicate =>
      throw new InternalCompilerErrorException("Attempt to directly convert to native boolean. Should be caught by toTruthyPredicate.")
  }
  
  override def withSchemeType(newType : vt.SchemeType) : IntermediateValue = {
    new CellValue(newType, boxedValue)
  }
  
  def preferredRepresentation : vt.ValueType =
    schemeType
  
  def needsClosureRepresentation =
    true
}

