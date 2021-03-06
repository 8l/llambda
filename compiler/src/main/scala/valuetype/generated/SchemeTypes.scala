/************************************************************
 * This file is generated by typegen. Do not edit manually. *
 ************************************************************/

package io.llambda.compiler.valuetype
import io.llambda.compiler

import compiler.{celltype => ct}

object UnitType extends SchemeTypeAtom(ct.UnitCell)
object ListElementType extends UnionType(Set(SchemeTypeAtom(ct.PairCell), SchemeTypeAtom(ct.EmptyListCell)))
object EmptyListType extends SchemeTypeAtom(ct.EmptyListCell)
object StringType extends SchemeTypeAtom(ct.StringCell)
object SymbolType extends SchemeTypeAtom(ct.SymbolCell)
object BooleanType extends SchemeTypeAtom(ct.BooleanCell)
object NumberType extends UnionType(Set(SchemeTypeAtom(ct.ExactIntegerCell), SchemeTypeAtom(ct.FlonumCell)))
object ExactIntegerType extends SchemeTypeAtom(ct.ExactIntegerCell)
object FlonumType extends SchemeTypeAtom(ct.FlonumCell)
object CharType extends SchemeTypeAtom(ct.CharCell)
object VectorType extends SchemeTypeAtom(ct.VectorCell)
object BytevectorType extends SchemeTypeAtom(ct.BytevectorCell)
object ErrorObjectType extends SchemeTypeAtom(ct.ErrorObjectCell)
object PortType extends SchemeTypeAtom(ct.PortCell)
object EofObjectType extends SchemeTypeAtom(ct.EofObjectCell)
object MailboxType extends SchemeTypeAtom(ct.MailboxCell)

object IntrinsicSchemeTypes {
  def apply() : Map[String, SchemeType] = Map(
    (ct.UnitCell.schemeName -> UnitType),
    (ct.ListElementCell.schemeName -> ListElementType),
    (ct.EmptyListCell.schemeName -> EmptyListType),
    (ct.StringCell.schemeName -> StringType),
    (ct.SymbolCell.schemeName -> SymbolType),
    (ct.BooleanCell.schemeName -> BooleanType),
    (ct.NumberCell.schemeName -> NumberType),
    (ct.ExactIntegerCell.schemeName -> ExactIntegerType),
    (ct.FlonumCell.schemeName -> FlonumType),
    (ct.CharCell.schemeName -> CharType),
    (ct.VectorCell.schemeName -> VectorType),
    (ct.BytevectorCell.schemeName -> BytevectorType),
    (ct.ErrorObjectCell.schemeName -> ErrorObjectType),
    (ct.PortCell.schemeName -> PortType),
    (ct.EofObjectCell.schemeName -> EofObjectType),
    (ct.MailboxCell.schemeName -> MailboxType)
  )
}
