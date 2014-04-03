#include <string>
#include <sstream>

#include "core/init.h"
#include "core/World.h"

#include "writer/ExternalFormDatumWriter.h"
#include "binding/UnitCell.h"
#include "binding/EmptyListCell.h"
#include "binding/BooleanCell.h"
#include "binding/ExactIntegerCell.h"
#include "binding/InexactRationalCell.h"
#include "binding/StringCell.h"
#include "binding/SymbolCell.h"
#include "binding/PairCell.h"
#include "binding/BytevectorCell.h"
#include "binding/VectorCell.h"
#include "binding/ProcedureCell.h"
#include "binding/CharacterCell.h"
#include "binding/RecordCell.h"
#include "binding/ErrorObjectCell.h"

#include "alloc/StrongRef.h"
#include "assertions.h"
#include "stubdefinitions.h"

namespace
{
using namespace lliby;

std::string externalFormFor(const DatumCell *datum)
{
	std::ostringstream outputStream;

	ExternalFormDatumWriter writer(outputStream);
	writer.render(datum);

	return outputStream.str();
}

void assertForm(const DatumCell *datum, std::string expected)
{
	ASSERT_EQUAL(externalFormFor(datum), expected);
}

SymbolCell *symbolFor(World &world, const char *utf8String)
{
	return SymbolCell::fromString(world, StringCell::fromUtf8CString(world, utf8String));
}

StringCell *stringFor(World &world, const char *utf8String)
{
	return StringCell::fromUtf8CString(world, utf8String);
}

void testUnit()
{
	assertForm(UnitCell::instance(), "#!unit");
}

void testEmptyList()
{
	assertForm(EmptyListCell::instance(), "()");
}

void testBoolean()
{
	assertForm(BooleanCell::trueInstance(), "#t");
	assertForm(BooleanCell::falseInstance(), "#f");
}

void testExactInteger(World &world)
{
	assertForm(ExactIntegerCell::fromValue(world, 25), "25");
	assertForm(ExactIntegerCell::fromValue(world, 0), "0");
	assertForm(ExactIntegerCell::fromValue(world,-31337), "-31337");
}

void testInexactRational(World &world)
{
	assertForm(InexactRationalCell::fromValue(world, 0.0), "0.0");

	assertForm(InexactRationalCell::fromValue(world, 12.5), "12.5");
	assertForm(InexactRationalCell::fromValue(world, -4.55), "-4.55");

	assertForm(InexactRationalCell::fromValue(world, 100.0), "100.0");
	assertForm(InexactRationalCell::fromValue(world, -500.0), "-500.0");

	assertForm(InexactRationalCell::NaN(world), "+nan.0");
	assertForm(InexactRationalCell::positiveInfinity(world), "+inf.0");
	assertForm(InexactRationalCell::negativeInfinity(world), "-inf.0");
}

void testSymbol(World &world)
{
	assertForm(symbolFor(world, u8"Hello"), u8"Hello");
	assertForm(symbolFor(world, u8"HelloWorldThisRequiresHeapAllocation"), u8"HelloWorldThisRequiresHeapAllocation");
	assertForm(symbolFor(world, u8"λ"), u8"|λ|");
	assertForm(symbolFor(world, u8"Hello, world"), u8"|Hello, world|");
	assertForm(symbolFor(world, u8"Back\\slash"), u8"|Back\\\\slash|");
	assertForm(symbolFor(world, u8"P|pe"), u8"|P\\|pe|");
	assertForm(symbolFor(world, u8"Quo\"te"), u8"|Quo\"te|");
	assertForm(symbolFor(world, u8""), u8"||");
}

void testString(World &world)
{
	assertForm(stringFor(world, u8"Hello"), u8"\"Hello\"");
	assertForm(stringFor(world, u8"λ"), u8"\"λ\"");
	assertForm(stringFor(world, u8"Hello, world"), u8"\"Hello, world\"");
	assertForm(stringFor(world, u8"Hello\nworld"), u8"\"Hello\\nworld\"");
	assertForm(stringFor(world, u8"Hello\bworld"), u8"\"Hello\\bworld\"");
	assertForm(stringFor(world, u8"Hello\"world"), u8"\"Hello\\\"world\"");
	assertForm(stringFor(world, u8""), u8"\"\"");
}

void testPair(World &world)
{
	alloc::StrongRef<SymbolCell> valueA(world, symbolFor(world, "A"));
	alloc::StrongRef<SymbolCell> valueB(world, symbolFor(world, "B"));
	alloc::StrongRef<SymbolCell> valueC(world, symbolFor(world, "C"));

	assertForm(PairCell::createProperList(world, {}), "()");
	assertForm(PairCell::createProperList(world, {valueA}), "(A)");
	assertForm(PairCell::createProperList(world, {valueA, valueB}), "(A B)");
	assertForm(PairCell::createProperList(world, {valueA, valueB, valueC}), "(A B C)");

	assertForm(PairCell::createList(world, {valueA}, valueB), "(A . B)");
	assertForm(PairCell::createList(world, {valueA, valueB}, valueC), "(A B . C)");

	// Create a  nested list
	DatumCell *innerList = PairCell::createList(world, {valueA, valueB}, valueC);
	DatumCell *outerList = PairCell::createProperList(world, {valueA, valueB, valueC, innerList});
	assertForm(outerList, "(A B C (A B . C))");
}

void testBytevector(World &world)
{
	{
		auto *emptyVector = BytevectorCell::fromData(world, nullptr, 0);
		assertForm(emptyVector, "#u8()");
	}

	{
		uint8_t testData[5] = { 100, 101, 202, 203, 204 };
		auto *testVector = BytevectorCell::fromData(world, testData, 5); 

		assertForm(testVector, "#u8(100 101 202 203 204)");
	}
}

void testVector(World &world)
{
	{
		VectorCell *emptyVector = VectorCell::fromFill(world, 0);
		assertForm(emptyVector, "#()");
	}

	{
		alloc::StrongRef<VectorCell> fillVector(world, VectorCell::fromFill(world, 5));

		for(unsigned int i = 0; i < 5; i++)
		{
			auto newExactInt = ExactIntegerCell::fromValue(world, i);
			fillVector->setElementAt(i, newExactInt);
		}

		assertForm(fillVector, "#(0 1 2 3 4)");
	}
}

void testProcedure(World &world)
{
	// Outputting of pointers isn't consistent across C++ standard libraries
	// This means our null entry point might be output differently on different 
	// platforms. The entry point output is just for debugging so there's not 
	// point checking it.
	std::string procedureForm = externalFormFor(ProcedureCell::createInstance(world, 0, true, nullptr, nullptr));
	const std::string expectedPrefix("#!procedure(");

	ASSERT_TRUE(procedureForm.compare(0, expectedPrefix.length(), expectedPrefix) == 0);

}

void testCharacter(World &world)
{
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x07)), "#\\alarm");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x08)), "#\\backspace");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x7f)), "#\\delete");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x1b)), "#\\escape");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x0a)), "#\\newline");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x00)), "#\\null");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x0d)), "#\\return");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x20)), "#\\space");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x09)), "#\\tab");
	assertForm(CharacterCell::createInstance(world, UnicodeChar('A')), "#\\A");
	assertForm(CharacterCell::createInstance(world, UnicodeChar('a')), "#\\a");
	assertForm(CharacterCell::createInstance(world, UnicodeChar('1')), "#\\1");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(')')), "#\\)");
	assertForm(CharacterCell::createInstance(world, UnicodeChar(0x03bb)), "#\\x3bb");
}

void testRecord(World &world)
{
    assertForm(RecordCell::createInstance(world, 0, true, nullptr), "#!record");
}

void testErrorObject(World &world)
{
	alloc::StrongRef<StringCell> errorString(world, StringCell::fromUtf8CString(world, u8"Test error"));
	auto errorObj = ErrorObjectCell::createInstance(world, errorString, EmptyListCell::instance());

	assertForm(errorObj, "#!error(Test error)");
}

void testAll(World &world)
{
	testUnit();
	testEmptyList();
	testBoolean();
	testExactInteger(world);
	testInexactRational(world);
	testSymbol(world);
	testString(world);
	testPair(world);
	testBytevector(world);
	testVector(world);
	testProcedure(world);
	testCharacter(world);
	testRecord(world);
	testErrorObject(world);
}

}

int main(int argc, char *argv[])
{
	lliby_init();
	
	lliby::World::launchWorld(&testAll);
}
