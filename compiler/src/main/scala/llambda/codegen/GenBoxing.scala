package llambda.codegen

import llambda.InternalCompilerErrorException

import llambda.planner.{step => ps}
import llambda.codegen.llvmir._
import llambda.{boxedtype => bt}

object GenBoxing {
  private val llibyStringFromUtf8Decl = IrFunctionDecl(
    result=IrFunction.Result(PointerType(bt.BoxedString.irType)),
    name="_lliby_string_from_utf8",
    arguments=List(IrFunction.Argument(PointerType(IntegerType(8)))),
    attributes=Set(IrFunction.NoUnwind)
  )

  def apply(state : GenerationState)(boxStep : ps.BoxValue, unboxedValue : IrValue) : IrValue = boxStep match {
    case _ : ps.BoxBoolean =>
      state.currentBlock.select("boxedBool")(unboxedValue, GlobalDefines.trueIrValue, GlobalDefines.falseIrValue)

    case ps.BoxExactInteger(_, allocTemp, allocIndex, _) =>
      val block = state.currentBlock
      val allocation = state.liveAllocations(allocTemp)

      val boxedIntCons = allocation.genTypedPointer(block)(0, bt.BoxedExactInteger) 
      bt.BoxedExactInteger.genStoreToValue(block)(unboxedValue, boxedIntCons)

      boxedIntCons
    
    case ps.BoxInexactRational(_, allocTemp, allocIndex, _) =>
      val block = state.currentBlock
      val allocation = state.liveAllocations(allocTemp)

      val boxedRationalCons = allocation.genTypedPointer(block)(0, bt.BoxedInexactRational) 
      bt.BoxedInexactRational.genStoreToValue(block)(unboxedValue, boxedRationalCons)

      boxedRationalCons

    case ps.BoxCharacter(_, allocTemp, allocIndex, _) =>
      val block = state.currentBlock
      val allocation = state.liveAllocations(allocTemp)

      val boxedCharCons = allocation.genTypedPointer(block)(0, bt.BoxedCharacter) 
      bt.BoxedCharacter.genStoreToUnicodeChar(block)(unboxedValue, boxedCharCons)

      boxedCharCons

    case _ : ps.BoxUtf8String =>
      val block = state.currentBlock

      state.module.unlessDeclared(llibyStringFromUtf8Decl) {
        state.module.declareFunction(llibyStringFromUtf8Decl)
      }

      block.callDecl(Some("boxedString"))(llibyStringFromUtf8Decl, List(unboxedValue)).get
  }
}

