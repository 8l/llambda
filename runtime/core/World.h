#ifndef _LLIBY_CORE_WORLD_H
#define _LLIBY_CORE_WORLD_H

namespace lliby
{

namespace dynamic
{

class State;

}

namespace alloc
{

class MemoryBlock;
class AllocCell;
class CellRefRangeList;

}

class World
{
public:
	static void launchWorld(void (*entryPoint)(World &));

	//
	// This is the public section of World
	// Generated code can access these fields directly
	// Any changes to the content, size or order of these fields will require codegen changes
	//
	
	alloc::AllocCell *allocNext;
	alloc::AllocCell *allocEnd;

	//
	// This is the private section of World
	// This is only used internally by the runtime
	//
	
	dynamic::State *activeState;

	// These are lists of strong and weak refs in the current world
	alloc::CellRefRangeList *strongRefs;
	alloc::CellRefRangeList *weakRefs;
	
	// Pointer to the start of the allocator semi-space
	alloc::MemoryBlock *activeAllocBlock = nullptr;

private:
	World();
	~World();
};

}

extern "C"
{

void _lliby_launch_world(void (*entryPoint)(lliby::World &));

}

#endif
