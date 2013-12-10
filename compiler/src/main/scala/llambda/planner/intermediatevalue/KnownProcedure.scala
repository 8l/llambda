package llambda.planner.intermediatevalue

import llambda.ProcedureSignature
import llambda.{celltype => ct}
import llambda.{valuetype => vt}
import llambda.planner.{step => ps}
import llambda.planner.{PlanWriter, InvokableProcedure}
import llambda.codegen.AdaptedProcedureSignature
import llambda.InternalCompilerErrorException

/** Represents a procedure with a known signature and direct entry point
  *
  * These procedures can be called directly without going through a costly 
  * trampoline. If this is converted to a ct.ProcedureCell a trampoline will 
  * be dynamically built to give it an adapated signature. Adapted signatures
  * are the same for all procedures so they can be called without specific 
  * knowledge of the backing procedure. These adapated procedure values are
  * represented by InvokableProcedureCell
  *
  * @param signature     Signature of the procedure
  * @param nativeSymbol  Native symbol of the direct entry point to the procedure
  * @param selfTempOpt   For procedures with closures a procedure cell
  *                      containing the procedure's closure. The entry point
  *                      does not have to be initialized; it will be set
  *                      dynamically to a generated trampofile if this value is
  *                      explicitly converted to a ct.ProcedureCell
  * @param reportName    Name of this procedure in R7RS. This is used as a tag 
  *                      to implemented certain optimizations elsewhere in the
  *                      planner. It is not directly used by this class
  */
class KnownProcedure(val signature : ProcedureSignature, nativeSymbol : String, selfTempOpt : Option[ps.TempValue], val reportName : Option[String] = None) extends IntermediateValue with InvokableProcedure with NonRecordValue {
  val possibleTypes = Set[ct.ConcreteCellType](ct.ProcedureCell) 
  
  def toInvokableProcedure()(implicit plan : PlanWriter) : Option[InvokableProcedure] = 
    Some(this)

  def toCellTempValue(targetType : ct.CellType)(implicit plan : PlanWriter) : Option[ps.TempValue] = {
    if (targetType.isTypeOrSupertypeOf(ct.ProcedureCell)) {
      val cellTemp = selfTempOpt getOrElse {
        // This must have an empty closure
        // If we had a closure selfTempOpt would have been defined to contain it
        // This means we have to create a new closureless procedure cell to
        // contain the entry point
        val tempAllocation = new ps.TempAllocation
        plan.steps += ps.AllocateCells(tempAllocation, 1)

        val cellTemp = new ps.TempValue
        val dataTemp = new ps.TempValue

        plan.steps += ps.RecordLikeInit(cellTemp, dataTemp, tempAllocation, 0, vt.EmptyClosureType)

        cellTemp
      }

      // At this point cellTemp doesn't have a valid entry point set regardless
      // of where it came from. Try to get an entry point with an adapated
      // signature
      val entryPointTemp = if (signature == AdaptedProcedureSignature) {
        // The procedure already has the correct signature
        // This is unlikely but worth checking
        val entryPointTemp = new ps.TempValue
        plan.steps += ps.StoreNamedEntryPoint(entryPointTemp, signature, nativeSymbol)

        entryPointTemp
      }
      else {
        // Give the trampoline a sufficently scary looking symbol
        val trampolineSymbol = "__llambda_" + nativeSymbol + "_trampoline"

        // Ensure this already hasn't been planned
        if (!plan.plannedFunctions.contains(trampolineSymbol)) {
          // Plan the trampoline
          plan.plannedFunctions += (trampolineSymbol -> PlanProcedureTrampoline(signature, nativeSymbol))
        }

        // Load the trampoline's entry point
        val trampEntryPointTemp = new ps.TempValue
        plan.steps += ps.StoreNamedEntryPoint(trampEntryPointTemp, AdaptedProcedureSignature, trampolineSymbol) 

        trampEntryPointTemp
      }
    
      // Store the entry point in the procedure cell
      plan.steps += ps.SetProcedureEntryPoint(cellTemp, entryPointTemp)

      if (targetType != ct.ProcedureCell) {
        // Cast this to super
        val castTemp = new ps.TempValue
        plan.steps += ps.CastCellToTypeUnchecked(castTemp, cellTemp, targetType)

        Some(castTemp)
      }
      else {
        Some(cellTemp)
      }
    }
    else {
      None
    }
  }
  
  def toNativeTempValue(nativeType : vt.NativeType)(implicit plan : PlanWriter) : Option[ps.TempValue] =
    // Procedures have no unboxed representation
    None

  def withReportName(newReportName : String) : KnownProcedure = {
    new KnownProcedure(signature, nativeSymbol, selfTempOpt, Some(newReportName))
  }
  
  def planEntryPoint()(implicit plan : PlanWriter) : ps.TempValue = {
    val entryPointTemp = new ps.TempValue
    plan.steps += ps.StoreNamedEntryPoint(entryPointTemp, signature, nativeSymbol)

    entryPointTemp
  }

  def planSelf()(implicit plan : PlanWriter) : ps.TempValue = selfTempOpt getOrElse {
    throw new InternalCompilerErrorException("Attempted to get self value of a procedure without a closure")
  }
  
  def preferredRepresentation : vt.ValueType =
    vt.IntrinsicCellType(ct.ProcedureCell)

  def closureRepresentation : Option[vt.ValueType] = 
    // We only need a closure if we have a closure ourselves (i.e. a self temp)
    selfTempOpt map { _ =>
      preferredRepresentation
    }
}

