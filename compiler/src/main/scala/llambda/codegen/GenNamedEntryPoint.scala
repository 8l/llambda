package llambda.codegen

import llambda.ProcedureSignature
import llambda.planner.{step => ps}
import llambda.codegen.llvmir._

object GenNamedEntryPoint {
  def apply(module : IrModuleBuilder)(signature : ProcedureSignature, nativeSymbol : String, plannedSymbols : Set[String]) : IrValue = {
    // Declare the function
    val irSignature = ProcedureSignatureToIr(signature)

    val irFunctionDecl = IrFunctionDecl(
      result=irSignature.result,
      name=nativeSymbol,
      arguments=irSignature.arguments,
      attributes=irSignature.attributes,
      callingConv=irSignature.callingConv)

    // Don't declare this if we're about to generate it
    if (!plannedSymbols.contains(nativeSymbol)) {
      module.unlessDeclared(irFunctionDecl) {
        module.declareFunction(irFunctionDecl)
      }
    }

    irFunctionDecl.irValue
  }
}
