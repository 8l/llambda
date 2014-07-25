package io.llambda.compiler.valuetype
import io.llambda

import llambda.compiler.{celltype => ct}

object SatisfiesType {
  /** Determines if a Scheme type satisfies another
    *
    * Some(true) indicates all values of test testing type satisfy the super type. Some(false) indicates no values of
    * the testing type satisfy the super type.  None indicates that some values of the testing type satisfy the super
    * type.
    */
  def apply(superType : SchemeType, testingType : SchemeType) : Option[Boolean] = {
    if (superType eq AnySchemeType) {
      // Quick optimisation - this does not affect correctness
      return Some(true)
    }

    (superType, testingType) match {
      case (_, UnionType(testingMemberTypes)) =>
        val memberResult = testingMemberTypes.map(apply(superType, _)) 

        if (memberResult == Set(Some(true))) {
          // All member types satisfy this type
          Some(true)
        }
        else if (memberResult == Set(Some(false))) {
          // No member types satisfy this type
          Some(false)
        }
        else {
          None
        }

      case (UnionType(memberTypes), _) =>
        val memberResult = memberTypes.map(apply(_, testingType))

        if (memberResult.contains(Some(true))) {
          // We satisfy at least one member type
          Some(true)
        }
        else if (memberResult == Set(Some(false))) {
          // We satisfy no member types
          Some(false)
        }
        else {
          None
        }

      case (superAtom : SchemeTypeAtom, testingAtom : SchemeTypeAtom) =>
        // Atoms are easy
        Some(superAtom == testingAtom)

      case (superValue : ConstantValueType, testingValue : ConstantValueType) =>
        Some(superValue == testingValue)

      case (superRecord : RecordType, testingRecord : RecordType) =>
        // Record types satisfy themselves
        Some(superRecord eq testingRecord)
      
      case (superPair : PairType, testingPair : PairType) =>
        // Pairs satisfy their more general pairs
        for(carSatisfies <- SatisfiesType(superPair.carType, testingPair.carType);
            cdrSatisfies <- SatisfiesType(superPair.cdrType, testingPair.cdrType))
        yield
          (carSatisfies && cdrSatisfies)

      case (superList @ ProperListType(superMember), _) =>
        testingType match {
          case ProperListType(testingMember) =>
            SatisfiesType(superMember, testingMember)

          case testingPair : PairType =>
            for(carSatisfies <- SatisfiesType(superMember, testingPair.carType);
                cdrSatisfies <- SatisfiesType(superList, testingPair.cdrType))
            yield
              (carSatisfies && cdrSatisfies)

          case EmptyListType =>
            // Empty list satisfies all proper lists
            Some(true)

          case _ =>
            Some(false)
        }

      case (_, testingList @ ProperListType(testingMember)) =>
        superType match {
          case superPair : PairType => 
            if ((SatisfiesType(superPair.carType, testingMember) == Some(false)) ||
                (SatisfiesType(superPair.cdrType, testingList) == Some(false))) {
              // Definitely don't satisfy
              Some(false)
            }

            // We may satisfy this pair or we may be an empty list
            None

          case EmptyListType =>
            // Proper lists may satisfy a proper list
            None
          
          case _ =>
            Some(false)
        }

      case (superDerived : DerivedSchemeType, _) =>
        // Didn't have an exact match - check our parent types
        apply(superDerived.parentType, testingType) match {
          case Some(false) =>
            // Definitely doesn't match
            Some(false)

          case _ =>
            // May match a super type
            None
        }
      
      case (_, testingDerived : DerivedSchemeType) =>
        apply(superType, testingDerived.parentType)
    }
  }
}
