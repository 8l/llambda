package io.llambda.compiler.frontend
import io.llambda

import org.scalatest.{FunSuite,Inside,OptionValues}

import llambda.compiler._
import llambda.compiler.{valuetype => vt}
import llambda.compiler.valuetype.Implicits._

class ExtractCaseLambdaSuite extends FunSuite with Inside with testutil.ExprHelpers {
  implicit val primitiveScope = new ImmutableScope(collection.mutable.Map(Primitives.bindings.toSeq : _*))
  val nfiScope = new ImmutableScope(testutil.NfiExports(), Some(primitiveScope))

  test("(case-lambda) with no clauses") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    val caseExpr = exprFor("""(case-lambda)""")(scope)
    assert(caseExpr === et.CaseLambda(Nil))
  }

  test("untyped (case-lambda) with fixed args") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    inside(exprFor(
      """(case-lambda
           (() #t)
           ((one) #f))"""
    )(scope)) {
      case caseExpr @ et.CaseLambda(List(firstLambda, secondLambda)) =>
        val firstProcType = vt.ProcedureType(
          fixedArgTypes=Nil,
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val firstProcTypePoly = firstProcType.toPolymorphic

        val secondProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.AnySchemeType),
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val secondProcTypePoly = secondProcType.toPolymorphic

        assert(caseExpr.schemeType === vt.CaseProcedureType(List(firstProcType, secondProcType)))

        inside(firstLambda) {
          case et.Lambda(`firstProcTypePoly`, Nil, None, et.Literal(ast.BooleanLiteral(true)), _) =>
        }

        inside(secondLambda) {
          case et.Lambda(`secondProcTypePoly`, List(_), None, et.Literal(ast.BooleanLiteral(false)), _) =>
        }
    }
  }

  test("typed (case-lambda) with fixed args") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    inside(exprFor(
      """(case-lambda
           (() #t)
           (([one : <exact-integer>]) #f))"""
    )(scope)) {
      case caseExpr @ et.CaseLambda(List(firstLambda, secondLambda)) =>
        val firstProcType = vt.ProcedureType(
          fixedArgTypes=Nil,
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val firstProcTypePoly = firstProcType.toPolymorphic

        val secondProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.ExactIntegerType),
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val secondProcTypePoly = secondProcType.toPolymorphic

        assert(caseExpr.schemeType === vt.CaseProcedureType(List(firstProcType, secondProcType)))

        inside(firstLambda) {
          case et.Lambda(`firstProcTypePoly`, Nil, None, et.Literal(ast.BooleanLiteral(true)), _) =>
        }

        inside(secondLambda) {
          case et.Lambda(`secondProcTypePoly`, List(_), None, et.Literal(ast.BooleanLiteral(false)), _) =>
        }
    }
  }

  test("untyped (case-lambda) with rest argument") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    inside(exprFor(
      """(case-lambda
           ((one) #t)
           ((first . rest) #f))"""
    )(scope)) {
      case caseExpr @ et.CaseLambda(List(firstLambda, secondLambda)) =>
        val firstProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.AnySchemeType),
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val firstProcTypePoly = firstProcType.toPolymorphic

        val secondProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.AnySchemeType),
          restArgMemberTypeOpt=Some(vt.AnySchemeType),
          returnType=vt.ReturnType.ArbitraryValues
        )
        val secondProcTypePoly = secondProcType.toPolymorphic

        assert(caseExpr.schemeType === vt.CaseProcedureType(List(firstProcType, secondProcType)))

        inside(firstLambda) {
          case et.Lambda(`firstProcTypePoly`, List(_), None, et.Literal(ast.BooleanLiteral(true)), _) =>
        }

        inside(secondLambda) {
          case et.Lambda(`secondProcTypePoly`, List(_), Some(_), et.Literal(ast.BooleanLiteral(false)), _) =>
        }
    }
  }

  test("untyped (case-lambda) with rest argument clause with no fixed args") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    inside(exprFor(
      """(case-lambda
           ((one) #t)
           (rest #f))"""
    )(scope)) {
      case caseExpr @ et.CaseLambda(List(firstLambda, secondLambda)) =>
        val firstProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.AnySchemeType),
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val firstProcTypePoly = firstProcType.toPolymorphic

        val secondProcType = vt.ProcedureType(
          fixedArgTypes=List(),
          restArgMemberTypeOpt=Some(vt.AnySchemeType),
          returnType=vt.ReturnType.ArbitraryValues
        )
        val secondProcTypePoly = secondProcType.toPolymorphic

        assert(caseExpr.schemeType === vt.CaseProcedureType(List(firstProcType, secondProcType)))

        inside(firstLambda) {
          case et.Lambda(`firstProcTypePoly`, List(_), None, et.Literal(ast.BooleanLiteral(true)), _) =>
        }

        inside(secondLambda) {
          case et.Lambda(`secondProcTypePoly`, Nil, Some(_), et.Literal(ast.BooleanLiteral(false)), _) =>
        }
    }
  }

  test("typed (case-lambda) with rest argument") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    inside(exprFor(
      """(case-lambda
           (((one : <symbol>)) #t)
           (([first : <string>] rest : <port> *) #f))"""
    )(scope)) {
      case caseExpr @ et.CaseLambda(List(firstLambda, secondLambda)) =>
        val firstProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.SymbolType),
          restArgMemberTypeOpt=None,
          returnType=vt.ReturnType.ArbitraryValues
        )
        val firstProcTypePoly = firstProcType.toPolymorphic

        val secondProcType = vt.ProcedureType(
          fixedArgTypes=List(vt.StringType),
          restArgMemberTypeOpt=Some(vt.PortType),
          returnType=vt.ReturnType.ArbitraryValues
        )
        val secondProcTypePoly = secondProcType.toPolymorphic

        assert(caseExpr.schemeType === vt.CaseProcedureType(List(firstProcType, secondProcType)))

        inside(firstLambda) {
          case et.Lambda(`firstProcTypePoly`, List(_), None, et.Literal(ast.BooleanLiteral(true)), _) =>
        }

        inside(secondLambda) {
          case et.Lambda(`secondProcTypePoly`, List(_), Some(_), et.Literal(ast.BooleanLiteral(false)), _) =>
        }
    }
  }

  test("(case-lambda) clause with same arity fails") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    intercept[BadSpecialFormException] {
      exprFor(
        """(case-lambda
             ((one) #t)
             ((one) #f))"""
      )(scope)
    }
  }

  test("(case-lambda) clause with after rest arg fails") {
    val scope = new Scope(collection.mutable.Map(), Some(nfiScope))

    intercept[BadSpecialFormException] {
      exprFor(
        """(case-lambda
             ((one . rest) #t)
             ((one two) #f))"""
      )(scope)
    }
  }
}
