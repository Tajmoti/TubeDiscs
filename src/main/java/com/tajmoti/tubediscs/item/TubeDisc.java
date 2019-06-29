package com.tajmoti.tubediscs.item;

import com.tajmoti.tubediscs.TubeDiscs;
import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import com.tajmoti.tubediscs.gui.TubeDiscGui;
import com.tajmoti.tubediscs.net.TubePlayMessage;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class TubeDisc extends ItemRecord {
    private static final String NBT_URL = "url";
    private final Logger logger;
    private final SimpleNetworkWrapper network;
    private final AudioTracker<TimedAudioRequest> audio;


    public TubeDisc(Logger logger, SimpleNetworkWrapper network, AudioTracker<TimedAudioRequest> audio) {
        super(null, null);
        this.logger = logger;
        this.network = network;
        this.audio = audio;
        setUnlocalizedName("tubedisc");
        setRegistryName("tubedisc");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack is = playerIn.getHeldItem(handIn);
        openUrlGUI(worldIn, playerIn);
        return ActionResult.newResult(EnumActionResult.SUCCESS, is);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        IBlockState bs = worldIn.getBlockState(pos);

        // If not jukebox, ignore everything and pass
        if (!isReadyJukebox(bs))
            return EnumActionResult.PASS;

        // Get the video URL
        URL url = getUrl(player.getHeldItem(hand));

        if (url == null) {
            if (worldIn.isRemote) {
                logger.info("Disc URL is null, opening GUI");
                openUrlGUI(worldIn, player);
            }
            return EnumActionResult.FAIL;
        }

        // If client, do not do anything
        if (worldIn.isRemote)
            return EnumActionResult.SUCCESS;

        long worldTime = worldIn.getTotalWorldTime();
        TimedAudioRequest request = new TimedAudioRequest(player.dimension, pos, url.toString(), worldTime);
        network.sendToAll(new TubePlayMessage(request, worldTime));
        audio.addSound(request);

        // Insert the record item into the jukebox
        ItemStack itemstack = player.getHeldItem(hand);
        ((BlockJukebox) Blocks.JUKEBOX).insertRecord(worldIn, pos, bs, itemstack);
        itemstack.shrink(1);
        player.addStat(StatList.RECORD_PLAYED);

        return EnumActionResult.SUCCESS;
    }

    private boolean isReadyJukebox(IBlockState bs) {
        return bs.getBlock() == Blocks.JUKEBOX && !bs.getValue(BlockJukebox.HAS_RECORD);
    }

    private void openUrlGUI(World worldIn, EntityPlayer playerIn) {
        BlockPos pos = playerIn.getPosition();
        playerIn.openGui(TubeDiscs.getInstance(), TubeDiscGui.ID, worldIn, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void setUrl(ItemStack is, URL url) {
        NBTTagCompound compound = is.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            is.setTagCompound(compound);
        }
        compound.setString(NBT_URL, url.toString());
    }

    public static URL getUrl(ItemStack is) {
        if (!is.hasTagCompound()) return null;
        try {
            return new URL(is.getTagCompound().getString(NBT_URL));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTagCompound()) {
            NBTTagCompound compound = stack.getTagCompound();
            tooltip.add(compound.getString(NBT_URL));
        }
    }
}
