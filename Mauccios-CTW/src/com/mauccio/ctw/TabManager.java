package com.mauccio.ctw;

import java.lang.reflect.Field;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PlayerConnection;

public class TabManager {
	
	public static void setTabListHeader(Player player, String header) {
		CraftPlayer cp = (CraftPlayer) player;
		PlayerConnection c = cp.getHandle().playerConnection;
		IChatBaseComponent top = ChatSerializer.a("{text: '" + header + "'}" );
		PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
		try {
			Field headerField = packet.getClass().getDeclaredField("a");
			headerField.setAccessible(true);
			headerField.set(packet, top);
			headerField.setAccessible(!headerField.isAccessible());
		} catch (Exception e) {
			e.printStackTrace();
		}
		c.sendPacket(packet);
	}
	
	public static void setTabListFooter(Player player, String footer) {
		CraftPlayer cp = (CraftPlayer) player;
		PlayerConnection c = cp.getHandle().playerConnection;
		IChatBaseComponent below = ChatSerializer.a("{text: '" + footer + "'}" );
		PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();	
		try {
			Field footerField = packet.getClass().getDeclaredField("b");
			footerField.setAccessible(true);
			footerField.set(packet, below);
			footerField.setAccessible(!footerField.isAccessible());
		} catch (Exception e) {
			e.printStackTrace();
		}
		c.sendPacket(packet);	
	}
	
}
