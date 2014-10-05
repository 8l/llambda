package io.llambda.compiler.planner
import io.llambda

import llambda.compiler.ProcedureSignature
import llambda.compiler.{valuetype => vt}

// Case procedures use the TopProcedureType signature for two reasons
// - Calculating a signature that covers all signatures is complex
// - The signature will likely not be much more efficient than the TopProcedureType (it almost certainly
//   requires a rest argument) and this will avoid creating an adapter procedure if it is cast to the 
//   TopProcedureType later
object CaseLambdaSignature extends
  ProcedureSignature(
    hasWorldArg=true,
    hasSelfArg=true,
    fixedArgTypes=Nil,
    restArgMemberTypeOpt=Some(vt.AnySchemeType),
    returnType=vt.ReturnType.ArbitraryValues,
    attributes=Set()
  )