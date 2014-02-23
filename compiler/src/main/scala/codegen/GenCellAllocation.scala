package io.llambda.compiler.codegen
import io.llambda

import llambda.llvmir._
import llambda.llvmir.IrFunction._

object GenCellAllocation {
  private val cellType = UserDefinedType("cell")
  private val cellPointerType = PointerType(cellType)
  
  private val llibyAllocCells = IrFunctionDecl(
    result=Result(cellPointerType),
    name="_lliby_alloc_cells",
    arguments=List(
      Argument(PointerType(WorldValue.irType)),
      Argument(IntegerType(64))
    ),
    attributes=Set(NoUnwind)
  )

  def genAllocation(initialState : GenerationState)(worldPtrIr : IrValue, count : Int) : (GenerationState, CellAllocation)  = {
    val startBlock = initialState.currentBlock

    if (count == 0) {
      val allocation = EmptyCellAllocation()
      return (initialState, allocation)
    }

    initialState.module.unlessDeclared(llibyAllocCells) {
      initialState.module.declareFunction(llibyAllocCells)
    }

    startBlock.comment(s"allocating ${count} cells")

    // We need this a few times
    val allocCountValue = IntegerConstant(IntegerType(64), count)

    // Load the pointer to our allocation start
    // This is our allocation unless we run out of memory
    val directAllocValue = WorldValue.genLoadFromAllocNext(startBlock)(worldPtrIr)
    
    // Load the pointer to our allocation end value
    val allocEndValue = WorldValue.genLoadFromAllocEnd(startBlock)(worldPtrIr)

    // Add our allocation count on to our allocation
    val newAllocNextValue = startBlock.getelementptr("newAllocNext")(cellType, directAllocValue, List(allocCountValue))

    // Create our child blocks for the upcoming branch
    val directSuccessBlock = startBlock.startChildBlock("directSuccess")
    val collectGarbageBlock = startBlock.startChildBlock("collectGarbage")
    val allocFinishedBlock = startBlock.startChildBlock("allocFinished")

    // See if we ran out of space
    val directSucceededPred = startBlock.icmp("directSucceeded")(ComparisonCond.LessThanEqual, Some(false), newAllocNextValue, allocEndValue)

    // This should almost always be true
    val expectedPred = GenLlvmExpect(startBlock)(directSucceededPred, IntegerConstant(IntegerType(1), 1))

    startBlock.condBranch(expectedPred, directSuccessBlock, collectGarbageBlock)

    // In the direct alloc block store our new start pointer
    WorldValue.genStoreToAllocNext(directSuccessBlock)(newAllocNextValue, worldPtrIr)
    directSuccessBlock.uncondBranch(allocFinishedBlock)

    // In the garage collection block first save our GC roots
    val collectGarbageState = initialState.copy(currentBlock=collectGarbageBlock) 

    val calcedBarrier = GenGcBarrier.calculateGcBarrier(collectGarbageState)
    GenGcBarrier.genSaveGcRoots(collectGarbageState)(calcedBarrier)

    // Now call the runtime
    val Some(runtimeAllocValue) = collectGarbageBlock.callDecl(Some("runtimeAlloc"))(llibyAllocCells, List(worldPtrIr, allocCountValue))

    // Now restore the GC roots
    val newIrValues = GenGcBarrier.genRestoreGcRoots(collectGarbageState)(calcedBarrier)  

    collectGarbageBlock.uncondBranch(allocFinishedBlock)

    // Phi the two result together
    val allocResultValue = allocFinishedBlock.phi("allocResult")(
      PhiSource(directAllocValue, directSuccessBlock),
      PhiSource(runtimeAllocValue, collectGarbageBlock)
    )

    // Now phi every restored GC root (ugh)
    val liveTempsUpdate = calcedBarrier.restoreTemps.zip(newIrValues).map { case ((tempValue, directIrValue), gcIrValue) =>
      val phiedGcRoot = allocFinishedBlock.phi("phiedGcRoot")(
        PhiSource(directIrValue, directSuccessBlock),
        PhiSource(gcIrValue, collectGarbageBlock)
      )

      (tempValue -> (phiedGcRoot : IrValue))
    }

    val allocation = new CellAllocation(allocResultValue, 0, count)

    // Note we don't update gcRootedTemps here because it happens in a branch
    (initialState.copy(
      currentBlock=allocFinishedBlock,
      liveTemps=initialState.liveTemps ++ liveTempsUpdate
    ), allocation)
  }
  
  def genDeallocation(state : GenerationState)(worldPtrIr : IrValue) {
    // How many cells were left in the allocation?
    val remainingCells = state.currentAllocation.remainingCells

    if (remainingCells != 0) {
      val block = state.currentBlock
      block.comment(s"Rolling back allocation of ${remainingCells} cells")

      // Load the pointer to our allocation end value
      val allocNextValue = WorldValue.genLoadFromAllocNext(block)(worldPtrIr)

      // Subtract the cells we haven't used
      val allocationDeltaIr = IntegerConstant(IntegerType(64), -remainingCells)
      val newAllocNextValue = block.getelementptr("newAllocNext")(cellType, allocNextValue, List(allocationDeltaIr))
      
      // Store the new next pointer
      WorldValue.genStoreToAllocNext(block)(newAllocNextValue, worldPtrIr)
    }
  }
}
