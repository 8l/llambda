package io.llambda.llvmir

import collection.mutable.ListBuffer

trait IrBranchTarget {
  val label : String
}

private[llvmir] abstract class IrInstrBuilder(protected val nameSource : LocalNameSource) {
  // This contains our instructions as they're built
  private[llvmir] val instructions = new ListBuffer[String]
}

abstract class IrBlockBuilder(nameSource : LocalNameSource, val label : String) extends IrInstrBuilder(nameSource) with Irable with TerminatorInstrs with MemoryInstrs with BitwiseInstrs with ConversionInstrs with OtherInstrs {
  private val childBlocks = new collection.mutable.ListBuffer[IrChildBlockBuilder]

  def comment(text : String) {
    instructions += s"; ${text}"
  }

  def toIr : String = {
    // Tab indent and join with newlines
    s"${label}:\n" + instructions.map("\t" + _).mkString("\n")
  }
  
  def startChildBlock(baseName : String) : IrChildBlockBuilder = {
    val label = nameSource.allocate(baseName)
    val block = new IrChildBlockBuilder(nameSource, label)

    childBlocks += block

    block
  }

  def allChildren : List[IrChildBlockBuilder] = {
    childBlocks.toList flatMap { block =>
      block :: block.allChildren
    }
  }
}

class IrEntryBlockBuilder(nameSource : LocalNameSource) extends IrBlockBuilder(nameSource, "entry")

class IrChildBlockBuilder(nameSource : LocalNameSource, label : String) extends IrBlockBuilder(nameSource, label) with IrBranchTarget with PhiInstr
