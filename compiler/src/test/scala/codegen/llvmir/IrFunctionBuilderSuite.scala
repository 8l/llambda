package llambda.codegen.llvmir

import org.scalatest.FunSuite
import IrFunction._

class IrFunctionBuilderSuite extends FunSuite {
  test("empty function def") {
    val result = IrFunction.Result(VoidType, Set())
   
    val function = new IrFunctionBuilder(
      result=result,
      name="donothing",
      namedArguments=Nil) {

      addBlock("entry")(new IrBlockBuilder {
        retVoid()
      })
    }

    assert(function.toIr ===
      "define void @donothing() {\n" +
      "entry1:\n" +
      "\tret void\n" +
      "}")
  }

  test("function returning arg def") {
    val result = Result(IntegerType(32), Set())
    
    val namedArguments = List("testArg" -> Argument(IntegerType(32)))
   
    val function = new IrFunctionBuilder(
      result=result,
      name="retArg",
      namedArguments=namedArguments) {

      addBlock("entry")(new IrBlockBuilder {
        ret(argumentValues("testArg"))
      })
    }

    assert(function.toIr ===
      "define i32 @retArg(i32 %testArg) {\n" +
      "entry1:\n" +
      "\tret i32 %testArg\n" +
      "}")
  }

  test("hello world def") {
    val helloWorldDef = IrGlobalVariableDef(
      name="helloWorldString",
      initializer=StringConstant("Hello, world!"),
      constant=true,
      unnamedAddr=true)
    
    val putsDecl = {
      IrFunctionDecl(
        result=IrFunction.Result(IntegerType(32), Set()),
        arguments=List(IrFunction.Argument(PointerType(IntegerType(8)), Set(NoCapture))),
        name="puts",
        attributes=Set(IrFunction.NoUnwind))
    }

    val result = IrFunction.Result(IntegerType(32), Set())
    
    val namedArguments = List(
      "argc" -> IrFunction.Argument(IntegerType(32)),
      "argv" -> IrFunction.Argument(PointerType(PointerType(IntegerType(8)))))

    val function = new IrFunctionBuilder(
      result=result,
      namedArguments=namedArguments,
      name="main") {
      
      addBlock("entry")(new IrBlockBuilder {
        val helloPointer = getelementptr("helloPtr")(
          resultType=PointerType(IntegerType(8)),
          basePointer=helloWorldDef.variable,
          indices=List(0, 0)
        )
          
        callDecl(None)(putsDecl, helloPointer :: Nil)
        ret(IntegerConstant(IntegerType(32), 0))
      })
    }

    assert(function.toIr === 
      """|define i32 @main(i32 %argc, i8** %argv) {
         |entry1:
         |	%helloPtr1 = getelementptr [14 x i8]* @helloWorldString, i32 0, i32 0
         |	call i32 @puts(i8* %helloPtr1) nounwind
         |	ret i32 0
         |}""".stripMargin)
  }
  
  test("multi block function def") {
    val result = IrFunction.Result(VoidType, Set())
   
    val function = new IrFunctionBuilder(
      result=result,
      name="donothing",
      namedArguments=Nil) {

      val continueLabel = declareBlock("continue")

      addBlock("entry")(new IrBlockBuilder {
        uncondBranch(continueLabel)
      })

      defineBlock(continueLabel)(new IrBlockBuilder {
        retVoid()
      })
    }

    assert(function.toIr ===
      "define void @donothing() {\n" +
      "entry1:\n" +
      "\tbr label %continue1\n" +
      "continue1:\n" +
      "\tret void\n" +
      "}")
  }

}