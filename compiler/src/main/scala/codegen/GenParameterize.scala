package io.llambda.compiler.codegen
import io.llambda

import llambda.compiler.planner.{step => ps}
import llambda.compiler.{celltype => ct}
import llambda.llvmir._
import llambda.llvmir.IrFunction._

object GenParameterize {
  private val llibyDynamicenvPush = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_push",
    arguments=List(
      Argument(PointerType(WorldValue.irType))
    )
  )

  private val llibyDynamicenvSetValue = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_set_value",
    arguments=List(
      Argument(PointerType(WorldValue.irType)),
      Argument(PointerType(ct.ProcedureCell.irType)),
      Argument(PointerType(ct.DatumCell.irType))
    ),
    attributes=Set(NoUnwind)
  )
  
private val llibyDynamicenvPop = IrFunctionDecl(
    result=Result(VoidType),
    name="_lliby_dynamicenv_pop",
    arguments=List(
      Argument(PointerType(WorldValue.irType))
    )
  )

  def apply(initialState : GenerationState, plannedSymbols : Set[String], typeGenerator : TypeGenerator)(step : ps.Parameterize) : GenResult = step match {
    case ps.Parameterize(result, worldPtrTemp, parameterValues, steps, innerResult) =>
      val module = initialState.currentBlock.function.module

      // Declare all support functions
      for(supportFunc <- List(llibyDynamicenvPush, llibyDynamicenvSetValue, llibyDynamicenvPop)) {
        module.unlessDeclared(supportFunc) {
          module.declareFunction(supportFunc)
        }
      }

      // Get our world pointer IR value
      val worldPtrIr = initialState.liveTemps(worldPtrTemp)

      val initialBlock = initialState.currentBlock
      
      // Push the new environment
      initialBlock.callDecl(None)(llibyDynamicenvPush, List(worldPtrIr))

      // Set each value
      for((parameterTemp, valueTemp) <- step.parameterValues) {
        val parameterIr = initialState.liveTemps(parameterTemp) 
        val valueIr = initialState.liveTemps(valueTemp) 

        initialBlock.callDecl(None)(llibyDynamicenvSetValue, List(worldPtrIr, parameterIr, valueIr))
      }

      val finalResult = GenPlanSteps(initialState, plannedSymbols, typeGenerator)(step.steps)

      finalResult match {
        case finalState : GenerationState =>
          val finalBlock = finalState.currentBlock

          // Pop the environment
          finalBlock.callDecl(None)(llibyDynamicenvPop, List(worldPtrIr))

          val updatedLiveTemps = finalState.liveTemps.withAliasedTempValue(
            step.innerResult,
            (step.result -> finalState.liveTemps(step.innerResult))
          )
            
          finalState.copy(
            liveTemps=updatedLiveTemps
          )

        case terminated : BlockTerminated =>
          terminated
      }
  }
}
