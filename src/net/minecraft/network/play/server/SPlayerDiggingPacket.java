package net.minecraft.network.play.server;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SPlayerDiggingPacket implements IPacket<IClientPlayNetHandler>
{
    /** Unused (probably related to the unused parameter in the constructor) */
    private static final Logger LOGGER = LogManager.getLogger();
    private BlockPos pos;
    private BlockState state;
    CPlayerDiggingPacket.Action action;
    private boolean successful;

    public SPlayerDiggingPacket()
    {
    }

    public SPlayerDiggingPacket(BlockPos pos, BlockState state, CPlayerDiggingPacket.Action action, boolean successful, String context)
    {
        this.pos = pos.toImmutable();
        this.state = state;
        this.action = action;
        this.successful = successful;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public void readPacketData(PacketBuffer buf) throws IOException
    {
        this.pos = buf.readBlockPos();
        this.state = Block.BLOCK_STATE_IDS.getByValue(buf.readVarInt());
        this.action = buf.readEnumValue(CPlayerDiggingPacket.Action.class);
        this.successful = buf.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) throws IOException
    {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(Block.getStateId(this.state));
        buf.writeEnumValue(this.action);
        buf.writeBoolean(this.successful);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(IClientPlayNetHandler handler)
    {
        handler.handleAcknowledgePlayerDigging(this);
    }

    public BlockState getBlockState()
    {
        return this.state;
    }

    public BlockPos getPosition()
    {
        return this.pos;
    }

    public boolean wasSuccessful()
    {
        return this.successful;
    }

    public CPlayerDiggingPacket.Action getAction()
    {
        return this.action;
    }
}
