package net.minecraft.network.play.server;

import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.List;

public class SSetPassengersPacket implements IPacket<IClientPlayNetHandler>
{
    private int entityId;
    private int[] passengerIds;

    public SSetPassengersPacket()
    {
    }

    public SSetPassengersPacket(Entity entityIn)
    {
        this.entityId = entityIn.getEntityId();
        List<Entity> list = entityIn.getPassengers();
        this.passengerIds = new int[list.size()];

        for (int i = 0; i < list.size(); ++i)
        {
            this.passengerIds[i] = list.get(i).getEntityId();
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public void readPacketData(PacketBuffer buf) throws IOException
    {
        this.entityId = buf.readVarInt();
        this.passengerIds = buf.readVarIntArray();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeVarInt(this.entityId);
        buf.writeVarIntArray(this.passengerIds);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(IClientPlayNetHandler handler)
    {
        handler.handleSetPassengers(this);
    }

    public int[] getPassengerIds()
    {
        return this.passengerIds;
    }

    public int getEntityId()
    {
        return this.entityId;
    }
}
