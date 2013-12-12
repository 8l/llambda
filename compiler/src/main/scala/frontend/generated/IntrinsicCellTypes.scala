/*****************************************************************
 * This file is generated by gen-types.py. Do not edit manually. *
 *****************************************************************/

package io.llambda.compiler.frontend
import io.llambda

import llambda.compiler.{celltype => ct}

object IntrinsicCellTypes {
  def apply() : Map[String, ct.CellType] = Map(
    ("<datum-cell>" -> ct.DatumCell),
    ("<unspecific-cell>" -> ct.UnspecificCell),
    ("<list-element-cell>" -> ct.ListElementCell),
    ("<pair-cell>" -> ct.PairCell),
    ("<empty-list-cell>" -> ct.EmptyListCell),
    ("<string-cell>" -> ct.StringCell),
    ("<symbol-cell>" -> ct.SymbolCell),
    ("<boolean-cell>" -> ct.BooleanCell),
    ("<numeric-cell>" -> ct.NumericCell),
    ("<exact-integer-cell>" -> ct.ExactIntegerCell),
    ("<inexact-rational-cell>" -> ct.InexactRationalCell),
    ("<character-cell>" -> ct.CharacterCell),
    ("<vector-cell>" -> ct.VectorCell),
    ("<bytevector-cell>" -> ct.BytevectorCell),
    ("<procedure-cell>" -> ct.ProcedureCell)
  )
}