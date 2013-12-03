package llambda.codegen

import llambda.{valuetype => vt}
import llambda.{celltype => ct}

case class SignedFirstClassType(
  irType : llvmir.FirstClassType,
  signed : Option[Boolean])

object ValueTypeToIr {
  def apply(valueType : vt.ValueType) : SignedFirstClassType = valueType match {
    case intLike : vt.IntLikeType =>
      SignedFirstClassType(llvmir.IntegerType(intLike.bits), Some(intLike.signed))

    case vt.Float =>
      SignedFirstClassType(llvmir.FloatType, None)

    case vt.Double =>
      SignedFirstClassType(llvmir.DoubleType, None)

    case vt.Utf8CString =>
      SignedFirstClassType(llvmir.PointerType(llvmir.IntegerType(8)), None)

    case vt.IntrinsicCellType(cellType) =>
      SignedFirstClassType(llvmir.PointerType(cellType.irType), None)

    case _ : vt.RecordCellType =>
      // All record cells have the same IR type. Their data is cast to the 
      // correct type on demand
      apply(vt.IntrinsicCellType(ct.RecordCell))
  }
}
