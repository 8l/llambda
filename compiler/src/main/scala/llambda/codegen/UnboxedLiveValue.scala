package llambda.codegen

import llambda.nfi
import llambda.codegen.{boxedtype => bt}
import llambda.codegen.llvmir._

abstract class UnboxedLiveValue(boxedType : bt.ConcreteBoxedType, nativeType : nfi.NativeType, unboxedValue : IrValue) extends LiveValue {
  val possibleTypes = Set(boxedType)

  // Should only be overriden by LiveBoolean
  def genTruthyPredicate(state : GenerationState) : IrValue =
    IntegerConstant(IntegerType(1), 1)

  def genBoxedValue(state : GenerationState) : (GenerationState, IrValue)

  def genCastBoxedValue(initialState : GenerationState)(targetType : bt.BoxedType) : (GenerationState, IrValue) = {
    val (state, uncastIrValue) = genBoxedValue(initialState)
    val castValueName = "castTo" + targetType.name

    val block = state.currentBlock
    (state, block.bitcastTo(castValueName)(uncastIrValue, PointerType(targetType.irType)))
  }

  // This should be good for most subclasses except for numerics which support
  // implicit conversions
  def genUnboxedValue(state : GenerationState)(targetType : nfi.UnboxedType) : Option[IrValue] = {
    if (targetType != nativeType) {
      None
    }
    else {
      Some(unboxedValue)
    }
  }
  
  def toNativeType(state : GenerationState)(targetType : nfi.NativeType) : Option[(GenerationState, IrValue)] = {
    targetType match {
      case nfi.BoxedValue(expectedType) =>
        if (!boxedType.isTypeOrSubtypeOf(expectedType)) {
          // Not possible
          None
        }
        else {
          Some(genCastBoxedValue(state)(expectedType))
        }
      
      case nfi.CTruthyBool if nativeType == nfi.CStrictBool =>
        // We can return this directy without truncating and extending it
        Some((state, unboxedValue))

      case nfi.CTruthyBool =>
        val block = state.currentBlock

        val truthyPred = genTruthyPredicate(state)
        val truthyBool = block.zextTo("truthyBool")(truthyPred, IntegerType(nfi.CStrictBool.bits))

        Some((state, truthyBool))

      case unboxedType : nfi.UnboxedType =>
        genUnboxedValue(state)(unboxedType).map((state, _))
    }
  }
}
