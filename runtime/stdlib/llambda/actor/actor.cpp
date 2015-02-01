#include "actor/ActorProcedureCell.h"

#include "binding/MailboxCell.h"
#include "binding/UnitCell.h"
#include "actor/Mailbox.h"
#include "actor/Message.h"

using namespace lliby;

extern "C"
{

MailboxCell* llactor_act(World &world, actor::ActorProcedureCell *actorProc)
{
	return MailboxCell::createInstance(world, actorProc->start());
}

void llactor_send(World &world, MailboxCell *destMailboxCell, AnyCell *messageCell)
{
	std::shared_ptr<actor::Mailbox> destMailbox(destMailboxCell->mailbox().lock());

	if (!destMailbox)
	{
		// Destination has gone away
		return;
	}

	actor::Message *msg = actor::Message::createFromCell(messageCell, world.mailbox());
	destMailbox->send(msg);
}

AnyCell *llactor_sender(World &world)
{
	std::shared_ptr<actor::Mailbox> mailbox(world.sender());

	if (!mailbox)
	{
		// No last sender
		return UnitCell::instance();
	}

	return MailboxCell::createInstance(world, mailbox);
}

AnyCell* llactor_receive(World &world)
{
	return world.mailbox()->receiveInto(world);
}

}