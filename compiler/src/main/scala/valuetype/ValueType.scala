package io.llambda.compiler.valuetype
import io.llambda

import llambda.compiler.{celltype => ct}

/** Type of any value known to the compiler
  * 
  * These are more specific than the types defined in R7RS in that they define both the semantic type and the runtime
  * representation. For example, Int32 and vt.ExactIntegerType are both exact integers but have different native
  * representations.
  */
sealed abstract class ValueType {
  val schemeName : String
  val schemeType : SchemeType
  val isGcManaged : Boolean

  override def toString = schemeName
}

/** Type represented by a native pointer */
sealed abstract trait PointerType extends ValueType

/** Pointer to a garbage collected value cell */
sealed abstract trait CellValueType extends PointerType {
  val cellType : ct.CellType
}

/** Primitive types shared with C */
sealed abstract class NativeType extends ValueType {
  val isGcManaged = false
}

/** Type reperesented by a native integer */
sealed abstract class IntLikeType(val bits : Int, val signed : Boolean) extends NativeType

/** Native integer type representing a Scheme boolean */
sealed abstract class BoolLikeType(bits : Int) extends IntLikeType(bits, false) {
  val schemeType = BooleanType
}

/** LLVM single bit predicates */
case object Predicate extends BoolLikeType(1) {
  val schemeName = "<internal-predicate>"
}

/** C99/C++ style single byte boolean */
case object CBool extends BoolLikeType(8) {
  val schemeName = "<bool>"
}

/** Native integer type representing a Scheme exact integer */
sealed abstract class IntType(bits : Int, signed : Boolean) extends IntLikeType(bits, signed) {
  val schemeName = if (signed) {
    s"<int${bits}>"
  }
  else {
    s"<uint${bits}>"
  }

  val schemeType = ExactIntegerType
}

case object Int8 extends IntType(8, true)
case object Int16 extends IntType(16, true)
case object Int32 extends IntType(32, true)
case object Int64 extends IntType(64, true)

case object UInt8 extends IntType(8, false)
case object UInt16 extends IntType(16, false)
case object UInt32 extends IntType(32, false)
// UInt64 is outside the range we can represent

/** Native floating point type representing a Scheme inexact rational */
sealed abstract class FpType extends NativeType {
  val schemeType = InexactRationalType
}

case object Float extends FpType {
  val schemeName = "<float>"
}

case object Double extends FpType {
  val schemeName = "<double>"
}

/** Native integer representing a Unicode code point */
case object UnicodeChar extends IntLikeType(32, true) {
  val schemeName = "<unicode-char>"
  val schemeType = CharacterType
}

/** Identifies a record field
  *
  * This is not a case class because a field with the same source name and type can be distinct if it's declared in
  * another record type. It's even possible for one type to have fields with the same source name if they come from
  * different scopes.
  */
class RecordField(val sourceName : String, val fieldType : ValueType)

/** Pointer to a garabge collected value cell containing a record-like type */
sealed abstract class RecordLikeType extends CellValueType {
  val sourceName : String
  val fields : List[RecordField]
  val cellType : ct.ConcreteCellType with ct.RecordLikeFields
  val isGcManaged = true
}

/** Pointer to a closure type
  *
  * Closure types store the data needed for a procedure from its parent lexical scope. The storage is internally
  * implemented identically to user-defined record types.
  */
class ClosureType(val sourceName : String, val fields : List[RecordField]) extends RecordLikeType {
  val cellType = ct.ProcedureCell
  val schemeName = "<internal-closure-type>"
  val schemeType = ProcedureType
}

/** Types visible to Scheme programs without using the NFI */ 
sealed abstract trait SchemeType extends CellValueType {
  val schemeType = this
  
  /** Subtracts another type from this one
    *
    * This is typically used after a type test to build a new possible Scheme type for a tested value
    */
  def -(otherType : SchemeType) : SchemeType

  /** Creates a union of this type with another */
  def +(otherType : SchemeType) : SchemeType = {
    SchemeType.fromTypeUnion(List(this, otherType))
  }
  
  /** Intersects this type with another */
  def &(otherType : SchemeType) : SchemeType

  /** Determines if we satisfy another Scheme type
    *
    * Some(true) indicates all values of this type satisfies the passed type. Some(false) indicates no values of this
    * type satisfy the passed type. None indicates that some values satisify the other type.
    */
  def satisfiesType(otherType : SchemeType) : Option[Boolean]
}


/** All Scheme types except unions
  *
  * This is to enforce that unions of unions have their member types flattened in to a single union.
  */
sealed abstract trait NonUnionSchemeType extends SchemeType {
  // Non-union types always have a concrete cell type
  val cellType : ct.ConcreteCellType

  def -(otherType : SchemeType) : SchemeType = 
    if (satisfiesType(otherType) == Some(true)) {
      UnionType(Set())
    }
    else {
      this
    }

  def &(otherType : SchemeType) : SchemeType = {
    // Find the most specific type
    if (this.satisfiesType(otherType) == Some(true)) {
      this
    }
    else if (otherType.satisfiesType(this) == Some(true)) {
      otherType
    }
    else {
      // No intersection
      UnionType(Set())
    }
  }
  
  def satisfiesNonUnionType(otherType : NonUnionSchemeType) : Option[Boolean]

  def satisfiesType(otherType : SchemeType) : Option[Boolean] = otherType match {
    case unionType : UnionType =>
      // This can be universally handled for all types
      val recursiveResult = unionType.memberTypes.map(this.satisfiesType(_))

      if (recursiveResult.contains(Some(true))) {
        // We satisfy at least one member type
        Some(true)
      }
      else if (recursiveResult == Set(Some(false))) {
        // We satisfy no member types
        Some(false)
      }
      else {
        None
      }

    case nonUnion : NonUnionSchemeType =>
      satisfiesNonUnionType(nonUnion)
  }

}

/** Utility type for Scheme types derived from other Scheme types */
sealed abstract trait DerivedSchemeType extends NonUnionSchemeType {
  val parentType : NonUnionSchemeType

  protected def derivedSatisfiesType(otherType : SchemeType) : Option[Boolean] 

  def satisfiesNonUnionType(otherType : NonUnionSchemeType) = 
    // Check our parent types first
    parentType.satisfiesNonUnionType(otherType) match {
      case Some(parentResult) =>
        Some(parentResult)
      case _ =>
        derivedSatisfiesType(otherType)
    }
}

/** Pointer to a garbage collected value cell containing an intrinsic type */
case class SchemeTypeAtom(cellType : ct.ConcreteCellType) extends NonUnionSchemeType {
  val schemeName = cellType.schemeName

  val isGcManaged = cellType match {
    case preconstruct : ct.PreconstructedCellType =>
      // Only constant instances of this exist
      false

    case _ =>
      true
  }
  
  def satisfiesNonUnionType(otherType : NonUnionSchemeType) = otherType match {
    case typeAtom : SchemeTypeAtom =>
      // We definitely satisfy ourselves
      Some(typeAtom == this)

    case derivedType : DerivedSchemeType =>
      if (satisfiesType(derivedType.parentType) != Some(false)) {
        // We may satisfy a super type
        None
      }
      else {
        Some(false)
      }
  }
}

/** Pointer to a garabge collected value cell containing a user-defined record type
  * 
  * This uniquely identifies a record type even if has the same name and internal structure as another type 
  */
class RecordType(val sourceName : String, val fields : List[RecordField]) extends RecordLikeType with DerivedSchemeType {
  val cellType = ct.RecordCell
  val schemeName = sourceName
  val parentType = SchemeTypeAtom(ct.RecordCell)

  def derivedSatisfiesType(otherType : SchemeType) = otherType match {
    case recordType : RecordType =>
      // We satisfy ourselves
      Some(recordType eq this)

    case _ =>
      None
  }
}

/** Union of multiple Scheme types */
case class UnionType(memberTypes : Set[NonUnionSchemeType]) extends SchemeType {
  lazy val isGcManaged = memberTypes.exists(_.isGcManaged)
  
  private def cellTypesBySpecificity(rootType : ct.CellType) : List[ct.CellType] = {
    rootType.directSubtypes.toList.flatMap(cellTypesBySpecificity) :+ rootType
  }

  /** Most specific cell type that is a superset all of our member types */
  lazy val cellType : ct.CellType = {
    val possibleCellTypes = memberTypes.map(_.cellType) : Set[ct.ConcreteCellType]

    // Find the most specific cell type that will cover all of our member types
    (cellTypesBySpecificity(ct.DatumCell).find { candidateCellType =>
      possibleCellTypes.subsetOf(candidateCellType.concreteTypes)
    }).get
  }

  /** Cell type exactly matching our member types or None if no exact match exists */
  private def exactCellTypeOpt : Option[ct.CellType] = {
    (cellTypesBySpecificity(ct.DatumCell).find { candidateCellType =>
      SchemeType.fromCellType(candidateCellType) == this 
    })
  }

  lazy val schemeName = exactCellTypeOpt match {
    case Some(exactCellType) =>
      exactCellType.schemeName 

    case _ =>
      memberTypes.toList.map(_.schemeName).sorted match {
        case singleTypeName :: Nil =>
          singleTypeName

        case multipleTypeNames =>
          "(U" + multipleTypeNames.map(" " + _).mkString("")  + ")"
      }
  }
  
  def satisfiesType(otherType : SchemeType) : Option[Boolean] = {
    val recursiveResult = memberTypes.map(_.satisfiesType(otherType))

    if (recursiveResult == Set(Some(true))) {
      // All member types satisfy this type
      Some(true)
    }
    else if (recursiveResult == Set(Some(false))) {
      // No member types satisfy this type
      Some(false)
    }
    else {
      None
    }
  }
  
  def -(otherType : SchemeType) : SchemeType = {
    val remainingMembers = memberTypes.filter(_.satisfiesType(otherType) != Some(true))

    SchemeType.fromTypeUnion(remainingMembers.toList)
  }

  def &(otherType: SchemeType) : SchemeType = {
    val intersectedMembers = memberTypes.map(_.&(otherType))
    SchemeType.fromTypeUnion(intersectedMembers.toList)
  }
}

/** Union of all possible Scheme types */
object AnySchemeType extends UnionType(ct.DatumCell.concreteTypes.map(SchemeTypeAtom(_)))

object SchemeType {
  def fromCellType(cellType : ct.CellType) : SchemeType = {
    cellType match {
      case concrete : ct.ConcreteCellType =>
        SchemeTypeAtom(concrete)

      case _ =>
        // Non-concrete cell types are a way of sharing unions types between C++ and Scheme
        // Break them down to union types on our side
        UnionType(cellType.concreteTypes.map(SchemeTypeAtom(_)))
    }
  }

  def fromTypeUnion(otherTypes : List[SchemeType]) : SchemeType = {
    val nonUnionTypes = otherTypes.flatMap {
      case nonUnion : NonUnionSchemeType =>
        Set(nonUnion)

      case union : UnionType =>
        union.memberTypes
    }

    nonUnionTypes.distinct match {
      case singleType :: Nil =>
        singleType

      case _ =>
        UnionType(nonUnionTypes.toSet)
    }
  }
}
