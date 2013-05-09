package llambda

object ExtractProgram {
  def apply(data : List[ast.Datum])(implicit libraryLoader : Seq[LibraryNameComponent] => Map[String, BoundValue]) : List[et.Expression] = {
    // Split out our imports from our expressions
    val (importData, expressionData) = data.span {
      case ast.ProperList(ast.Symbol("import") :: _) => true
      case _ => false
    }
    
    // Find our initial bindings
    val initialBindingList = importData.flatMap(ResolveImportDecl(_))

    // Convert it to a scope
    val initialScope = new Scope(collection.mutable.Map(initialBindingList : _*), None)

    // Extract the body expressions
    val (expressions, _) = ExtractBody(expressionData)(initialScope)
    expressions
  }
}