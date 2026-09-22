package com.nightbeam.remnants.client;

import com.nightbeam.remnants.config.JaumlConfigLib;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.BossEvent;

/** Original pixel ornaments drawn in GUI coordinates; no resolution-dependent raster scaling. */
public final class RemnantBossBars {
    private RemnantBossBars() {}
    public static int theme(Component name) {
        if(name.getContents() instanceof TranslatableContents translated) {
            String key=translated.getKey();
            if(key.equals("entity.remnant_bosses.ossukage")||key.equals("entity.remnants.ossukage")) return 0;
            if(key.equals("entity.remnant_bosses.kotsukage")||key.equals("entity.remnants.kotsukage")) return 1;
            if(key.equals("entity.remnant_bosses.umbrakar")||key.equals("entity.remnants.umbrakar")) return 2;
            if(key.equals("entity.remnant_bosses.hollow_sovereign")) return 3;
        }
        for(Component sibling:name.getSiblings()) { int t=theme(sibling); if(t>=0)return t; }
        return -1;
    }
    public static boolean enabled() { return JaumlConfigLib.getNumberValue("remnant/client","presentation","custom_boss_bars")>0; }
    private static void rect(GuiGraphics g,int x,int y,int w,int h,int color) { g.fill(x,y,x+w,y+h,color); }
    public static boolean draw(GuiGraphics g,int x,int y,BossEvent event) {
        int theme=theme(event.getName()); if(theme<0||!enabled())return false;
        int[] colors={0xFFE55169,0xFFE6BB74,0xFFBD66FF,0xFF70E6B0};
        int[] shadows={0xFF692B40,0xFF75503D,0xFF503260,0xFF375B4C};
        int glow=colors[theme],shade=shadows[theme],ink=0xFF16121F,bone=0xFFAD9181,light=0xFFE2C9AB;
        // The frame leaves the vanilla name row intact. All ornaments fit the 36px row.
        rect(g,x-3,y-2,188,13,ink); rect(g,x-1,y,184,9,bone); rect(g,x,y+1,182,7,shade);
        int fill=Math.max(0,Math.min(182,(int)(event.getProgress()*182)));
        rect(g,x,y+2,fill,5,glow); rect(g,x,y+2,fill,1,0xFFE7FFED); rect(g,x,y+6,fill,1,shade);
        for(int i=13;i<182;i+=14) rect(g,x+i,y+1,1,7,ink);
        for(int side:new int[]{-1,1}) {
            int end=side<0?x-4:x+185;
            // Layered flared bone/wing endcaps, mirrored around the bar.
            for(int i=0;i<5;i++) {
                int px=end+side*i*3;
                rect(g,px-2,y-3+i,5,13-i,ink);
                rect(g,px-1,y-2+i,3,9-i,theme==2?shade:bone);
                rect(g,px-1,y-2+i,2,2,light);
            }
            for(int i=0;i<3;i++) {
                int px=side<0?x+12+i*13:x+167-i*13;
                rect(g,px,y+8,3,5+i%2*2,ink); rect(g,px+1,y+8,1,4,bone);
                if(theme==2) rect(g,px-3,y+10,7,2,shade);
            }
        }
        int c=x+91;
        y+=6;
        rect(g,c-10,y-3,20,16,ink); rect(g,c-8,y-4,16,16,bone);
        rect(g,c-6,y-5,12,3,light); rect(g,c-6,y+10,12,6,ink); rect(g,c-4,y+10,8,5,bone);
        if(theme==2) { // Riftmaw: horned single eye.
            rect(g,c-7,y,14,7,shade); rect(g,c-5,y+2,10,3,glow); rect(g,c-1,y,2,7,ink);
            for(int s:new int[]{-1,1}) { rect(g,c+s*10-1,y-6,3,8,ink); rect(g,c+s*12-1,y-5,3,3,bone); }
        } else {
            rect(g,c-6,y+1,5,5,ink); rect(g,c+1,y+1,5,5,ink);
            rect(g,c-5,y+2,3,2,glow); rect(g,c+2,y+2,3,2,glow); rect(g,c-1,y+6,2,3,ink);
            for(int i=-3;i<=3;i+=3) rect(g,c+i,y+11,1,4,ink);
            if(theme==3) for(int i=-1;i<=1;i++) { rect(g,c+i*5-1,y-6-Math.abs(i),3,5,ink); rect(g,c+i*5,y-5-Math.abs(i),1,3,glow); }
            if(theme==0) { rect(g,c-9,y-1,18,2,shade); rect(g,c+9,y,7,2,glow); }
            if(theme==1) for(int s:new int[]{-1,1}) { rect(g,c+s*11-1,y-5,3,9,ink); rect(g,c+s*12-1,y-4,2,5,light); }
        }
        return true;
    }
}
