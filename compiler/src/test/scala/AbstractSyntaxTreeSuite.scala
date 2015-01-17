package io.llambda.compiler
import io.llambda

import org.scalatest.FunSuite

class AbstractSyntaxTreeSuite  extends FunSuite {
  test("fold case of symbol") {
    assert(ast.Symbol("UPPER").toCaseFolded === ast.Symbol("upper"))
    assert(ast.Symbol("Mixed").toCaseFolded === ast.Symbol("mixed"))
    assert(ast.Symbol("LOWER").toCaseFolded === ast.Symbol("lower"))
  }

  test("fold case preserves source location") {
    val testLoc = SourceLocation(Some("test-file"), "HELLO", 0)

    val testSymbol = ast.Symbol("UPPER")
    testSymbol.locationOpt = Some(testLoc)

    val foldedSymbol = testSymbol.toCaseFolded

    assert(foldedSymbol.locationOpt === Some(testLoc))
  }

  test("fold case of symbols inside pair") {
    val testPair = ast.Pair(
      ast.Symbol("CAR"),
      ast.Symbol("Cdr")
    )

    val expectedPair = ast.Pair(
      ast.Symbol("car"),
      ast.Symbol("cdr")
    )

    assert(testPair.toCaseFolded === expectedPair)
  }

  test("fold case of symbols inside vector") {
    val testVector = ast.VectorLiteral(Vector(
      ast.Symbol("ONE"),
      ast.Symbol("Two"),
      ast.Symbol("three")
    ))

    val expectedVector = ast.VectorLiteral(Vector(
      ast.Symbol("one"),
      ast.Symbol("two"),
      ast.Symbol("three")
    ))

    assert(testVector.toCaseFolded === expectedVector)
  }
}